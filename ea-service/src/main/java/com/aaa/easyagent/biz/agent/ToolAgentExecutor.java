package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.advisor.SseAdvisor;
import com.aaa.easyagent.biz.agent.advisor.ToolExecutionAdvisor;
import com.aaa.easyagent.biz.agent.context.FunctionCallbackAdapter;
import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI ChatClient 的 Tool 模式执行器。
 * <p>
 * 使用 ChatClient 的 Fluent API + Advisor 替代直接操作 ChatModel + CommonLlmChatOptions。
 * </p>
 *
 * <pre>
 * 执行流程（由父类 doExec() 控制外部循环）：
 *   1. buildPrompt() → 初始化空 Prompt
 *   2. addUserMessage() → 将用户问题存入 this.messages
 *   3. run() → ChatClient 构建请求并流式调用 LLM
 *   4. {@link ToolExecutionAdvisor} 拦截 tool_call → 自动执行工具 → 结果存入 this.messages
 *   5. doExec() 下一轮 → run() 从 this.messages 构建包含历史的消息列表
 * </pre>
 *
 * @author liuzhen.tian
 * @version 1.0 ToolAgentExecutor.java  2025/2/23 20:45
 */
@Slf4j
public class ToolAgentExecutor extends BaseAgent {

    /**
     * ChatClient，已通过 defaultSystem/defaultTools/defaultAdvisors 配置
     */
    private final ChatClient chatClient;


    public ToolAgentExecutor(AgentContext agentContext) {
        super(agentContext);

        // 将 FunctionCallback 适配为 Spring AI ToolCallback
        List<ToolCallback> toolCallbacks = callbackMap.values().stream()
                .map(FunctionCallbackAdapter::new)
                .collect(Collectors.toList());

        // 不变的部分在构造函数一次性配置：system、tools、advisors
        // 以及通过构造函数注入到 Advisor 中的 sse、callbackMap、toolSameCallCountMap
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(agentContext.getPrompt() != null ? agentContext.getPrompt() : "")
                .defaultToolCallbacks(toolCallbacks)
                .defaultAdvisors(new SseAdvisor(sse),
                        new ToolExecutionAdvisor(callbackMap, toolSameCallCountMap, sse))
                .build();
    }

    @Override
    public Prompt buildPrompt() {
        return new Prompt(new ArrayList<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentOutput run() {
        AtomicBoolean withToolCall = new AtomicBoolean(false);
        StringBuilder resStr = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();

        var spec = chatClient.prompt();

        // 注入历史消息（用户问题 + 之前的 assistant/tool 消息）
        if (!messages.isEmpty()) {
            spec.messages(messages.toArray(new Message[0]));
        }

        // 每轮变化的参数（messages、withToolCall）通过 advisor params 传入
        spec.advisors(as -> {
            as.param(ToolExecutionAdvisor.MESSAGES_KEY, messages);
            as.param(ToolExecutionAdvisor.WITH_TOOL_CALL_KEY, withToolCall);
        });

        Flux<ChatResponse> stream = spec.stream().chatResponse();

        CountDownLatch runOver = new CountDownLatch(1);

        stream.subscribe(chatRes -> {
            // 思考过程（SSE 由 SseAdvisor 推送，这里只做内存汇总）
            String lineThinks = ChatResponseUtil.getReasoningContent(chatRes);
            if (StringUtils.hasText(lineThinks)) {
                reasoningContent.append(lineThinks);
            }

            // 结果文本
            String lineResStr = ChatResponseUtil.getResStr(chatRes);
            if (StringUtils.hasText(lineResStr)) {
                resStr.append(lineResStr);
            }
        }, e -> {
            log.error("ToolAgentExecutor.run.error", e);
            SseHelper.sendData(sse, "系统异常：" + e.getMessage());
            runOver.countDown();
        }, () -> {
            log.info("ToolAgentExecutor.run.complete");
            runOver.countDown();
        });

        try {
            runOver.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (withToolCall.get()) {
            return new FunctionUseAction("", "");
        }

        return new AgentFinish(resStr.toString());
    }

    @Override
    protected void addUserMessage(String question) {
        messages.add(new org.springframework.ai.chat.messages.UserMessage(question));
    }
}
