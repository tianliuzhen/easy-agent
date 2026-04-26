package com.aaa.easyagent.biz.agent.advisor;

import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 自定义 Advisor，通过 ChatClientRequest context 中的 SseEmitter 实时 SSE 推送
 * 流式响应中的思考过程和结果文本。
 *
 * <p>用法：</p>
 * <pre>{@code
 * ChatClient.builder(chatModel)
 *     .defaultAdvisors(new SseAdvisor())
 *     .build()
 *     .prompt()
 *     .advisors(as -> as.param(SseAdvisor.SSE_EMITTER_KEY, sseEmitter))
 *     ...
 * }</pre>
 *
 * @author liuzhen.tian
 * @version 1.0 SseAdvisor.java  2026/4/26
 */
public class SseAdvisor implements CallAdvisor, StreamAdvisor {

    /** ChatClientRequest context 中 SseEmitter 的 key */
    public static final String SSE_EMITTER_KEY = "sseEmitter";

    private final int order;

    public SseAdvisor() {
        this(0);
    }

    public SseAdvisor(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return "SseAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    // ========== Call (non-streaming) ==========

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        ChatResponse chatResponse = chatClientResponse != null ? chatClientResponse.chatResponse() : null;
        if (chatResponse != null) {
            sendToSse(chatResponse, chatClientRequest);
        }
        return chatClientResponse;
    }

    // ========== Stream ==========

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                  StreamAdvisorChain streamAdvisorChain) {
        Flux<ChatClientResponse> stream = streamAdvisorChain.nextStream(chatClientRequest);

        return stream.doOnNext(response -> {
            if (response != null && response.chatResponse() != null) {
                ChatResponse chatResponse = response.chatResponse();
                // 跳过 tool_call 的 chunk
                if (!chatResponse.hasToolCalls()) {
                    sendToSse(chatResponse, chatClientRequest);
                }
            }
        });
    }

    private static void sendToSse(ChatResponse chatResponse, ChatClientRequest request) {
        SseEmitter sse = getSseEmitter(request);
        if (sse == null) {
            return;
        }

        String reasoning = ChatResponseUtil.getReasoningContent(chatResponse);
        if (StringUtils.hasText(reasoning)) {
            SseHelper.sendThink(sse, reasoning);
        }

        String data = ChatResponseUtil.getResStr(chatResponse);
        if (StringUtils.hasText(data)) {
            SseHelper.sendData(sse, data);
        }
    }

    static SseEmitter getSseEmitter(ChatClientRequest request) {
        if (request == null || request.context() == null) {
            return null;
        }
        Object sseObj = request.context().get(SSE_EMITTER_KEY);
        return sseObj instanceof SseEmitter ? (SseEmitter) sseObj : null;
    }
}
