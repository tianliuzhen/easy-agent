package com.aaa.easyagent.biz.agent.advisor;

import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.util.LoopDetector;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自定义 Advisor，拦截流式响应中的 tool_call chunk，自动执行工具并将结果写回消息历史。
 * <p>
 * 配合 {@link SseAdvisor} 使用：SseAdvisor 负责 SSE 推送，本 Advisor 负责工具调度。
 * </p>
 *
 * <p>通过 ChatClientRequest context 传递以下参数：</p>
 * <ul>
 *   <li>{@link #CALLBACK_MAP_KEY} — FunctionCallback 映射表</li>
 *   <li>{@link #MESSAGES_KEY} — 消息历史列表（用于追加 assistant/tool response）</li>
 *   <li>{@link #TOOL_SAME_CALL_MAP_KEY} — 卡住检测用的调用计数 map</li>
 *   <li>{@link #WITH_TOOL_CALL_KEY} — 标记本轮是否有工具调用</li>
 * </ul>
 *
 * @author liuzhen.tian
 * @version 1.0 ToolExecutionAdvisor.java  2026/4/26
 */
public class ToolExecutionAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * ChatClientRequest context 中 FunctionCallback 映射表的 key
     */
    public static final String CALLBACK_MAP_KEY = "toolCallbackMap";

    /**
     * ChatClientRequest context 中消息历史列表的 key
     */
    public static final String MESSAGES_KEY = "messages";

    /**
     * ChatClientRequest context 中卡住检测计数 map 的 key
     */
    public static final String TOOL_SAME_CALL_MAP_KEY = "toolSameCallCountMap";

    /**
     * ChatClientRequest context 中标记本轮是否有 tool call 的 key
     */
    public static final String WITH_TOOL_CALL_KEY = "withToolCall";

    /**
     * ChatClientRequest context 中 SseEmitter 的 key（复用 SseAdvisor）
     */
    public static final String SSE_EMITTER_KEY = "sseEmitter";

    private final int order;
    private final Map<String, FunctionCallback> callbackMap;
    private final Map<String, Integer> toolSameCallCountMap;
    private final SseEmitter sseEmitter;

    public ToolExecutionAdvisor() {
        this(0, null, null, null);
    }

    public ToolExecutionAdvisor(int order) {
        this(order, null, null, null);
    }

    public ToolExecutionAdvisor(Map<String, FunctionCallback> callbackMap,
                                Map<String, Integer> toolSameCallCountMap,
                                SseEmitter sseEmitter) {
        this(0, callbackMap, toolSameCallCountMap, sseEmitter);
    }

    public ToolExecutionAdvisor(int order,
                                Map<String, FunctionCallback> callbackMap,
                                Map<String, Integer> toolSameCallCountMap,
                                SseEmitter sseEmitter) {
        this.order = order;
        this.callbackMap = callbackMap;
        this.toolSameCallCountMap = toolSameCallCountMap;
        this.sseEmitter = sseEmitter;
    }

    @Override
    public String getName() {
        return "ToolExecutionAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    // ========== Call (non-streaming) ==========

    @Override
    @SuppressWarnings("unchecked")
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        if (response != null && response.chatResponse() != null && response.chatResponse().hasToolCalls()) {
            List<Message> messages = (List<Message>) chatClientRequest.context().get(MESSAGES_KEY);
            AtomicBoolean withToolCall = (AtomicBoolean) chatClientRequest.context().get(WITH_TOOL_CALL_KEY);

            if (withToolCall != null) {
                withToolCall.set(true);
            }

            response.chatResponse().getResults().stream()
                    .filter(g -> g.getOutput() != null
                            && g.getOutput().getToolCalls() != null
                            && !g.getOutput().getToolCalls().isEmpty())
                    .findFirst()
                    .ifPresent(generation -> executeTools(generation.getOutput(), callbackMap, messages, toolSameCallCountMap, chatClientRequest));
        }

        return response;
    }

    // ========== Stream ==========

    @Override
    @SuppressWarnings("unchecked")
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {

        List<Message> messages = (List<Message>) chatClientRequest.context().get(MESSAGES_KEY);
        AtomicBoolean withToolCall = (AtomicBoolean) chatClientRequest.context().get(WITH_TOOL_CALL_KEY);

        Flux<ChatClientResponse> stream = streamAdvisorChain.nextStream(chatClientRequest);

        return stream.doOnNext(response -> {
            if (response != null && response.chatResponse() != null
                    && response.chatResponse().hasToolCalls()) {

                if (withToolCall != null) {
                    withToolCall.set(true);
                }

                response.chatResponse().getResults().stream()
                        .filter(g -> g.getOutput() != null
                                && g.getOutput().getToolCalls() != null
                                && !g.getOutput().getToolCalls().isEmpty())
                        .findFirst()
                        .ifPresent(generation ->
                                executeTools(generation.getOutput(), callbackMap, messages, toolSameCallCountMap, chatClientRequest));
            }
        });
    }

    /**
     * 执行工具调用，写入消息历史和卡住检测。
     */
    @SuppressWarnings("unchecked")
    private void executeTools(AssistantMessage assistantMessage,
                              Map<String, FunctionCallback> callbackMap,
                              List<Message> messages,
                              Map<String, Integer> toolSameCallCountMap,
                              ChatClientRequest request) {
        if (callbackMap == null || messages == null) {
            return;
        }

        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        // 添加 assistant 消息到历史（包含 tool_call 声明）
        messages.add(assistantMessage);

        SseEmitter sse = getSseEmitter(request);

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            FunctionCallback callback = callbackMap.get(toolCall.name());
            if (callback == null) {
                throw new AgentToolException("无法匹配 toolFunction: " + toolCall.name());
            }

            // SSE 日志
            if (sse != null) {
                SseHelper.sendTool(sse, String.format("正在执行工具：%s", toolCall.name()));
                SseHelper.sendTool(sse, String.format("工具入参：%s", toolCall.arguments()));
            }

            String callToolResult = callback.call(toolCall.arguments());

            if (sse != null) {
                SseHelper.sendTool(sse, String.format("执行结果：%s", callToolResult));
            }

            // 保存到数据库
            ChatRecordSaver.addToolCall(
                    new FunctionUseAction(toolCall.name(), toolCall.arguments()),
                    callToolResult);

            // 记录卡住检测
            if (toolSameCallCountMap != null) {
                String actionKey = toolCall.name() + ":" + LoopDetector.normalizeAndSHA256(
                        JSONObject.parseObject(toolCall.arguments()));
                toolSameCallCountMap.put(actionKey,
                        toolSameCallCountMap.getOrDefault(actionKey, 0) + 1);
            }

            // ToolResponseMessage 加入历史
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
            responses.add(new ToolResponseMessage.ToolResponse(
                    StringUtils.hasText(toolCall.id()) ? toolCall.id() : UUID.randomUUID().toString(),
                    toolCall.name(),
                    callToolResult));
            messages.add(ToolResponseMessage.builder().responses(responses).build());
        }
    }

    private SseEmitter getSseEmitter(ChatClientRequest request) {
        if (request == null || request.context() == null) {
            return sseEmitter;
        }
        Object sseObj = request.context().get(SSE_EMITTER_KEY);
        if (sseObj instanceof SseEmitter) {
            return (SseEmitter) sseObj;
        }
        // 如果 context 中没有，使用构造函数注入的
        return sseEmitter;
    }
}
