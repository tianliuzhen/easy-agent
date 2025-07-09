package com.aaa.springai.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmApiClient.java  2025/6/8 19:16
 */

import com.aaa.springai.util.JacksonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Data
public class CommonLlmApiClient {

    private final WebClient webClient;
    private final CommonLLmProperties properties;
    private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    public CommonLlmApiClient(CommonLLmProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<DeepseekChatCompletion> chatCompletion(DeepseekChatCompletionRequest request) {
        return this.webClient.post()
                .uri(properties.getChat().getCompletionsPath())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepseekChatCompletion.class);
    }

    public Flux<DeepseekChatCompletion> chatCompletionStream(DeepseekChatCompletionRequest request) {
        request.setStream(true);

        return this.webClient.post()
                .uri(properties.getChat().getCompletionsPath())
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                // cancels the flux stream after the "[DONE]" is received.
                .takeUntil(SSE_DONE_PREDICATE)
                // filters out the "[DONE]" message.
                .filter(SSE_DONE_PREDICATE.negate())
                .map(this::parseEvent); // 自定义解析
    }

    private DeepseekChatCompletion parseEvent(String event) {
        DeepseekChatCompletion deepseekChatCompletion = new DeepseekChatCompletion();
        try {
            // 假设每个事件是有效的JSON对象
            deepseekChatCompletion = JacksonUtil.strToBean(event, DeepseekChatCompletion.class);
        } catch (Exception e) {
            // "[DONE]"
            log.warn("Failed to parse event:{}", event);
            return new DeepseekChatCompletion();
        }
        return deepseekChatCompletion;
    }


    // 请求和响应类保持不变
    @Data
    public static class DeepseekChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Boolean stream = false;

        @Data
        public static class Message {
            private String role;
            private String content;
        }
    }

    // @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class DeepseekChatCompletion {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private Integer index;
            private Message message;
            private String finishReason;

            /**
             * 流失填充
             */
            private Delta delta;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Message {
                private String role;
                private String content;
                private String reasoning_content;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Delta {
                private String role;
                private String content;
                private String reasoning_content;
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {
            private Integer promptTokens;
            private Integer completionTokens;
            private Integer totalTokens;
        }
    }
}
