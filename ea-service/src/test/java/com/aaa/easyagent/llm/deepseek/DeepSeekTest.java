package com.aaa.easyagent.llm.deepseek;
/**
 * @author liuzhen.tian
 * @version 1.0 DeepSeekTest.java  2026/4/4 16:50
 */

import com.aaa.easyagent.common.util.ChatResponseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API 集成测试类
 * 包含：同步对话、流式对话、工具对话
 */
public class DeepSeekTest {

    // 配置信息（请替换成你的真实 API Key）
    private static final String API_KEY = "sk-0cc836096581452b86b34e2f604d3a90";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/";
    private static final String MODEL = "qwen3.5-35b-a3b";

    /**
     * 手动创建 DeepSeekChatModel
     */
    public static DeepSeekChatModel createChatModel() {
        // 1. 创建 API 实例
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .build();

        // 2. 创建配置选项
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(MODEL)
                .temperature(0.7)
                .maxTokens(2000)
                .build();

        // 3. 创建 ChatModel
        return  DeepSeekChatModel.builder().deepSeekApi(deepSeekApi).defaultOptions(options).build();
    }



    /**
     * 测试2：带系统提示词的同步对话
     */
    @Test
    public void testSyncChatWithSystemPrompt() {
        System.out.println("========== 带系统提示词的同步对话 ==========");

        DeepSeekChatModel chatModel = createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String systemPrompt = "你是一个专业的Java编程助手，回答要简洁、专业，并尽量提供代码示例";
        String userMessage = "什么是Spring Boot？";

        System.out.println("系统提示: " + systemPrompt);
        System.out.println("用户: " + userMessage);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        System.out.println("DeepSeek: " + response);
    }

    /**
     * 测试3：带对话记忆的同步对话（多轮对话）
     */
    @Test
    public void testSyncChatWithMemory() {
        System.out.println("========== 带记忆的多轮对话 ==========");

        DeepSeekChatModel chatModel = createChatModel();

        // 创建带内存的 ChatClient


        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .build();

        // 第一轮对话
        String response1 = chatClient.prompt()
                .user("我的名字叫张三")
                .call()
                .content();
        System.out.println("用户: 我的名字叫张三");
        System.out.println("DeepSeek: " + response1);
        System.out.println();

        // 第二轮对话（会记住上下文）
        String response2 = chatClient.prompt()
                .user("我叫什么名字？")
                .call()
                .content();
        System.out.println("用户: 我叫什么名字？");
        System.out.println("DeepSeek: " + response2);
    }

    /**
     * 测试5：工具对话（Function Calling）
     * 注意：需要先定义工具类
     */
    @Test
    public void testToolChat() {
        System.out.println("========== 工具对话测试 ==========");

        DeepSeekChatModel chatModel = createChatModel();

        // 注册工具
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(new DeepSeekTools())
                .build()
                .getToolCallbacks();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(toolCallbacks)
                .build();

        // 测试天气查询
        String question1 = "北京今天天气怎么样？";
        System.out.println("用户: " + question1);

        String response1 = chatClient.prompt()
                .user(question1)
                .call()
                .content();
        System.out.println("DeepSeek: " + response1);
        System.out.println();

        // 测试时间查询
        String question2 = "现在几点了？";
        System.out.println("用户: " + question2);

        String response2 = chatClient.prompt()
                .user(question2)
                .call()
                .content();
        System.out.println("DeepSeek: " + response2);
        System.out.println();

        // 测试计算
        String question3 = "计算 123 + 456 等于多少？";
        System.out.println("用户: " + question3);

        String response3 = chatClient.prompt()
                .user(question3)
                .call()
                .content();
        System.out.println("DeepSeek: " + response3);
    }


    /**
     * 测试7：获取完整的 ChatResponse 对象
     */
    @Test
    public void testGetFullResponse() {
        System.out.println("========== 获取完整响应对象 ==========");

        DeepSeekChatModel chatModel = createChatModel();

        // 配置工具
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new WeatherTools());


        // 创建带工具的 Prompt
        Prompt prompt = new Prompt(
                List.of(new UserMessage("查询北京的天气")),
                DeepSeekChatOptions.builder()
                        // .model(DeepSeekApi.DEFAULT_CHAT_MODEL)
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
                    // 检查是否有工具调用

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
            }
            return city + "天气：晴，温度20-28°C";
        }
    }

}
