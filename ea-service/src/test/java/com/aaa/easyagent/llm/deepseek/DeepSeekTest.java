package com.aaa.easyagent.llm.deepseek;
/**
 * @author liuzhen.tian
 * @version 1.0 DeepSeekTest.java  2026/4/4 16:50
 */

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import reactor.core.publisher.Flux;

import java.util.Scanner;

/**
 * DeepSeek API 集成测试类
 * 包含：同步对话、流式对话、工具对话
 */
public class DeepSeekTest {

    // 配置信息（请替换成你的真实 API Key）
    private static final String API_KEY = "sk-f26c256e24e6423ebceafab78ffe6878";
    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private static final String MODEL = "deepseek-chat";

    /**
     * 手动创建 DeepSeekChatModel
     */
    private static DeepSeekChatModel createChatModel() {
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

    public static void main(String[] args) {
        testSyncChat();
    }

    /**
     * 测试1：同步对话
     */
    public static void testSyncChat() {
        System.out.println("========== 同步对话测试 ==========");

        DeepSeekChatModel chatModel = createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String userMessage = "你好，请介绍一下你自己";
        System.out.println("用户: " + userMessage);

        String response = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        System.out.println("DeepSeek: " + response);
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
     * 测试4：流式对话
     */
    @Test
    public void testStreamChat() {
        System.out.println("========== 流式对话测试 ==========");
        System.out.println("用户: 请写一首关于春天的诗");
        System.out.println("DeepSeek: ");

        DeepSeekChatModel chatModel = createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        Flux<String> flux = chatClient.prompt()
                .user("请写一首关于春天的诗")
                .stream()
                .content();

        // 流式输出
        flux.subscribe(
                chunk -> System.out.print(chunk),
                error -> System.err.println("错误: " + error.getMessage()),
                () -> System.out.println("\n\n[流式输出完成]")
        );

        // 等待流式输出完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
     * 测试6：手动控制参数的对话
     */
    @Test
    public void testChatWithCustomParams() {
        System.out.println("========== 自定义参数对话 ==========");

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .build();

        // 使用不同的参数配置
        DeepSeekChatOptions creativeOptions = (DeepSeekChatOptions)DeepSeekChatOptions.builder()
                .model(MODEL)
                .temperature(0.9)  // 高温度，更有创意
                .maxTokens(1000)
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder().defaultOptions(creativeOptions).build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String userMessage = "给我讲一个有趣的笑话";
        System.out.println("用户: " + userMessage);
        System.out.println("参数: temperature=0.9 (创意模式)");

        String response = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        System.out.println("DeepSeek: " + response);
    }

    /**
     * 测试7：获取完整的 ChatResponse 对象
     */
    @Test
    public void testGetFullResponse() {
        System.out.println("========== 获取完整响应对象 ==========");

        DeepSeekChatModel chatModel = createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String userMessage = "说一句问候语";

        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .chatResponse();

        System.out.println("用户: " + userMessage);
        System.out.println("响应内容: " + response.getResult().getOutput().getText());
        System.out.println("模型: " + response.getMetadata().getModel());
        System.out.println("Token使用: " + response.getMetadata().getUsage());
    }

    /**
     * 测试8：交互式对话（控制台输入）
     * 注意：这个测试需要手动输入，不会自动执行
     */
    public void testInteractiveChat() {
        System.out.println("========== 交互式对话 ==========");
        System.out.println("输入 'exit' 退出对话\n");

        DeepSeekChatModel chatModel = createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .build();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("你: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("对话结束");
                break;
            }

            String response = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content();

            System.out.println("DeepSeek: " + response);
            System.out.println();
        }

        scanner.close();
    }
}
