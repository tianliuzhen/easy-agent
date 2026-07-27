package com.aaa.easyagent.llm.common;

import com.aaa.easyagent.llm.deepseek.DeepSeekTest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;

public class MemoryTest {

    public static void main(String[] args) {
        // 创建AI模型客户端
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        ChatClient chatClient = ChatClient.builder(DeepSeekTest.createChatModel())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 对话记忆的唯一标识
        String conversationIdStr = "1";

        // 发送第一个请求
        test1(chatClient,"我喜欢吃水果",conversationIdStr);
        System.out.println("===============================");
        test1(chatClient,"我喜欢吃西瓜",conversationIdStr);
        System.out.println("===============================");
        test1(chatClient,"我刚刚说喜欢吃什么",conversationIdStr);
    }

    private static void test1(ChatClient chatClient,String msg,String conversationIdStr) {
        ChatResponse response = chatClient
                .prompt()
                .system("系统提示词：生活指南大师")
                .user(msg)
                .advisors(spec -> {
                    spec.param(ChatMemory.CONVERSATION_ID, conversationIdStr);
                })
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        System.out.println("AI回应: " + content);
    }
}
