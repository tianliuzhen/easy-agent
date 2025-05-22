package com.aaa.springai.config;

/**
 * @author liuzhen.tian
 * @version 1.0 RedisVectorStoreAutoConfiguration.java  2025/5/22 21:58
 */

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * from: org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration
 * <p>
 * {@link AutoConfiguration Auto-configuration} for Redis Vector Store.
 *
 * @author Christian Tzolov
 * @author Eddú Meléndez
 * @author Soby Chacko
 * @author Jihoon Kim
 */
@Slf4j
@Configuration
public class RedisVectorStoreAutoConfiguration {

    /**
     * fix for：org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration#vectorStore(org.springframework.ai.embedding.EmbeddingModel, org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties, org.springframework.docDetailInfo.redis.connection.jedis.JedisConnectionFactory, org.springframework.beans.factory.ObjectProvider, org.springframework.beans.factory.ObjectProvider, org.springframework.ai.embedding.BatchingStrategy)
     * 因为同时集成了ollma和open-ai，默认注入会失败，因此手动注入
     * Parameter 0 of method vectorStore in org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration required a single bean, but 2 were found:
     * - ollamaEmbeddingModel: defined by method 'ollamaEmbeddingModel' in class path resource [org/springframework/ai/autoconfigure/ollama/OllamaAutoConfiguration.class]
     * - openAiEmbeddingModel: defined by method 'openAiEmbeddingModel' in class path resource [org/springframework/ai/autoconfigure/openai/OpenAiAutoConfiguration.class]
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisVectorStore redisVectorStore(EmbeddingModel ollamaEmbeddingModel, RedisVectorStoreProperties properties,
                                        JedisConnectionFactory jedisConnectionFactory, ObjectProvider<ObservationRegistry> observationRegistry,
                                        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                        BatchingStrategy batchingStrategy) {

        RedisVectorStore build = null;
        try {
            JedisPooled jedisPooled = this.jedisPooled(jedisConnectionFactory);
            build = RedisVectorStore.builder(jedisPooled, ollamaEmbeddingModel)
                    .initializeSchema(properties.isInitializeSchema())
                    .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                    .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                    .batchingStrategy(batchingStrategy)
                    .indexName(properties.getIndex())
                    .prefix(properties.getPrefix())
                    .build();
        } catch (Throwable e) {
            log.error("vectorStore.err:", e);
        }
        return build;
    }

    private JedisPooled jedisPooled(JedisConnectionFactory jedisConnectionFactory) {

        String host = jedisConnectionFactory.getHostName();
        int port = jedisConnectionFactory.getPort();

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(jedisConnectionFactory.isUseSsl())
                .clientName(jedisConnectionFactory.getClientName())
                .timeoutMillis(jedisConnectionFactory.getTimeout())
                .password(jedisConnectionFactory.getPassword())
                .database(jedisConnectionFactory.getDatabase())
                .build();

        return new JedisPooled(new HostAndPort(host, port), clientConfig);
    }

}
