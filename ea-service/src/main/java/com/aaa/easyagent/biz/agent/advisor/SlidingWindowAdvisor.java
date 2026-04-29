package com.aaa.easyagent.biz.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 滑动窗口 Advisor，基于 token 计数控制上下文窗口。
 * <p>
 * 当累计 token 总数超过 {@code windowLimit × threshold} 时，按轮次粒度从前往后裁剪消息，
 * 保留更近的消息。与轮数限制相比，滑动窗口能更精细地利用模型上下文窗口。
 * </p>
 *
 * <p>通过 ChatClientRequest context 传递以下参数：</p>
 * <ul>
 *   <li>{@link #MESSAGES_KEY} — 消息历史列表引用（用于裁剪）</li>
 * </ul>
 *
 * @author liuzhen.tian
 * @version 1.0 SlidingWindowAdvisor.java 2026/4/29
 */
@Slf4j
public class SlidingWindowAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * ChatClientRequest context 中消息历史列表的 key
     */
    public static final String MESSAGES_KEY = "slidingWindowMessages";

    private final int order;
    private final long windowLimit;
    private final double threshold;

    /**
     * 累计输入 token 数（跨决策轮次累积）
     */
    private long accumulatedInputTokens;

    /**
     * 累计输出 token 数（跨决策轮次累积）
     */
    private long accumulatedOutputTokens;

    /**
     * 当前流式调用中最后一次见到的 usage（用于流结束时累加）
     */
    private long roundInputTokens;
    private long roundOutputTokens;

    public SlidingWindowAdvisor(long windowLimit, double threshold) {
        this(0, 0L, 0L, windowLimit, threshold);
    }

    public SlidingWindowAdvisor(int order, long windowLimit, double threshold) {
        this(order, 0L, 0L, windowLimit, threshold);
    }

    public SlidingWindowAdvisor(int order, long initInputTokens, long initOutputTokens, long windowLimit, double threshold) {
        this.order = order;
        this.windowLimit = windowLimit;
        this.threshold = threshold;
        this.accumulatedInputTokens = initInputTokens;
        this.accumulatedOutputTokens = initOutputTokens;
    }

    @Override
    public String getName() {
        return "SlidingWindowAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    // ========== Call (non-streaming) ==========

    @Override
    @SuppressWarnings("unchecked")
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 调用前：检查并裁剪
        List<Message> messages = (List<Message>) chatClientRequest.context().get(MESSAGES_KEY);
        trimIfNeeded(messages);

        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // 调用后：累加 token
        if (response != null && response.chatResponse() != null) {
            accumulateFrom(response.chatResponse());
        }

        return response;
    }

    // ========== Stream ==========

    @Override
    @SuppressWarnings("unchecked")
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                  StreamAdvisorChain streamAdvisorChain) {
        // 调用前：检查并裁剪
        List<Message> messages = (List<Message>) chatClientRequest.context().get(MESSAGES_KEY);
        trimIfNeeded(messages);

        // 重置本轮计数
        roundInputTokens = 0;
        roundOutputTokens = 0;

        Flux<ChatClientResponse> stream = streamAdvisorChain.nextStream(chatClientRequest);

        return stream
                .doOnNext(response -> {
                    if (response != null && response.chatResponse() != null) {
                        trackRoundUsage(response.chatResponse());
                    }
                })
                .doFinally(signalType -> {
                    // 流结束后，将本轮用量累加到全局
                    accumulatedInputTokens += roundInputTokens;
                    accumulatedOutputTokens += roundOutputTokens;
                    if (roundInputTokens > 0 || roundOutputTokens > 0) {
                        log.debug("SlidingWindowAdvisor: round tokens +{}(in)/+{}(out)，accumulated: {}(in)/{}(out)",
                                roundInputTokens, roundOutputTokens,
                                accumulatedInputTokens, accumulatedOutputTokens);
                    }
                });
    }

    // ========== Usage tracking ==========

    /**
     * 追踪当前流式响应中的 token 用量（取本轮见到过的最大值）
     */
    private void trackRoundUsage(ChatResponse chatResponse) {
        if (chatResponse.getMetadata() == null || chatResponse.getMetadata().getUsage() == null) {
            return;
        }
        var usage = chatResponse.getMetadata().getUsage();
        try {
            if (usage instanceof EmptyUsage) {
                return;
            }
            int promptTokens = usage.getPromptTokens();
            int completionTokens = usage.getCompletionTokens();
            if (promptTokens > roundInputTokens) {
                roundInputTokens = promptTokens;
            }
            if (completionTokens > roundOutputTokens) {
                roundOutputTokens = completionTokens;
            }
        } catch (Exception e) {
            // ignore: some usage implementations may throw
        }
    }

    /**
     * 从非流式响应中累加 token 用量
     */
    private void accumulateFrom(ChatResponse chatResponse) {
        if (chatResponse.getMetadata() == null || chatResponse.getMetadata().getUsage() == null) {
            return;
        }
        var usage = chatResponse.getMetadata().getUsage();
        if (usage instanceof EmptyUsage) {
            return;
        }
        try {
            accumulatedInputTokens += usage.getPromptTokens();
            accumulatedOutputTokens += usage.getCompletionTokens();
            log.debug("SlidingWindowAdvisor: accumulated tokens +{}(in)/+{}(out)",
                    usage.getPromptTokens(), usage.getCompletionTokens());
        } catch (Exception e) {
            // ignore
        }
    }

    // ========== Trimming ==========

    /**
     * 检查是否需要裁剪消息历史。
     * <p>
     * 当 {@code accumulatedInputTokens + accumulatedOutputTokens > windowLimit * threshold} 时触发裁剪，
     * 从前往后按轮次粒度移除消息，直到 token 总量降到安全线以下。
     * </p>
     */
    private void trimIfNeeded(List<Message> messages) {
        if (windowLimit <= 0 || threshold <= 0 || messages == null || messages.isEmpty()) {
            return;
        }

        long totalTokens = accumulatedInputTokens + accumulatedOutputTokens;
        long triggerLimit = (long) (windowLimit * threshold);

        if (totalTokens <= triggerLimit) {
            return;
        }

        // 触发裁剪：移除前半部分消息
        int originalSize = messages.size();
        int targetRemove = Math.max(1, originalSize / 2);
        int removed = 0;

        while (removed < targetRemove && !messages.isEmpty()) {
            messages.remove(0);
            removed++;
        }

        // 按比例缩减累计 token 数
        if (originalSize > 0) {
            double ratio = (double) removed / originalSize;
            accumulatedInputTokens = Math.max(0, (long) (accumulatedInputTokens * (1 - ratio)));
            accumulatedOutputTokens = Math.max(0, (long) (accumulatedOutputTokens * (1 - ratio)));
        }

        log.warn("SlidingWindowAdvisor: 触发裁剪! totalTokens={}, triggerLimit={}, "
                        + "removed={}/{} msgs, accumulated after trim: {}(in)/{}(out)",
                totalTokens, triggerLimit, removed, originalSize,
                accumulatedInputTokens, accumulatedOutputTokens);
    }
}
