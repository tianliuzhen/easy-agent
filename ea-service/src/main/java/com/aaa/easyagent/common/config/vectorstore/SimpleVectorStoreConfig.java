package com.aaa.easyagent.common.config.vectorstore;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;

/**
 * @author liuzhen.tian
 * @version 1.0 SimpleVectorStoreConfig.java  2025/11/9 22:07
 */

@Configuration
public class SimpleVectorStoreConfig {
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
     * 内存型缓存
     *
     * @return
     */
    @Bean
    public ChatMemory InMemoryChatMemory() {
        return MessageWindowChatMemory.builder().build();
    }
}
