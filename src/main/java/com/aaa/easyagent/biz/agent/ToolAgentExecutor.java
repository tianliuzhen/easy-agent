package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.model.AgentFinish;
import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.common.llm.deepseek.OpenAiChatOptions;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.core.domain.model.AgentModel;
import com.aaa.easyagent.core.domain.model.ToolModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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


    public ToolAgentExecutor(AgentModel agentModel) {
        super(agentModel);
    }

    /**
     * 构建提示词
     *
     * @return
     */
    public Prompt buildPrompt() {
        List<Message> messages = new ArrayList<>();
        List<ToolModel> toolModels = agentModel.getToolModels();
        Map<String, FunctionCallback> callbackMap = buildToolFun(toolModels);

        // tool 模式
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .toolCallbacks(callbackMap.values().stream().toList())
                .internalToolExecutionEnabled(false) // 禁用内部工具执行
                .build();

        return new Prompt(messages, chatOptions);
    }

    @Override
    public AgentOutput run() {

        ChatResponse chatResponse = chatModel.call(prompt);

        // 添加助手执行记忆
        addAssistantMessage(prompt, chatResponse);

        if (prompt.getOptions() != null && chatResponse.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResults().stream().flatMap(e -> e.getOutput().getToolCalls().stream()).toList();
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                // 可并行调用 todo
                toolExecute(new FunctionUseAction(toolCall.name(), toolCall.arguments()));
            }

            return new AgentOutput();
        }

        return new AgentFinish(ChatResponseUtil.getResStr(chatResponse), ChatResponseUtil.getResStr(chatResponse));
    }
}
