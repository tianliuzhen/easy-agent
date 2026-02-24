package com.aaa.easyagent.common.util;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

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
                .orElse(null);
    }

    public static String getReasoningContent(ChatResponse chatResponse) {
        return Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getMetadata)
                .map(e -> (String) e.get("reasoningContent"))
                .orElse(null);
    }
}
