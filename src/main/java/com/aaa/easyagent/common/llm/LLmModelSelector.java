package com.aaa.easyagent.common.llm;

import com.aaa.easyagent.common.llm.common.CommonLLmProperties;
import com.aaa.easyagent.common.llm.common.CommonLlmApi;
import com.aaa.easyagent.common.llm.common.CommonLlmChatModel;
import com.aaa.easyagent.common.llm.common.CommonLlmChatOptions;
import com.aaa.easyagent.common.util.SpringContextUtil;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
     * @param agentContext
     * @return
     */
    public static ChatModel getDefaultModel(AgentContext agentContext) {
        return chatModelMap.get(agentContext.getModelType());
    }

    /**
     * 构建ChatModel大模型
     *
     * @param agentContext
     * @return
     */
    public static ChatModel buildChatModel(AgentContext agentContext) {
        /**
         * 硅基流动 初始化
         */
        if (agentContext.getModelType() == ModelTypeEnum.siliconflow) {
            CommonLlmApi commonLlmApi = SpringContextUtil.getBean(CommonLlmApi.class);
            ToolCallingManager toolCallingManager = SpringContextUtil.getBean(ToolCallingManager.class);

            CommonLLmProperties commonLLmProperties = buildCommonLLmProperties(agentContext);

            return new CommonLlmChatModel(commonLlmApi, commonLLmProperties, toolCallingManager);
        }

        return chatModelMap.get(agentContext.getModelType());
    }

    private static CommonLLmProperties buildCommonLLmProperties(AgentContext agentContext) {
        CommonLLmProperties commonLLmProperties = new CommonLLmProperties();
        commonLLmProperties.setBaseUrl(agentContext.getAgentModelConfig().getBaseUrl());
        commonLLmProperties.setApiKey(agentContext.getAgentModelConfig().getApiKey());

        CommonLLmProperties.ChatProperties lLmProperties = new CommonLLmProperties.ChatProperties();
        lLmProperties.setCompletionsPath(agentContext.getAgentModelConfig().getCompletionsPath());
        // todo 模型各项配置参数暂时不全
        lLmProperties.setOptions(CommonLlmChatOptions.builder()
                .model(agentContext.getAgentModelConfig().getModelVersion())
                .build());
        commonLLmProperties.setChat(lLmProperties);
        return commonLLmProperties;
    }
}
