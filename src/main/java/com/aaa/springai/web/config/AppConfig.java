package com.aaa.springai.web.config;

import com.aaa.springai.web.function.tool.MockWeatherService;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.function.Function;

/**
 * @author liuzhen.tian
 * @version 1.0 AppConfig.java  2024/12/28 20:23
 */
@Configuration
public class AppConfig {

    @Bean
    @Description("Get the weather in location") // function description
    public Function<MockWeatherService.Request, MockWeatherService.Response> currentWeather() {
        return new MockWeatherService();
    }


    /**
     * OllamaEmbeddingModel 默认模型是： org.springframework.ai.ollama.api.OllamaModel#MXBAI_EMBED_LARGE
     *
     * @param ollamaEmbeddingModel
     * @return
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(OllamaEmbeddingModel ollamaEmbeddingModel) {
        return new SimpleVectorStore(ollamaEmbeddingModel);
    }


    /**
     * fix for：org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration#vectorStore(org.springframework.ai.embedding.EmbeddingModel, org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties, org.springframework.data.redis.connection.jedis.JedisConnectionFactory, org.springframework.beans.factory.ObjectProvider, org.springframework.beans.factory.ObjectProvider, org.springframework.ai.embedding.BatchingStrategy)
     * 因为同时集成了ollma和open-ai，默认注入会失败，因此手动注入
     * Parameter 0 of method vectorStore in org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration required a single bean, but 2 were found:
     * - ollamaEmbeddingModel: defined by method 'ollamaEmbeddingModel' in class path resource [org/springframework/ai/autoconfigure/ollama/OllamaAutoConfiguration.class]
     * - openAiEmbeddingModel: defined by method 'openAiEmbeddingModel' in class path resource [org/springframework/ai/autoconfigure/openai/OpenAiAutoConfiguration.class]
     *
     * @param ollamaEmbeddingModel
     * @param properties
     * @param jedisConnectionFactory
     * @param observationRegistry
     * @param customObservationConvention
     * @param batchingStrategy
     * @return
     * @see org.springframework.boot.autoconfigure.data.redis.JedisConnectionConfiguration.JedisConnectionConfiguration
     * @see org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisVectorStore redisVectorStore(EmbeddingModel ollamaEmbeddingModel, RedisVectorStoreProperties properties,
                                             JedisConnectionFactory jedisConnectionFactory, ObjectProvider<ObservationRegistry> observationRegistry,
                                             ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                             BatchingStrategy batchingStrategy) {

        var config = RedisVectorStore.RedisVectorStoreConfig.builder()
                .withIndexName(properties.getIndex())
                .withPrefix(properties.getPrefix())
                .build();

        // JedisPooled jedis = new JedisPooled(jedisConnectionFactory.getHostName(),
        //         jedisConnectionFactory.getPort(),
        //         jedisConnectionFactory.getClientName()
        //         , jedisConnectionFactory.getPassword()
        // );
        int port = jedisConnectionFactory.getPort();
        String host = jedisConnectionFactory.getHostName();
        int database = jedisConnectionFactory.getDatabase();
        String password = jedisConnectionFactory.getPassword();
        JedisPooled jedisPooled = new JedisPooled(
                (new HostAndPort(host, port)),
                DefaultJedisClientConfig.builder().database(database).password(password).build()
        );
        return new RedisVectorStore(config, ollamaEmbeddingModel,
                jedisPooled,
                properties.isInitializeSchema(), observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                customObservationConvention.getIfAvailable(() -> null), batchingStrategy);
    }
}
