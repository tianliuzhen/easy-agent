package com.aaa.easyagent.common.llm;

import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.model.AgentModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 大模型选择器
 *
 * @author liuzhen.tian
 * @version 1.0 LLmModelSelector.java  2025/5/25 17:59
 */
@Component
public class LLmModelSelector {

    @Autowired
    private OpenAiChatModel deepSeekChatModel;
    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private OllamaChatModel ollamaChatModel;

    /**
     * 根据AgentID切换大模型
     *
     * @param agentModel
     * @return
     */
    public ChatModel getModel(AgentModel agentModel) {
        if (agentModel.getModelType() == ModelTypeEnum.ollama) {
            return ollamaChatModel;
        }
        if (agentModel.getModelType() == ModelTypeEnum.deepseek) {
            return deepSeekChatModel;
        }
        if (agentModel.getModelType() == ModelTypeEnum.openai) {
            return openAiChatModel;
        }
        return null;
    }
}
