package com.aaa.springai.config;

import com.aaa.springai.function.tool.MockWeatherService;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.lang.reflect.Constructor;
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
        //  通过Java的反射机制，你可以访问并调用受保护的构造函数
        try {
            // 假设第三方包的类路径为 "com.thirdparty.SimpleVectorStore"
            // 请根据实际情况替换为正确的类路径
            Class<?> clazz = Class.forName("org.springframework.ai.vectorstore.SimpleVectorStore");

            // 获取私有构造函数 SimpleVectorStore(SimpleVectorStoreBuilder builder)
            Constructor<?> constructor = clazz.getDeclaredConstructor(
                    clazz.getDeclaredClasses()[0] == SimpleVectorStore.SimpleVectorStoreBuilder.class
                            ? SimpleVectorStore.SimpleVectorStoreBuilder.class
                            : null);
            constructor.setAccessible(true);

            // 创建 SimpleVectorStoreBuilder 实例
            // 假设 SimpleVectorStoreBuilder 有一个无参构造函数

            // 调用私有构造函数创建 SimpleVectorStore 实例
            Object simpleVectorStore = constructor.newInstance(SimpleVectorStore.builder(ollamaEmbeddingModel));

            return (SimpleVectorStore) simpleVectorStore;

        } catch (Exception e) {
            System.err.println("simpleVectorStore.found: " + e.getMessage());
            return null;
        }
    }


    /**
     * fix for：org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration#vectorStore(org.springframework.ai.embedding.EmbeddingModel, org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties, org.springframework.docDetailInfo.redis.connection.jedis.JedisConnectionFactory, org.springframework.beans.factory.ObjectProvider, org.springframework.beans.factory.ObjectProvider, org.springframework.ai.embedding.BatchingStrategy)
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


        return RedisVectorStore.builder(jedisPooled, ollamaEmbeddingModel)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .indexName(properties.getIndex())
                .prefix(properties.getPrefix())
                .build();

    }

    /**
     * 内存型缓存
     *
     * @return
     */
    @Bean
    public ChatMemory InMemoryChatMemory() {
        return new InMemoryChatMemory();
    }
}
