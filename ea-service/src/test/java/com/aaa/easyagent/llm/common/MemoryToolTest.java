package com.aaa.easyagent.llm.common;

import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.context.FunctionCallbackAdapter;
import com.aaa.easyagent.llm.deepseek.DeepSeekTest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;

public class MemoryToolTest {

    public static void main(String[] args) {
        // 工具：项目自定义 FunctionCallback -> 适配为 Spring AI ToolCallback
        ToolCallback weather = new FunctionCallbackAdapter(weatherTool());

        // 跨轮对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        // ToolCallingAdvisor（Spring AI 内置）自己完成整个「调用工具 -> 回灌结果 -> 再问 LLM」递归循环，
        // 无需手动维护 messages / withToolCall。它只在 prompt options 是 ToolCallingChatOptions 时生效，
        // 而 DeepSeekChatOptions 本身即 ToolCallingChatOptions，注册 toolCallbacks 后自动满足。
        // 它是 ToolAdvisor，显式注册后 ChatClient 不会再自动追加第二个（只允许一个 ToolAdvisor）。
        ChatClient chatClient = ChatClient.builder(DeepSeekTest.createChatModel())
                .defaultSystem("系统提示词：生活指南大师，可以调用工具查询天气")
                .defaultTools(weather)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ToolCallingAdvisor.builder().build())
                .build();

        String conversationIdStr = "1";

        test1(chatClient, "北京今天天气怎么样", conversationIdStr);
        System.out.println("===============================");
        test1(chatClient, "我刚刚问的是哪个城市的天气", conversationIdStr);
    }

    private static void test1(ChatClient chatClient, String msg, String conversationIdStr) {
        ChatResponse response = chatClient
                .prompt()
                .user(msg)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationIdStr))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        System.out.println("AI回应: " + content);
    }

    private static FunctionCallback weatherTool() {
        return new FunctionCallback() {
            @Override
            public String getName() {
                return "queryWeather";
            }

            @Override
            public String getDescription() {
                return "根据城市名查询该城市今天的天气";
            }

            @Override
            public String getInputTypeSchema() {
                return "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名，例如：北京\"}},\"required\":[\"city\"]}";
            }

            @Override
            public String call(String actionInput) {
                // actionInput 形如 {"city":"北京"}，这里返回 mock 结果
                return "晴，25℃，微风。入参=" + actionInput;
            }
        };
    }
}
