package com.aaa.springai.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmApi.java  2025/6/8 19:16
 */

import com.aaa.springai.llm.deepseek.OpenAiApi;
import com.aaa.springai.util.JacksonUtil;
import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@Data
public class CommonLlmApi {
    private final RestClient restClient;
    private final WebClient webClient;
    private final CommonLLmProperties properties;
    private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    public CommonLlmApi(CommonLLmProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ResponseEntity<ChatCompletion> chatCompletion(ChatCompletionRequest request) {
        ResponseEntity<ChatCompletion> entity = this.restClient.post()
                .uri(properties.getChat().getCompletionsPath())
                .body(request)
                .retrieve()
                .toEntity(ChatCompletion.class);
        return entity;
    }

    public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest request) {
        request.setStream(true);

        return this.webClient.post()
                .uri(properties.getChat().getCompletionsPath())
                // .bodyValue(request)
                .body(Mono.just(request), ChatCompletionRequest.class)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                // cancels the flux stream after the "[DONE]" is received.
                .takeUntil(SSE_DONE_PREDICATE)
                // filters out the "[DONE]" message.
                .filter(SSE_DONE_PREDICATE.negate())
                .map(this::parseEvent); // 自定义解析
    }

    private ChatCompletion parseEvent(String event) {
        ChatCompletion chatCompletion = new ChatCompletion();
        try {
            // 假设每个事件是有效的JSON对象
            chatCompletion = JacksonUtil.strToBean(event, ChatCompletion.class);
        } catch (Exception e) {
            // "[DONE]"
            log.warn("Failed to parse event:{}", event);
            return new ChatCompletion();
        }
        return chatCompletion;
    }


    // 请求和响应类保持不变
    @Data
    @NoArgsConstructor
    public static class ChatCompletionRequest {
        private String model;
        private List<ChatCompletionMessage> messages;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Boolean stream = false;
        @JsonProperty("tools")
        private List<FunctionTool> tools;

        public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
            this.messages = messages;
            this.stream = stream;
        }
    }

    /**
     * @see OpenAiApi.ChatCompletion
     */
    // @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ChatCompletion {
        protected String id;
        protected String object;
        protected Long created;
        protected String model;
        protected List<Choice> choices;
        protected Usage usage;
        @JsonProperty("system_fingerprint")
        protected String systemFingerprint;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private Integer index;
            private ChatCompletionMessage message;
            private String finishReason;

            /**
             * 流式交互参数
             */
            private Delta delta;
        }


    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolCall {
        @JsonProperty("index")
        private Integer index;
        @JsonProperty("id")
        private String id;
        @JsonProperty("type")
        private String type;
        @JsonProperty("function")
        private ChatCompletionFunction function;

        public ToolCall(String id, String type, ChatCompletionFunction function) {
            this.id = id;
            this.type = type;
            this.function = function;
        }
    }

    /**
     * @see com.aaa.springai.llm.deepseek.OpenAiApi.ChatCompletionMessage
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    public static class ChatCompletionMessage {
        /**
         * @JsonProperty("content") Object rawContent,
         * @JsonProperty("role") ChatCompletionMessage.Role role,
         * @JsonProperty("reasoning_content") String reasoningContent,
         * @JsonProperty("name") String name,
         * @JsonProperty("tool_call_id") String toolCallId,
         * @JsonProperty("tool_calls")
         * @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<ChatCompletionMessage.ToolCall> toolCalls,
         * @JsonProperty("refusal") String refusal,
         */

        private String role;
        private String content;
        @JsonProperty("reasoning_content")
        private String reasoningContent;
        private String name;
        @JsonProperty("tool_call_id")
        private String toolCallId;
        @JsonProperty("tool_calls")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<ToolCall> toolCalls;
        @JsonProperty("refusal")
        private String refusal;

        public ChatCompletionMessage(String content, String role) {
            this.role = role;
            this.content = content;
        }

        public ChatCompletionMessage(String content, String role, List<ToolCall> toolCalls) {
            this.role = role;
            this.content = content;
            this.toolCalls = toolCalls;
        }

        public ChatCompletionMessage(String content, String role, String reasoningContent, String name, String toolCallId, List<ToolCall> toolCalls, String refusal) {
            this.role = role;
            this.content = content;
            this.reasoningContent = reasoningContent;
            this.name = name;
            this.toolCallId = toolCallId;
            this.toolCalls = toolCalls;
            this.refusal = refusal;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionFunction { // @formatter:on
        // @formatter:off
        @JsonProperty("name")
        private String name;
        @JsonProperty("arguments")
        private String arguments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String role;
        // 回答内容
        private String content;
        // 回答推理
        private String reasoning_content;
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }


    /**
     * Represents a tool the model may call. Currently, only functions are supported as a
     * tool.
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionTool {

        /**
         * The type of the tool. Currently, only 'function' is supported.
         */
        @JsonProperty("type")
        private FunctionTool.Type type = FunctionTool.Type.FUNCTION;

        /**
         * The function definition.
         */
        @JsonProperty("function")
        private FunctionTool.Function function;

        public FunctionTool() {

        }

        /**
         * Create a tool of type 'function' and the given function definition.
         *
         * @param type     the tool type
         * @param function function definition
         */
        public FunctionTool(FunctionTool.Type type,FunctionTool.Function function) {
            this.type = type;
            this.function = function;
        }

        /**
         * Create a tool of type 'function' and the given function definition.
         *
         * @param function function definition.
         */
        public FunctionTool(FunctionTool.Function function) {
            this(FunctionTool.Type.FUNCTION, function);
        }


        /**
         * Create a tool of type 'function' and the given function definition.
         */
        public enum Type {

            /**
             * Function tool type.
             */
            @JsonProperty("function")
            FUNCTION

        }

        /**
         * Function definition.
         */
        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Function {

            @JsonProperty("description")
            private String description;

            @JsonProperty("name")
            private String name;

            @JsonProperty("parameters")
            private Map<String, Object> parameters;

            @JsonProperty("strict")
            Boolean strict;

            @JsonIgnore
            private String jsonSchema;

            /**
             * NOTE: Required by Jackson, JSON deserialization!
             */
            @SuppressWarnings("unused")
            private Function() {
            }

            /**
             * Create tool function definition.
             *
             * @param description A description of what the function does, used by the
             *                    model to choose when and how to call the function.
             * @param name        The name of the function to be called. Must be a-z, A-Z, 0-9,
             *                    or contain underscores and dashes, with a maximum length of 64.
             * @param parameters  The parameters the functions accepts, described as a JSON
             *                    Schema object. To describe a function that accepts no parameters, provide
             *                    the value {"type": "object", "properties": {}}.
             * @param strict      Whether to enable strict schema adherence when generating the
             *                    function call. If set to true, the model will follow the exact schema
             *                    defined in the parameters field. Only a subset of JSON Schema is supported
             *                    when strict is true.
             */
            public Function(String description, String name, Map<String, Object> parameters, Boolean strict) {
                this.description = description;
                this.name = name;
                this.parameters = parameters;
                this.strict = strict;
            }

            /**
             * Create tool function definition.
             *
             * @param description tool function description.
             * @param name        tool function name.
             * @param jsonSchema  tool function schema as json.
             */
            public Function(String description, String name, String jsonSchema) {
                this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema), null);
            }



            public String getJsonSchema() {
                return this.jsonSchema;
            }

            public void setJsonSchema(String jsonSchema) {
                this.jsonSchema = jsonSchema;
                if (jsonSchema != null) {
                    this.parameters = ModelOptionsUtils.jsonToMap(jsonSchema);
                }
            }

        }

    }

}
