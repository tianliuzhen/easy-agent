package com.aaa.easyagent.common.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmAutoConfiguration.java  2025/6/8 19:44
 */

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(CommonLlmChatModel.class)
@EnableConfigurationProperties(CommonLLmProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.dp", name = "api-key")
public class CommonLlmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonLlmApi commonLlmApi(CommonLLmProperties properties) {
        return new CommonLlmApi(properties);
    }

    @Bean(name = "clChatModel")
    @ConditionalOnMissingBean
    public CommonLlmChatModel clChatModel(CommonLlmApi commonLlmApi, CommonLLmProperties properties, ToolCallingManager toolCallingManager)

    {
        return new CommonLlmChatModel(commonLlmApi, properties, toolCallingManager);
    }
}
