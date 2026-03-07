package com.aaa.easyagent.common.llm;

import com.aaa.easyagent.common.llm.common.CommonLLmProperties;
import com.aaa.easyagent.common.llm.common.CommonLlmApi;
import com.aaa.easyagent.common.llm.common.CommonLlmChatModel;
import com.aaa.easyagent.common.llm.common.CommonLlmChatOptions;
import com.aaa.easyagent.common.util.SpringContextUtil;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
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
        // 模型调用参数配置：model topK topP temperature 等
        CommonLLmProperties commonLLmProperties = buildCommonLLmProperties(agentContext);

        // 大模型调用客户端
        CommonLlmApi commonLlmApi = new CommonLlmApi(commonLLmProperties);

        // 工具管理器
        ToolCallingManager toolCallingManager = SpringContextUtil.getBean(ToolCallingManager.class);

        return new CommonLlmChatModel(commonLlmApi, commonLLmProperties, toolCallingManager);
    }

    private static CommonLLmProperties buildCommonLLmProperties(AgentContext agentContext) {
        CommonLLmProperties commonLLmProperties = new CommonLLmProperties();
        commonLLmProperties.setBaseUrl(agentContext.getAgentModelConfig().getBaseUrl());
        commonLLmProperties.setApiKey(agentContext.getAgentModelConfig().getApiKey());

        CommonLLmProperties.ChatProperties lLmProperties = new CommonLLmProperties.ChatProperties();
        lLmProperties.setCompletionsPath(StringUtils.defaultIfBlank(agentContext.getAgentModelConfig().getCompletionsPath(), CommonLLmProperties.ChatProperties.DEFAULT_COMPLETIONS_PATH));

        // todo 模型各项配置参数暂时不全
        CommonLlmChatOptions options = CommonLlmChatOptions.builder()
                .model(agentContext.getAgentModelConfig().getModelVersion())
                .temperature(CommonLLmProperties.ChatProperties.DEFAULT_TEMPERATURE)
                .topP(agentContext.getAgentModelConfig().getTopP())
                .build();
        lLmProperties.setOptions(options);
        commonLLmProperties.setChat(lLmProperties);
        return commonLLmProperties;
    }
}
