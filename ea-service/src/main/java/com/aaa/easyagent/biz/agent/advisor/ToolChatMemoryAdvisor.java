package com.aaa.easyagent.biz.agent.advisor;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.Assert;

/**
 * 自定义会话记忆 Advisor，仅负责「跨请求持久化」。
 *
 * <p>职责边界：
 * <ul>
 *   <li>历史加载：不在这里做。由 {@code ToolAgentExecutor} 构造时从 ChatMemory
 *       加载进 {@code this.messages}，因为 {@code spec.messages()} 在主线程先执行，
 *       advisor 的 {@code before()} 在 scheduler 线程后执行，来不及影响 prompt。</li>
 *   <li>prompt：完全由 {@code this.messages} 决定，advisor 绝不修改 prompt。</li>
 *   <li>历史保存：流结束时把 {@link ToolExecutionAdvisor#MESSAGES_KEY} 里的完整
 *       消息列表（含 tool_calls）写回 ChatMemory。</li>
 * </ul>
 * </p>
 *
 * @author liuzhen.tian
 */
public final class ToolChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final ChatMemory chatMemory;
    private final int order;
    private final Scheduler scheduler;

    private ToolChatMemoryAdvisor(ChatMemory chatMemory, int order, Scheduler scheduler) {
        Assert.notNull(chatMemory, "chatMemory cannot be null");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.chatMemory = chatMemory;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * 不修改 prompt，直接透传。历史加载已由 ToolAgentExecutor 构造时完成。
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    /**
     * 非流式路径保存历史。
     */
    @SuppressWarnings("unchecked")
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        List<Message> messages = (List<Message>) response.context().get(ToolExecutionAdvisor.MESSAGES_KEY);
        appendFinalAssistantIfAbsent(messages, response.chatResponse());
        saveMessages(getConversationId(response.context()), messages);
        return response;
    }

    /**
     * 流式路径：聚合完成后把完整 this.messages（含 tool_calls）写回 ChatMemory。
     * 从 request context 捕获 messages 引用，聚合回调触发时 ToolExecutionAdvisor
     * 已把 assistant(tool_calls) + tool 追加进该引用。
     */
    @SuppressWarnings("unchecked")
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain) {

        String conversationId = getConversationId(chatClientRequest.context());
        List<Message> messages = (List<Message>) chatClientRequest.context()
                .get(ToolExecutionAdvisor.MESSAGES_KEY);

        return Mono.just(chatClientRequest)
                .publishOn(this.scheduler)
                .map(request -> this.before(request, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream)
                .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                        aggregated -> {
                            // 最终答案（无 tool_calls 的 assistant）不会被 ToolExecutionAdvisor 写入，
                            // 这里补进 messages，保证历史包含 AI 的完整回复
                            appendFinalAssistantIfAbsent(messages, aggregated.chatResponse());
                            saveMessages(conversationId, messages);
                        }));
    }

    /**
     * 若本轮聚合结果是「不带 tool_calls 的 assistant 最终答案」，
     * 且尚未在 messages 中，则追加。带 tool_calls 的 assistant 已由
     * ToolExecutionAdvisor 写入，无需处理。
     */
    private void appendFinalAssistantIfAbsent(List<Message> messages, ChatResponse chatResponse) {
        if (messages == null || chatResponse == null || chatResponse.getResult() == null) {
            return;
        }
        AssistantMessage assistant = chatResponse.getResult().getOutput();
        if (assistant == null) {
            return;
        }
        boolean hasToolCalls = assistant.getToolCalls() != null && !assistant.getToolCalls().isEmpty();
        if (hasToolCalls) {
            return; // tool_calls 消息已由 ToolExecutionAdvisor 写入
        }
        // 无内容的空回复不保存
        if (assistant.getText() == null || assistant.getText().isBlank()) {
            return;
        }
        // 避免重复追加
        if (!messages.isEmpty() && messages.get(messages.size() - 1) == assistant) {
            return;
        }
        messages.add(assistant);
    }

    /**
     * clear + 全量保存：this.messages 每轮都是完整累加的历史，
     * 用整体替换避免重复追加。
     */
    private void saveMessages(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        this.chatMemory.clear(conversationId);
        this.chatMemory.add(conversationId, messages);
    }

    // ========== Builder ==========

    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    public static final class Builder {
        private int order = 0;
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private final ChatMemory chatMemory;

        private Builder(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public ToolChatMemoryAdvisor build() {
            return new ToolChatMemoryAdvisor(this.chatMemory, this.order, this.scheduler);
        }
    }
}
