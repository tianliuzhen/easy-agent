package com.aaa.easyagent.common.llm.common;

/**
 * 通用的大模型调用组件
 *
 * @author liuzhen.tian
 * @version 1.0 CommonLlmChatModel.java  2025/6/8 19:16
 */


import com.aaa.easyagent.common.util.JacksonUtil;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class CommonLlmChatModel implements ChatModel {
    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

    private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;
    ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    private final CommonLlmApi apiClient;
    private final CommonLLmProperties properties;

    private final ToolCallingManager toolCallingManager;

    public CommonLlmChatModel(CommonLlmApi apiClient, CommonLLmProperties properties, ToolCallingManager toolCallingManager) {
        Assert.notNull(apiClient, "CommonLlmApi must not be null");
        Assert.notNull(properties, "CommonLLmProperties must not be null");

        this.apiClient = apiClient;
        this.properties = properties;
        this.toolCallingManager = toolCallingManager;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return internalCall(prompt, null);

    }

    private ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
        // 1. 基础请求参数处理：基础参数url\ak\sk\工具
        CommonLlmApi.ChatCompletionRequest request = createRequest(prompt, false);

        // 2. 同步调用处理
        ResponseEntity<CommonLlmApi.ChatCompletion> responseEntity = apiClient.chatCompletion(request);
        CommonLlmApi.ChatCompletion chatCompletion = responseEntity.getBody();
        if (chatCompletion == null) {
            log.warn("No chat completion returned for prompt: {}", prompt);
            return new ChatResponse(List.of());
        }
        List<CommonLlmApi.ChatCompletion.Choice> choices = chatCompletion.getChoices();
        if (choices == null) {
            log.warn("No choices returned for prompt: {}", prompt);
            return new ChatResponse(List.of());
        }

        // 3. 解析调用返回结果
        List<Generation> generations = choices.stream().map(choice -> {
            return buildGeneration(choice);
        }).filter(e -> e != null).toList();

        // 使用量计算
        CommonLlmApi.Usage usage = chatCompletion.usage;
        Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
        Usage accumulatedUsage = UsageUtils.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);
        ChatResponse response = new ChatResponse(generations, from(chatCompletion, accumulatedUsage));

        // 4. functionCall递归调用
        if (prompt.getOptions() != null && ToolCallingChatOptions.isInternalToolExecutionEnabled(prompt.getOptions()) && response.hasToolCalls()) {
            var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
            if (toolExecutionResult.returnDirect()) {
                // Return tool execution result directly to the client.
                return ChatResponse.builder().from(response).generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build();
            } else {
                // Send the tool execution result back to the model.
                return this.internalCall(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()), response);
            }
        }
        return response;
    }

    private static Generation buildGeneration(CommonLlmApi.ChatCompletion.Choice choice) {
        if (choice.getDelta() != null && choice.getMessage() == null) {
            choice.setMessage(choice.getDelta());
        }


        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        if (choice.getMessage() != null) {
            // 解析toolCall
            toolCalls = choice.getMessage().getToolCalls() == null
                    ? List.of()
                    : choice.getMessage().getToolCalls().stream().map(toolCall -> new AssistantMessage.ToolCall(toolCall.getId(), "function", toolCall.getFunction().getName(), toolCall.getFunction().getArguments())).toList();

        }


        // openAi的call和Stream【delta】，返回数据格式不一样：解析返回值和推理内容
        String content = Optional.ofNullable(choice.getMessage()).map(CommonLlmApi.ChatCompletionMessage::getContent).orElse("");
        String reasoning_content = Optional.ofNullable(choice.getMessage()).map(CommonLlmApi.ChatCompletionMessage::getReasoningContent).orElse("");
        // if (StringUtils.isBlank(content)) {
        //     content = Optional.ofNullable(choice.getDelta()).map(CommonLlmApi.Delta::getContent).orElse("");
        //     reasoning_content = Optional.ofNullable(choice.getDelta()).map(CommonLlmApi.Delta::getReasoningContent).orElse("");
        // }


        // Map<String, Object> metadata = Map.of(
        //         "id", chatCompletion2.getId(),
        //         "role", roleMap.getOrDefault(id, ""),
        //         "index", choice.getIndex(),
        //         "finishReason", choice.getFinishReason() != null ? choice.getFinishReason() : "",
        //         "refusal", StringUtils.isNotBlank(choice.getMessage().getRefusal()) ? choice.getMessage().getRefusal() : "",
        //         "reasoningContent", org.springframework.util.StringUtils.hasText(choice.getMessage().getReasoningContent()) ? choice.getMessage().getReasoningContent() : ""
        // );

        var assistantMessage = new AssistantMessage(content, Map.of(), toolCalls);
        ChatGenerationMetadata.Builder builder = ChatGenerationMetadata.builder().finishReason(choice.getFinishReason());
        // 推理内容
        assistantMessage.getMetadata().put("reasoningContent", reasoning_content);
        return new Generation(assistantMessage, builder.build());
    }


    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {

        return internalStream(prompt, null);
    }


    public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
        return Flux.deferContextual(contextView -> {
            CommonLlmApi.ChatCompletionRequest request = createRequest(prompt, true);


            Flux<CommonLlmApi.ChatCompletion> completionChunks = this.apiClient.chatCompletionStream(request);


            final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                    .prompt(prompt)
                    .provider(OpenAiApiConstants.PROVIDER_NAME)
                    .requestOptions(Objects.requireNonNullElse(prompt.getOptions(), CommonLlmChatOptions.builder().build()))
                    .build();

            Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);
            observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

            // Convert the ChatCompletionChunk into a ChatCompletion to be able to reuse
            // the function call handling logic.
            Flux<ChatResponse> chatResponse = completionChunks.switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
                try {
                    String id = chatCompletion2.getId();
                    List<Generation> generations = chatCompletion2.getChoices().stream()
                            .map(choice -> {
                                return buildGeneration(choice);
                            }).toList();
                    CommonLlmApi.Usage usage = chatCompletion2.getUsage();
                    Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
                    Usage accumulatedUsage = UsageUtils.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);
                    return new ChatResponse(generations, from(chatCompletion2, accumulatedUsage));
                } catch (Exception e) {
                    log.error("Error processing chat completion", e);
                    return new ChatResponse(List.of());
                }
            }));

            // @formatter:off
            Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
                        if (prompt.getOptions()!=null &&
                                ToolCallingChatOptions.isInternalToolExecutionEnabled(prompt.getOptions()) && response.hasToolCalls()) {
                            ToolExecutionResult toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
                            if (toolExecutionResult.returnDirect()) {
                                // Return tool execution result directly to the client.
                                return Flux.just(ChatResponse.builder().from(response)
                                        .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                                        .build());
                            } else {
                                // Send the tool execution result back to the model.
                                return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
                                        response);
                            }
                        } else {
                            return Flux.just(response);
                        }
                    })
                    .doOnError(observation::error)
                    .doFinally(s -> observation.stop())
                    .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
            // @formatter:on
            return new MessageAggregator().aggregate(flux, observationContext::setResponse);
        });
    }


    /**
     * Accessible for testing.
     */
    private CommonLlmApi.ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {

        List<CommonLlmApi.ChatCompletionMessage> messages = prompt.getInstructions().stream().map(message -> {
            if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
                return List.of(new CommonLlmApi.ChatCompletionMessage(message.getText(), message.getMessageType().getValue()));
            } else if (message.getMessageType() == MessageType.ASSISTANT) {
                var assistantMessage = (AssistantMessage) message;
                List<CommonLlmApi.ToolCall> toolCalls = null;
                if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                    toolCalls = assistantMessage.getToolCalls().stream().map(toolCall -> {
                        var function = new CommonLlmApi.ChatCompletionFunction(toolCall.name(), toolCall.arguments());
                        return new CommonLlmApi.ToolCall(toolCall.id(), toolCall.type(), function);
                    }).toList();
                }
                return List.of(new CommonLlmApi.ChatCompletionMessage(assistantMessage.getText(), message.getMessageType().getValue(), toolCalls));
            } else if (message.getMessageType() == MessageType.TOOL) {
                ToolResponseMessage toolMessage = (ToolResponseMessage) message;

                toolMessage.getResponses().forEach(response -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
                List<CommonLlmApi.ChatCompletionMessage> list = toolMessage.getResponses().stream().map(tr -> {
                    return new CommonLlmApi.ChatCompletionMessage(tr.responseData(), MessageType.TOOL.getValue(), null, tr.name(), tr.id(), null, null);
                }).toList();
                return list;
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
            }
        }).flatMap(Collection::stream).toList();

        CommonLlmApi.ChatCompletionRequest request = new CommonLlmApi.ChatCompletionRequest(messages, stream);
        request.setModel(this.properties.getChat().getOptions().getModel());
        request.setTemperature(this.properties.getChat().getOptions().getTemperature());
        request.setMaxTokens(this.properties.getChat().getOptions().getMaxTokens());
        request.setTopP(this.properties.getChat().getOptions().getTopP());
        request.setFrequencyPenalty(this.properties.getChat().getOptions().getFrequencyPenalty());
        request.setPresencePenalty(this.properties.getChat().getOptions().getPresencePenalty());
        request.setStream(stream);


        CommonLlmChatOptions requestOptions = (CommonLlmChatOptions) prompt.getOptions();
        if (requestOptions != null) {
            // request = ModelOptionsUtils.merge(requestOptions, request, CommonLlmApi.ChatCompletionRequest.class);
            // Add the tool definitions to the request's tools parameter.
            List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
            if (!CollectionUtils.isEmpty(toolDefinitions)) {
                request.setTools(getFunctionTools(toolDefinitions));
            }
        }
        return request;
    }

    private List<CommonLlmApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
        return toolDefinitions.stream().map(toolDefinition -> {
            var function = new CommonLlmApi.FunctionTool.Function(toolDefinition.description(), toolDefinition.name(), toolDefinition.inputSchema());
            return new CommonLlmApi.FunctionTool(function);
        }).toList();
    }

    /**
     * openAi的call和Stream，返回数据格式不一样
     * call：
     * 处理方案：OpenAiChatModel#buildGeneration(com.aaa.springai.llm.deepseek.OpenAiApi.ChatCompletion.Choice, java.util.Map, com.aaa.springai.llm.deepseek.OpenAiApi.ChatCompletionRequest)
     * <p>
     * choices.message.content          (回答)
     * choices.message.reasoningContent (推理)
     * </P>
     * Stream:
     * 处理方案：OpenAiChatModel#chunkToChatCompletion(com.aaa.springai.llm.deepseek.OpenAiApi.ChatCompletionChunk)
     * <p>
     * choices.delta.content           (回答)
     * choices.delta.reasoningContent  (推理)
     * <p/>
     *
     * @param response
     * @return
     */
    private ChatResponse convertResponse(CommonLlmApi.ChatCompletion response) {
        List<Generation> generations = null;
        try {
            if (CollectionUtils.isEmpty(response.getChoices())) {
                return new ChatResponse(new ArrayList<>());
            }

            generations = response.getChoices().stream().map(choice -> {
                String content = Optional.ofNullable(choice.getMessage()).map(CommonLlmApi.ChatCompletionMessage::getContent).orElse("");
                String reasoning_content = Optional.ofNullable(choice.getMessage()).map(CommonLlmApi.ChatCompletionMessage::getReasoningContent).orElse("");
                // if (StringUtils.isBlank(content)) {
                //     content = Optional.ofNullable(choice.getDelta()).map(CommonLlmApi.Delta::getContent).orElse("");
                //     reasoning_content = Optional.ofNullable(choice.getDelta()).map(CommonLlmApi.Delta::getReasoningContent).orElse("");
                // }
                ChatGenerationMetadata.Builder builder = ChatGenerationMetadata.builder().finishReason(choice.getFinishReason());
                AssistantMessage assistantMessage = new AssistantMessage(content);
                assistantMessage.getMetadata().put("reasoningContent", reasoning_content);
                return new Generation(assistantMessage, builder.build());
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to convertResponse:{}", JacksonUtil.toStr(response));
            return new ChatResponse(generations);
        }

        return new ChatResponse(generations);
    }

    private ChatResponseMetadata from(CommonLlmApi.ChatCompletion result, Usage usage) {
        Assert.notNull(result, "OpenAI ChatCompletionResult must not be null");
        var builder = ChatResponseMetadata.builder().id(result.id != null ? result.id : "").usage(usage).model(result.model != null ? result.model : "").keyValue("created", result.created != null ? result.created : 0L).keyValue("system-fingerprint", result.systemFingerprint != null ? result.systemFingerprint : "");
        return builder.build();
    }

    private DefaultUsage getDefaultUsage(CommonLlmApi.Usage usage) {
        return new DefaultUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens(), usage);
    }

}
