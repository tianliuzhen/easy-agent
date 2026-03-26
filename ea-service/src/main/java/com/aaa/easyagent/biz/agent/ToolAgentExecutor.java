package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.common.llm.common.CommonLlmChatOptions;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.*;

/**
 * @author liuzhen.tian
 * @version 1.0 ReActAgentExecutor.java  2025/2/23 20:45
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

        // tool 模式
        CommonLlmChatOptions chatOptions = CommonLlmChatOptions.builder()
                .toolCallbacks(callbackMap.values().stream().toList())
                .internalToolExecutionEnabled(false) // 禁用内部工具执行
                .build();

        return new Prompt(messages, chatOptions);
    }

    @Override
    public AgentOutput run() {

        ChatResponse chatResponse = chatModel.call(prompt);

        // 添加助手执行记忆
        addAssistantMessage(chatResponse);

        if (prompt.getOptions() != null && chatResponse.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResults().stream().flatMap(e -> e.getOutput().getToolCalls().stream()).toList();
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                // 可并行调用 todo
                toolExecute(new FunctionUseAction(toolCall.name(), toolCall.arguments()));
            }

            return new AgentOutput();
        }

        return new AgentFinish(ChatResponseUtil.getResStr(chatResponse));
    }
}
