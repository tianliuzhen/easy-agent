package com.aaa.easyagent.common.config.vectorstore;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量库通用配置：与 provider（es / milvus）无关的公共 Bean。
 *
 * @author liuzhen.tian
 */
@Configuration
public class VectorStoreCommonConfig {

    @Bean
    @ConditionalOnMissingBean(BatchingStrategy.class)
    BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }
}
