package com.aaa.easyagent.common.llm;

import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.model.AgentModel;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.Map;

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

    private static Map<ModelTypeEnum, ChatModel> chatModelMap;

    @PostConstruct
    public void init() {
        chatModelMap = new HashMap<>();
        chatModelMap.put(ModelTypeEnum.ollama, ollamaChatModel);
        chatModelMap.put(ModelTypeEnum.deepseek, deepSeekChatModel);
        chatModelMap.put(ModelTypeEnum.openai, openAiChatModel);
    }

    /**
     * 根据AgentID切换大模型
     *
     * @param agentModel
     * @return
     */
    public static ChatModel getModel(AgentModel agentModel) {
        return chatModelMap.get(agentModel.getModelType());
    }
}
