package com.aaa.springai.web.config;

import com.aaa.springai.web.function.tool.MockWeatherService;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

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


}
