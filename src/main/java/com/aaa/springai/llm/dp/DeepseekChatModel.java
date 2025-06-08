package com.aaa.springai.llm.dp;

/**
 * @author liuzhen.tian
 * @version 1.0 DeepseekChatModel.java  2025/6/8 19:16
 */


import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeepseekChatModel implements ChatModel {

    private final DeepseekApiClient apiClient;
    private final DeepseekProperties properties;

    public DeepseekChatModel(DeepseekApiClient apiClient, DeepseekProperties properties) {
        Assert.notNull(apiClient, "DeepseekApiClient must not be null");
        Assert.notNull(properties, "DeepseekProperties must not be null");

        this.apiClient = apiClient;
        this.properties = properties;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        DeepseekApiClient.DeepseekChatCompletionRequest request = createRequest(prompt, false);
        DeepseekApiClient.DeepseekChatCompletion response = apiClient.chatCompletion(request)
                .block(); // 同步阻塞获取结果

        return convertResponse(response);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        DeepseekApiClient.DeepseekChatCompletionRequest request = createRequest(prompt, true);

        return apiClient.chatCompletionStream(request)
                .map(this::convertResponse);
    }

    private DeepseekApiClient.DeepseekChatCompletionRequest createRequest(Prompt prompt, boolean stream) {
        List<DeepseekApiClient.DeepseekChatCompletionRequest.Message> messages = prompt.getInstructions()
                .stream()
                .map(message -> {
                    DeepseekApiClient.DeepseekChatCompletionRequest.Message apiMessage =
                            new DeepseekApiClient.DeepseekChatCompletionRequest.Message();
                    apiMessage.setRole(message.getMessageType().getValue());
                    apiMessage.setContent(message.getText());
                    return apiMessage;
                })
                .collect(Collectors.toList());

        DeepseekApiClient.DeepseekChatCompletionRequest request =
                new DeepseekApiClient.DeepseekChatCompletionRequest();
        request.setModel(this.properties.getChat().getOptions().getModel());
        request.setMessages(messages);
        request.setTemperature(this.properties.getChat().getOptions().getTemperature());
        request.setMaxTokens(this.properties.getChat().getOptions().getMaxTokens());
        request.setTopP(this.properties.getChat().getOptions().getTopP());
        request.setFrequencyPenalty(this.properties.getChat().getOptions().getFrequencyPenalty());
        request.setPresencePenalty(this.properties.getChat().getOptions().getPresencePenalty());
        request.setStream(stream);

        return request;
    }

    private ChatResponse convertResponse(DeepseekApiClient.DeepseekChatCompletion response) {
        List<Generation> generations = response.getChoices().stream()
                .map(choice -> {
                    String content = Optional.ofNullable(choice.getMessage()).map(DeepseekApiClient.DeepseekChatCompletion.Choice.Message::getContent).orElse("");
                    String reasoning_content = Optional.ofNullable(choice.getMessage()).map(DeepseekApiClient.DeepseekChatCompletion.Choice.Message::getReasoning_content).orElse("");
                    if (StringUtils.isBlank(content)) {
                        content = Optional.ofNullable(choice.getDelta()).map(DeepseekApiClient.DeepseekChatCompletion.Choice.Delta::getContent).orElse("");
                        reasoning_content = Optional.ofNullable(choice.getDelta()).map(DeepseekApiClient.DeepseekChatCompletion.Choice.Delta::getReasoning_content).orElse("");
                    }
                    ChatGenerationMetadata.Builder builder = ChatGenerationMetadata.builder().finishReason(choice.getFinishReason());
                    builder.metadata("reasoning_content", reasoning_content);
                    return new Generation(
                            new AssistantMessage(content),
                            builder.build());
                })
                .collect(Collectors.toList());

        return new ChatResponse(generations);
    }
}
