package com.aaa.springai.llm.dp;

/**
 * @author liuzhen.tian
 * @version 1.0 DeepseekAutoConfiguration.java  2025/6/8 19:44
 */

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(DeepseekChatModel.class)
@EnableConfigurationProperties(DeepseekProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.dp", name = "api-key")
public class DeepseekAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DeepseekApiClient deepseekApiClient(DeepseekProperties properties) {
        return new DeepseekApiClient(properties);
    }

    @Bean(name = "dpChatModel")
    @ConditionalOnMissingBean
    public DeepseekChatModel dpChatModel(DeepseekApiClient apiClient, DeepseekProperties properties) {
        return new DeepseekChatModel(apiClient, properties);
    }
}
