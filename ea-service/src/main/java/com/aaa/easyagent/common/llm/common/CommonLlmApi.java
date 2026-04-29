package com.aaa.easyagent.common.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmApi.java  2025/6/8 19:16
 */

import com.aaa.easyagent.common.util.JacksonUtil;
import com.fasterxml.jackson.annotation.*;
import io.netty.channel.ChannelOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.time.Duration;
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

        // 配置HttpClient用于WebClient，优化SSE流式连接的超时和稳定性
        HttpClient webClientHttp = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 连接超时60秒（SSE建立连接可能需要更长时间）
                .option(ChannelOption.SO_KEEPALIVE, true) // 启用TCP Keep-Alive，防止连接被防火墙/NAT设备关闭
                .responseTimeout(Duration.ofMinutes(10)) // 响应超时10分钟（SSE是长连接）
                .keepAlive(true); // 启用HTTP Keep-Alive

        // 创建带超时配置的WebClient
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(webClientHttp))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 增加内存缓冲区到16MB
                .build();

        // 配置HttpComponentsClientHttpRequestFactory用于RestClient，添加超时配置
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(30000); // 连接超时30秒
        factory.setReadTimeout(300000); // 读取超时5分钟

        // 配置RestClient，添加超时配置
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
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
        // 1. 设置请求为流式模式
        request.setStream(true);

        // 2. 状态标志：标记是否正在处理工具调用块
        AtomicBoolean isInsideTool = new AtomicBoolean(false);

        // 3. 发起WebClient请求（SSE流），添加重试和错误处理
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
                            // new CommonLlmApi.ChatCompletion(null, null, null, null, null, null, null, null),
                            CommonLlmApiHelper::merge);
                    return List.of(monoChunk);
                })
                .flatMap(mono -> mono)
                // 8. 添加重试机制：遇到Connection reset等网络错误时自动重试
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2)) // 最多重试3次，初始间隔2秒
                        .maxBackoff(Duration.ofSeconds(10)) // 最大退避时间10秒
                        .filter(throwable -> {
                            // 只对特定类型的错误进行重试
                            String errorMsg = throwable.getMessage();
                            return errorMsg != null && (
                                errorMsg.contains("Connection reset") ||
                                errorMsg.contains("connection closed") ||
                                errorMsg.contains("Broken pipe") ||
                                errorMsg.contains("timeout")
                            );
                        })
                        .doBeforeRetry(retrySignal ->
                            log.warn("SSE流发生错误，准备第{}次重试: {}",
                                    retrySignal.totalRetries() + 1,
                                    retrySignal.failure().getMessage()))
                )
                // 9. 错误降级：如果重试后仍然失败，返回空流而不是抛出异常
                .onErrorResume(e -> {
                    log.error("SSE流处理后仍然失败，返回空流: {}", e.getMessage(), e);
                    return Flux.empty();
                });
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
        @JsonProperty("stream_options")
        private StreamOptions streamOptions;

        public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
            this.messages = messages;
            this.stream = stream;
        }
    }

    /**
     *  OpenAiApi.ChatCompletion
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
             * OpenAiApi.ChatCompletionFinishReason
             */
            private String finishReason;

            /**
             * 流式交互参数
             * 同于：com.aaa.springai.llm.common.CommonLlmApi.ChatCompletion.Choice#message
             */
            private ChatCompletionMessage delta;

            @JsonProperty("logprobs")
            private LogProbs logprobs;
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
     *OpenAiApi.ChatCompletionMessage
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
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PromptTokensDetails {
            @JsonProperty("cached_tokens")
            private Integer cachedTokens;
        }
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record AudioParameters(Voice voice,AudioResponseFormat format) {
        public AudioParameters(@JsonProperty("voice")Voice voice, @JsonProperty("format")AudioResponseFormat format) {
            this.voice = voice;
            this.format = format;
        }

        @JsonProperty("voice")
        public AudioParameters.Voice voice() {
            return this.voice;
        }

        @JsonProperty("format")
        public AudioParameters.AudioResponseFormat format() {
            return this.format;
        }

        public static enum Voice {
            @JsonProperty("alloy")
            ALLOY,
            @JsonProperty("ash")
            ASH,
            @JsonProperty("ballad")
            BALLAD,
            @JsonProperty("coral")
            CORAL,
            @JsonProperty("echo")
            ECHO,
            @JsonProperty("fable")
            FABLE,
            @JsonProperty("onyx")
            ONYX,
            @JsonProperty("nova")
            NOVA,
            @JsonProperty("sage")
            SAGE,
            @JsonProperty("shimmer")
            SHIMMER;
        }

        public static enum AudioResponseFormat {
            @JsonProperty("mp3")
            MP3,
            @JsonProperty("flac")
            FLAC,
            @JsonProperty("opus")
            OPUS,
            @JsonProperty("pcm16")
            PCM16,
            @JsonProperty("wav")
            WAV;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record StreamOptions(Boolean includeUsage) {
        public static CommonLlmApi.StreamOptions INCLUDE_USAGE = new CommonLlmApi.StreamOptions(true);

        public StreamOptions(@JsonProperty("include_usage") Boolean includeUsage) {
            this.includeUsage = includeUsage;
        }

        @JsonProperty("include_usage")
        public Boolean includeUsage() {
            return this.includeUsage;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record WebSearchOptions(
            OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize searchContextSize, OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation userLocation) {
        public WebSearchOptions(@JsonProperty("search_context_size") OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize searchContextSize, @JsonProperty("user_location") OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation userLocation) {
            this.searchContextSize = searchContextSize;
            this.userLocation = userLocation;
        }

        @JsonProperty("search_context_size")
        public OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize searchContextSize() {
            return this.searchContextSize;
        }

        @JsonProperty("user_location")
        public OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation userLocation() {
            return this.userLocation;
        }

        public static enum SearchContextSize {
            @JsonProperty("low")
            LOW,
            @JsonProperty("medium")
            MEDIUM,
            @JsonProperty("high")
            HIGH;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static record UserLocation(String type, OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation.Approximate approximate) {
            public UserLocation(@JsonProperty("type") String type, @JsonProperty("approximate") OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation.Approximate approximate) {
                this.type = type;
                this.approximate = approximate;
            }

            @JsonProperty("type")
            public String type() {
                return this.type;
            }

            @JsonProperty("approximate")
            public OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation.Approximate approximate() {
                return this.approximate;
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static record Approximate(String city, String country, String region, String timezone) {
                public Approximate(@JsonProperty("city") String city, @JsonProperty("country") String country, @JsonProperty("region") String region, @JsonProperty("timezone") String timezone) {
                    this.city = city;
                    this.country = country;
                    this.region = region;
                    this.timezone = timezone;
                }

                @JsonProperty("city")
                public String city() {
                    return this.city;
                }

                @JsonProperty("country")
                public String country() {
                    return this.country;
                }

                @JsonProperty("region")
                public String region() {
                    return this.region;
                }

                @JsonProperty("timezone")
                public String timezone() {
                    return this.timezone;
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
