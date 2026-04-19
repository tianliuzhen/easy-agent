package com.aaa.easyagent.llm.common;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmTest.java  2026/4/19
 */

import com.aaa.easyagent.common.llm.common.CommonLLmProperties;
import com.aaa.easyagent.common.llm.common.CommonLlmApi;
import com.aaa.easyagent.common.llm.common.CommonLlmChatModel;
import com.aaa.easyagent.common.llm.common.CommonLlmChatOptions;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CommonLlmChatModel 集成测试类
 * 包含：流式对话、工具调用测试
 */
public class CommonLlmTest {

    // 配置信息（请替换成你的真实 API Key）
    private static final String API_KEY = "sk-0cc836096581452b86b34e2f604d3a90";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/";
    private static final String MODEL = "qwen3.5-35b-a3b";

    /**
     * 手动创建 CommonLlmChatModel
     */
    private static CommonLlmChatModel createChatModel() {
        // 1. 创建配置属性
        CommonLLmProperties properties = new CommonLLmProperties();
        properties.setApiKey(API_KEY);
        properties.setBaseUrl(BASE_URL);

        CommonLLmProperties.ChatProperties chatProperties = new CommonLLmProperties.ChatProperties();
        chatProperties.setCompletionsPath("/v1/chat/completions");

        // 配置 ChatOptions
        CommonLlmChatOptions chatOptions = CommonLlmChatOptions.builder()
                .model(MODEL)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
        chatProperties.setOptions(chatOptions);
        properties.setChat(chatProperties);

        // 2. 创建 API 实例
        CommonLlmApi commonLlmApi = new CommonLlmApi(properties);

        // 3. 创建 ToolCallingManager（使用默认实现）
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        // 4. 创建 ChatModel
        return new CommonLlmChatModel(commonLlmApi, properties, toolCallingManager);
    }

    /**
     * 测试：获取完整的 ChatResponse 对象（带工具调用）
     */
    @Test
    public void testGetFullResponseWithTool() {
        System.out.println("========== 获取完整响应对象（带工具） ==========");

        CommonLlmChatModel chatModel = createChatModel();

        // 配置工具
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new WeatherTools());

        // 创建带工具的 Prompt
        Prompt prompt = new Prompt(
                List.of(new UserMessage("查询北京的天气")),
                com.aaa.easyagent.common.llm.common.CommonLlmChatOptions.builder()
                        .temperature(0.7)
                        .toolCallbacks(toolCallbacks)  // 直接传对象，自动识别 @Tool 方法
                        .build()
        );

        // 流式调用
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // 收集结果
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        stream.subscribe(
                response -> {
                    // 获取内容
                    String content = ChatResponseUtil.getResStr(response);
                    if (content != null && !content.isEmpty()) {
                        fullResponse.append(content);
                    }
                },
                error -> {
                    System.err.println("错误: " + error.getMessage());
                    latch.countDown();
                },
                () -> {
                    System.out.println("\n========== 流式输出完成 ==========");
                    System.out.println("完整响应: " + fullResponse.toString());
                    latch.countDown();
                }
        );

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.print(fullResponse);
    }

    /**
     * 测试：不带工具的流式对话
     */
    @Test
    public void testStreamWithoutTool() {
        System.out.println("========== 流式对话（无工具） ==========");

        CommonLlmChatModel chatModel = createChatModel();

        // 创建不带工具的 Prompt
        Prompt prompt = new Prompt(
                List.of(new UserMessage("你好，请介绍一下你自己")),
                com.aaa.easyagent.common.llm.common.CommonLlmChatOptions.builder()
                        .temperature(0.7)
                        .build()
        );

        // 流式调用
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        // 收集结果
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        stream.subscribe(
                response -> {
                    String content = ChatResponseUtil.getResStr(response);
                    if (content != null && !content.isEmpty()) {
                        fullResponse.append(content);
                        System.out.print(content); // 实时打印
                    }
                },
                error -> {
                    System.err.println("\n错误: " + error.getMessage());
                    latch.countDown();
                },
                () -> {
                    System.out.println("\n========== 流式输出完成 ==========");
                    latch.countDown();
                }
        );

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 天气查询工具类
     */
    public static class WeatherTools {

        @Tool(description = "查询指定城市的天气信息，返回温度、天气状况等")
        public String queryWeather(
                @ToolParam(description = "城市名称，如：北京、上海、深圳") String city,
                @ToolParam(description = "查询日期，格式yyyy-MM-dd，默认为今天", required = false) String date) {

            // 模拟天气查询逻辑
            if ("北京".equals(city)) {
                return "北京今天晴天，温度25°C，空气质量良好";
            } else if ("上海".equals(city)) {
                return "上海今天多云，温度22°C，湿度较高";
            } else if ("深圳".equals(city)) {
                return "深圳今天阵雨，温度28°C，注意带伞";
            }
            return city + "天气：晴，温度20-28°C";
        }
    }

}
