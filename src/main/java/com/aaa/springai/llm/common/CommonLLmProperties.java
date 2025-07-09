package com.aaa.springai.llm.common;

import com.aaa.springai.llm.deepseek.OpenAiChatOptions;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLLmProperties.java  2025/6/8 19:29
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.dp")
public class CommonLLmProperties {

    /**
     * DeepSeek API base URL
     */
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * DeepSeek API key
     */
    private String apiKey;


    @NestedConfigurationProperty
    private ChatProperties chat;

    @Data
    public static class ChatProperties {

        public static final String DEFAULT_COMPLETIONS_PATH = "/v1/chat/completions";

        private static final Double DEFAULT_TEMPERATURE = 0.7;

        /**
         * Enable OpenAI chat model.
         */
        private boolean enabled = true;

        private String completionsPath = DEFAULT_COMPLETIONS_PATH;

        @NestedConfigurationProperty
        private OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(DEFAULT_TEMPERATURE)
                .build();
    }
}
