package com.aaa.springai.llm.common;

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
    public CommonLlmApi deepseekApiClient(CommonLLmProperties properties) {
        return new CommonLlmApi(properties);
    }

    @Bean(name = "dpChatModel")
    @ConditionalOnMissingBean
    public CommonLlmChatModel dpChatModel(CommonLlmApi apiClient, CommonLLmProperties properties, ToolCallingManager toolCallingManager)

    {
        return new CommonLlmChatModel(apiClient, properties, toolCallingManager);
    }
}
