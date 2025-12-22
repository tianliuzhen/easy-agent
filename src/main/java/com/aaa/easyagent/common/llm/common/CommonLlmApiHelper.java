package com.aaa.easyagent.common.llm.common;

import com.aaa.easyagent.common.llm.deepseek.OpenAiApi;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmApiHelper.java  2025/7/10 21:35
 */
public class CommonLlmApiHelper {

    /**
     * @param chatCompletion the ChatCompletionChunk to check
     * @return true if the ChatCompletionChunk is a streaming tool function call and it is
     * the last one.
     */
    public static boolean isStreamingToolFunctionCallFinish(CommonLlmApi.ChatCompletion chatCompletion) {

        if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.getChoices())) {
            return false;
        }

        var choice = chatCompletion.getChoices().get(0);
        if (choice == null || choice.getDelta() == null) {
            return false;
        }
        return OpenAiApi.ChatCompletionFinishReason.TOOL_CALLS.name().equalsIgnoreCase(choice.getFinishReason());
    }

    public static boolean isStreamingToolFunctionCall(CommonLlmApi.ChatCompletion chatCompletion) {

        if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.getChoices())) {
            return false;
        }

        var choice = chatCompletion.getChoices().get(0);
        if (choice == null || choice.getDelta() == null) {
            return false;
        }
        return !CollectionUtils.isEmpty(choice.getDelta().getToolCalls());
    }


    /**
     * Merge the previous and current ChatCompletionChunk into a single one.
     *
     * @param previous the previous ChatCompletionChunk
     * @param current  the current ChatCompletionChunk
     * @return the merged ChatCompletionChunk
     */
    public static CommonLlmApi.ChatCompletion merge(CommonLlmApi.ChatCompletion previous, CommonLlmApi.ChatCompletion current) {

        if (previous == null) {
            return current;
        }

        String id = (current.getId() != null ? current.getId() : previous.getId());
        Long created = (current.getCreated() != null ? current.getCreated() : previous.getCreated());
        String model = (current.getModel() != null ? current.getModel() : previous.getModel());
        String serviceTier = (current.getServiceTier() != null ? current.getServiceTier() : previous.getServiceTier());
        String systemFingerprint = (current.getSystemFingerprint() != null ? current.getSystemFingerprint()
                : previous.getSystemFingerprint());
        String object = (current.getObject() != null ? current.getObject() : previous.getObject());
        CommonLlmApi.Usage usage = (current.getUsage() != null ? current.getUsage() : previous.getUsage());

        CommonLlmApi.ChatCompletion.Choice previousChoice0 = (CollectionUtils.isEmpty(previous.getChoices()) ? null : previous.getChoices().get(0));
        CommonLlmApi.ChatCompletion.Choice currentChoice0 = (CollectionUtils.isEmpty(current.getChoices()) ? null : current.getChoices().get(0));

        CommonLlmApi.ChatCompletion.Choice choice = merge(previousChoice0, currentChoice0);
        List<CommonLlmApi.ChatCompletion.Choice> chunkChoices = choice == null ? List.of() : List.of(choice);
        return new CommonLlmApi.ChatCompletion(id, object, created, model, chunkChoices, usage, systemFingerprint, serviceTier);
    }

    private static CommonLlmApi.ChatCompletion.Choice merge(CommonLlmApi.ChatCompletion.Choice previous, CommonLlmApi.ChatCompletion.Choice current) {
        if (previous == null) {
            return current;
        }

        String finishReason = (current.getFinishReason() != null ? current.getFinishReason()
                : previous.getFinishReason());
        Integer index = (current.getIndex() != null ? current.getIndex() : previous.getIndex());

        CommonLlmApi.ChatCompletionMessage message = merge(previous.getDelta(), current.getDelta());

        OpenAiApi.LogProbs logprobs = (current.getLogprobs() != null ? current.getLogprobs() : previous.getLogprobs());
        return new CommonLlmApi.ChatCompletion.Choice(index, null, finishReason, message, logprobs);
    }

    private static CommonLlmApi.ChatCompletionMessage merge(CommonLlmApi.ChatCompletionMessage previous, CommonLlmApi.ChatCompletionMessage current) {
        String content = (current.getContent() != null ? current.getContent()
                : "" + ((previous.getContent() != null) ? previous.getContent() : ""));
        String role = (current.getRole() != null ? current.getRole() : previous.getRole());
        role = (role != null ? role : "assistant"); // default to ASSISTANT (if null
        String name = (current.getName() != null ? current.getName() : previous.getName());
        String toolCallId = (current.getToolCallId() != null ? current.getToolCallId() : previous.getToolCallId());
        String refusal = (current.getRefusal() != null ? current.getRefusal() : previous.getRefusal());
        String reasoning_content = current.getReasoningContent();
        CommonLlmApi.AudioOutput audioOutput = (current.getAudioOutput() != null ? current.getAudioOutput()
                : previous.getAudioOutput());

        List<CommonLlmApi.ToolCall> toolCalls = new ArrayList<>();
        CommonLlmApi.ToolCall lastPreviousTooCall = null;
        if (previous.getToolCalls() != null) {
            lastPreviousTooCall = previous.getToolCalls().get(previous.getToolCalls().size() - 1);
            if (previous.getToolCalls().size() > 1) {
                toolCalls.addAll(previous.getToolCalls().subList(0, previous.getToolCalls().size() - 1));
            }
        }
        if (current.getToolCalls() != null) {
            if (current.getToolCalls().size() > 1) {
                throw new IllegalStateException("Currently only one tool call is supported per message!");
            }
            var currentToolCall = current.getToolCalls().iterator().next();
            if (currentToolCall.getId() != null) {
                if (lastPreviousTooCall != null) {
                    toolCalls.add(lastPreviousTooCall);
                }
                toolCalls.add(currentToolCall);
            } else {
                toolCalls.add(merge(lastPreviousTooCall, currentToolCall));
            }
        } else {
            if (lastPreviousTooCall != null) {
                toolCalls.add(lastPreviousTooCall);
            }
        }
        return new CommonLlmApi.ChatCompletionMessage(role, content, reasoning_content, name, toolCallId, toolCalls, refusal, audioOutput);
    }

    private static CommonLlmApi.ToolCall merge(CommonLlmApi.ToolCall previous, CommonLlmApi.ToolCall current) {
        if (previous == null) {
            return current;
        }
        String id = (current.getId() != null ? current.getId() : previous.getId());
        String type = (current.getType() != null ? current.getType() : previous.getType());
        CommonLlmApi.ChatCompletionFunction function = merge(previous.getFunction(), current.getFunction());
        return new CommonLlmApi.ToolCall(id, type, function);
    }

    private static CommonLlmApi.ChatCompletionFunction merge(CommonLlmApi.ChatCompletionFunction previous, CommonLlmApi.ChatCompletionFunction current) {
        if (previous == null) {
            return current;
        }
        String name = (current.getName() != null ? current.getName() : previous.getName());
        StringBuilder arguments = new StringBuilder();
        if (previous.getArguments() != null) {
            arguments.append(previous.getArguments());
        }
        if (current.getArguments() != null) {
            arguments.append(current.getArguments());
        }
        return new CommonLlmApi.ChatCompletionFunction(name, arguments.toString());
    }
}


