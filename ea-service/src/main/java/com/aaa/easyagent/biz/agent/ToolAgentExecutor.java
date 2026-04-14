package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.context.FunctionCallbackAdapter;
import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.*;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.llm.common.CommonLlmChatOptions;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author liuzhen.tian
 * @version 1.0 ReActAgentOldExecutor.java  2025/2/23 20:45
 */
@Slf4j
public class ToolAgentExecutor extends BaseAgent {


    public ToolAgentExecutor(AgentContext agentContext) {
        super(agentContext);
    }

    /**
     * 构建提示词
     *
     * @return
     */
    public Prompt buildPrompt() {
        List<Message> messages = new ArrayList<>();
        List<ToolDefinition> toolDefinitions = agentContext.getToolDefinitions();
        Map<String, FunctionCallback> callbackMap = buildToolFun(toolDefinitions);

        // tool 模式 - 将自定义 FunctionCallback 转换为 Spring AI ToolCallback
        List<ToolCallback> toolCallbacks = callbackMap.values().stream()
                .map(FunctionCallbackAdapter::new)
                .collect(Collectors.toList());

        CommonLlmChatOptions chatOptions = CommonLlmChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false) // 禁用内部工具执行
                .build();

        return new Prompt(messages, chatOptions);
    }

    @Override
    public AgentOutput run() {
        StringBuilder resStr = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();

        Flux<ChatResponse> stream = chatModel.stream(prompt);

        StringBuilder sb = new StringBuilder();
        CountDownLatch runOver = new CountDownLatch(1);

        AtomicBoolean withToolCall = new AtomicBoolean(false);
        stream.subscribe(chatRes -> {
            if (chatRes.hasToolCalls()) {
                withToolCall.set(true);
                Generation generation = chatRes.getResults()
                        .stream()
                        .filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No tool call requested by the chat model"));
                AssistantMessage assistantMessage = generation.getOutput();

                toolExecute(assistantMessage);
            }

            // 思考
            String lineThinks = ChatResponseUtil.getReasoningContent(chatRes);
            reasoningContent.append(lineThinks);
            SseHelper.sendThink(sse, lineThinks);

            // 结果
            String lineResStr = ChatResponseUtil.getResStr(chatRes);
            resStr.append(lineResStr);
            SseHelper.sendData(sse, lineResStr);
        }, e -> {
            runOver.countDown();
            SseHelper.sendData(sse, "系统异常：" + e.getMessage());
            log.error("ToolAgentExecutor.think.err", e);
        }, () -> {
            runOver.countDown();
            log.info("ToolAgentExecutor.think.runOver");
        });

        try {
            runOver.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 保存到数据库前过滤XML标签
        ChatRecordSaver.addThinking(reasoningContent.toString());
        ChatRecordSaver.addData(resStr.toString());

        String chatResponse = sb.toString();
        if (withToolCall.get()) {
            return new FunctionUseAction("","");
        }

        return new AgentFinish(chatResponse);
    }

    /**
     * todo并行执行
     *
     * @param assistantMessage
     */
    protected void toolExecute(AssistantMessage assistantMessage) {
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        // 添加助手执行记忆 assistantMessage：ToolResponseMessage = 1：N
        addMessage(assistantMessage);
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            FunctionCallback functionToolCallback = callbackMap.get(toolCall.name());
            if (functionToolCallback == null) {
                throw new AgentToolException("无法匹配 toolFunction");
            }
            String callToolResult = functionToolCallback.call(toolCall.arguments());

            // 添加工具执行结果记忆
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
            responses.add(new ToolResponseMessage.ToolResponse(
                    StringUtils.defaultIfBlank(toolCall.id(), UUID.randomUUID().toString()),
                    toolCall.arguments(),
                    callToolResult));

            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder().responses(responses).build();

            /*
             * deepseek： Messages with role 'tool' must be a response to a preceding message with 'tool_calls
             * 你当前的设计其实是 ReAct 模式的工具调用（用 XML 标签控制流程），但混合了 OpenAI 的 tool calling 格式，导致冲突。
             * deepseek 这里不能是tool
             */

            // 添加工具执行结果记忆
            addMessage(toolResponseMessage);
        }


    }
}
