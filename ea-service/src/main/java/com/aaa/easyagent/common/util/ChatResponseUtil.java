package com.aaa.easyagent.common.util;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author liuzhen.tian
 * @version 1.0 ChatResponseUtil.java  2024/12/28 20:21
 */
public class ChatResponseUtil {
    public static String getResStr(ChatResponse chatResponse) {
        return Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .orElse("");
    }

    public static String getReasoningContent(ChatResponse chatResponse) {
        return Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getMetadata)
                .map(e -> (String) e.get("reasoningContent"))
                .orElse("");
    }

    /**
     * 获取List<ChatResponse>的文本内容（拼接所有文本）
     */
    public static String getResStr(List<ChatResponse> chatResponses) {
        if (CollectionUtils.isEmpty(chatResponses)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (ChatResponse response : chatResponses) {
            String text = getResStr(response);
            result.append(text);
        }
        return !result.isEmpty() ? result.toString() : "";
    }

    /**
     * 获取List<ChatResponse>的思考内容（拼接所有思考内容）
     */
    public static String getReasoningContent(List<ChatResponse> chatResponses) {
        if (CollectionUtils.isEmpty(chatResponses)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (ChatResponse response : chatResponses) {
            String reasoning = getReasoningContent(response);
            result.append(reasoning);
        }
        return !result.isEmpty() ? result.toString() : "";
    }
}
