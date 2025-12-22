package com.aaa.easyagent.common.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmApi.java  2025/6/8 19:16
 */

import com.aaa.easyagent.common.llm.deepseek.OpenAiApi;
import com.aaa.easyagent.common.util.JacksonUtil;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey()).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
        this.webClient = WebClient.builder().baseUrl(properties.getBaseUrl()).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey()).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    public ResponseEntity<ChatCompletion> chatCompletion(ChatCompletionRequest request) {
        ResponseEntity<ChatCompletion> entity = this.restClient.post().uri(properties.getChat().getCompletionsPath()).body(request).retrieve().toEntity(ChatCompletion.class);
        return entity;
    }

    public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest request) {
        // 1. 设置请求为流式模式
        request.setStream(true);

        // 2. 状态标志：标记是否正在处理工具调用块
        AtomicBoolean isInsideTool = new AtomicBoolean(false);

        // 3. 发起WebClient请求（SSE流）
        return this.webClient.post().uri(properties.getChat().getCompletionsPath())
                // .bodyValue(request)
                .body(Mono.just(request), ChatCompletionRequest.class)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                // 4. 流处理管道
                .takeUntil(SSE_DONE_PREDICATE) // 遇到"[DONE]"时终止流
                .filter(SSE_DONE_PREDICATE.negate())  // 过滤掉"[DONE]"消息
                .map(this::parseEvent) // 解析SSE事件为ChatCompletion对象
                // 5. 标记工具调用块的开始/结束
                .map(chunk -> { // 自定义解析
                    if (CommonLlmApiHelper.isStreamingToolFunctionCall(chunk)) {
                        isInsideTool.set(true);
                    }
                    return chunk;
                })
                // 6. 按工具调用块分组（windowUntil）
                // Group all chunks belonging to the same function call.
                // Flux<ChatCompletionChunk> -> Flux<Flux<ChatCompletionChunk>>
                .windowUntil(chunk -> {
                    if (isInsideTool.get() && CommonLlmApiHelper.isStreamingToolFunctionCallFinish(chunk)) {
                        isInsideTool.set(false);
                        return true;
                    }
                    return !isInsideTool.get();
                })
                // 7. 合并每个窗口内的分块
                // Merging the window chunks into a single chunk.
                // Reduce the inner Flux<ChatCompletionChunk> window into a single
                // Mono<ChatCompletionChunk>,
                // Flux<Flux<ChatCompletionChunk>> -> Flux<Mono<ChatCompletionChunk>>
                .concatMapIterable(window -> {
                    Mono<CommonLlmApi.ChatCompletion> monoChunk = window.reduce(
                            new CommonLlmApi.ChatCompletion(null, null, null, null, null, null, null, null),
                            (previous, current) -> CommonLlmApiHelper.merge(previous, current));
                    return List.of(monoChunk);
                })
                .flatMap(mono -> mono);
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletion {
        protected String id;
        protected String object;
        protected Long created;
        protected String model;
        protected List<Choice> choices;
        protected Usage usage;
        @JsonProperty("system_fingerprint")
        protected String systemFingerprint;
        @JsonProperty("service_tier")
        protected String serviceTier;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private Integer index;
            private ChatCompletionMessage message;
            /**
             * @see OpenAiApi.ChatCompletionFinishReason
             */
            private String finishReason;

            /**
             * 流式交互参数
             * 同于：com.aaa.springai.llm.common.CommonLlmApi.ChatCompletion.Choice#message
             */
            private ChatCompletionMessage delta;

            @JsonProperty("logprobs")
            private OpenAiApi.LogProbs logprobs;
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
     * @see OpenAiApi.ChatCompletionMessage
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
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
        private List<ToolCall> toolCalls;
        @JsonProperty("refusal")
        private String refusal;
        @JsonProperty("audio")
        private AudioOutput audioOutput;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionFunction { // @formatter:on
        // @formatter:off
        @JsonProperty("name")
        private String name;
        @JsonProperty("arguments")
        private String arguments;public ChatCompletionFunction(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        public ChatCompletionFunction() {
            System.out.println();
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getArguments() {
            return arguments;
        }
        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
    // @JsonInclude(JsonInclude.Include.NON_NULL)
    // public record ChatCompletionFunction(// @formatter:off
    //                                      @JsonProperty("name") String name,
    //                                      @JsonProperty("arguments") String arguments) { // @formatter:on
    // }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    /**
     * Audio response from the model.
     *
     * @param id         Unique identifier for the audio response from the model.
     * @param data       Audio output from the model.
     * @param expiresAt  When the audio content will no longer be available on the
     *                   server.
     * @param transcript Transcript of the audio output from the model.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AudioOutput(// @formatter:off
                              @JsonProperty("id") String id,
                              @JsonProperty("data") String data,
                              @JsonProperty("expires_at") Long expiresAt,
                              @JsonProperty("transcript") String transcript
    ) { // @formatter:on
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
        public FunctionTool(FunctionTool.Type type, FunctionTool.Function function) {
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
            @JsonProperty("function") FUNCTION

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

    /**
     * Log probability information for the choice.
     *
     * @param content A list of message content tokens with log probability information.
     * @param refusal A list of message refusal tokens with log probability information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LogProbs(
            @JsonProperty("content") List<OpenAiApi.LogProbs.Content> content,
            @JsonProperty("refusal") List<OpenAiApi.LogProbs.Content> refusal) {

        /**
         * Message content tokens with log probability information.
         *
         * @param token       The token.
         * @param logprob     The log probability of the token.
         * @param probBytes   A list of integers representing the UTF-8 bytes representation
         *                    of the token. Useful in instances where characters are represented by multiple
         *                    tokens and their byte representations must be combined to generate the correct
         *                    text representation. Can be null if there is no bytes representation for the
         *                    token.
         * @param topLogprobs List of the most likely tokens and their log probability, at
         *                    this token position. In rare cases, there may be fewer than the number of
         *                    requested top_logprobs returned.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Content(// @formatter:off
                              @JsonProperty("token") String token,
                              @JsonProperty("logprob") Float logprob,
                              @JsonProperty("bytes") List<Integer> probBytes,
                              @JsonProperty("top_logprobs") List<OpenAiApi.LogProbs.Content.TopLogProbs> topLogprobs) { // @formatter:on

            /**
             * The most likely tokens and their log probability, at this token position.
             *
             * @param token     The token.
             * @param logprob   The log probability of the token.
             * @param probBytes A list of integers representing the UTF-8 bytes
             *                  representation of the token. Useful in instances where characters are
             *                  represented by multiple tokens and their byte representations must be
             *                  combined to generate the correct text representation. Can be null if there
             *                  is no bytes representation for the token.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record TopLogProbs(// @formatter:off
                                      @JsonProperty("token") String token,
                                      @JsonProperty("logprob") Float logprob,
                                      @JsonProperty("bytes") List<Integer> probBytes) { // @formatter:on
            }

        }

    }

}
