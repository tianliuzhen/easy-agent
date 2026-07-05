package com.aaa.easyagent.common.config.vectorstore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaInitializationProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * embedding 提供方开关：easy-agent.embedding.provider = ollama | openai。
 * 统一暴露一个名为 activeEmbeddingModel 的 Bean，供 VectorStoreRegister 等注入使用。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Configuration
public class EmbeddingModelConfig {

    /**
     * @see OpenAiEmbeddingAutoConfiguration#openAiEmbeddingModel(OpenAiCommonProperties, OpenAiEmbeddingProperties, ObjectProvider, ObjectProvider, ObjectProvider, ObjectProvider)
     * @see OllamaEmbeddingAutoConfiguration#ollamaEmbeddingModel(OllamaApi, OllamaEmbeddingProperties, OllamaInitializationProperties, ObjectProvider, ObjectProvider)
     * @param provider
     * @param ollamaEmbeddingModel
     * @param openAiEmbeddingModel
     * @return
     */
    @Bean("activeEmbeddingModel")
    EmbeddingModel activeEmbeddingModel(
            @Value("${easy-agent.embedding.provider:ollama}") String provider,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel openAiEmbeddingModel) {
        if ("openai".equalsIgnoreCase(provider)) {
            log.info("当前 embedding provider=openai，使用 OpenAI 兼容接口（百炼 text-embedding-v4）");
            return openAiEmbeddingModel;
        }
        log.info("当前 embedding provider=ollama，使用本地 Ollama embedding");
        return ollamaEmbeddingModel;
    }
}
