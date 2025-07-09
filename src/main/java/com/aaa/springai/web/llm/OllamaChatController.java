package com.aaa.springai.web.llm;

import com.aaa.springai.util.ChatResponseUtil;
import com.aaa.springai.web.docs.LocalDocumentService;
import com.aaa.springai.web.docs.RedisDocumentService;
import com.aaa.springai.web.sse.SseEmitterUTF8;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author liuzhen.tian
 * @version 1.0 OllamaChatController.java  2024/10/27 18:36
 */
@RestController
@RequestMapping("ollama")
public class OllamaChatController {
    private final OllamaChatModel chatModel;
    // 文档检索
    private final LocalDocumentService localDocumentService;
    @Autowired(required = false)
    private RedisDocumentService redisDocumentService;

    // 初始化基于内存的对话记忆
    private final ChatMemory inMemoryChatMemory;

    public OllamaChatController(OllamaChatModel chatModel, LocalDocumentService localDocumentService, ChatMemory inMemoryChatMemory) {
        this.chatModel = chatModel;
        this.localDocumentService = localDocumentService;
        this.inMemoryChatMemory = inMemoryChatMemory;
    }


    @GetMapping("/ai/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {
        return Map.of("generation", chatModel.call(message));
    }

    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String msg) {
        Prompt prompt = new Prompt(new UserMessage(msg));
        return chatModel.stream(prompt);
    }

    @GetMapping("/ai/sseEmitter")
    public SseEmitter sseEmitter(String msg) {
        SseEmitterUTF8 sseEmitter = new SseEmitterUTF8(1000 * 60L);
        Prompt prompt = new Prompt(new UserMessage(msg));
        System.out.println(Thread.currentThread().getName() + "-out");
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "-inner-1");
            Flux<ChatResponse> stream = chatModel.stream(prompt);
            stream.subscribe(e -> {
                try {
                    String text = e.getResult().getOutput().getText();
                    sseEmitter.send(text);
                    System.out.println(Thread.currentThread().getName() + "-inner-2-" + text);
                } catch (Exception ex) {
                    sseEmitter.complete();
                }

            }, err -> {
                // 处理流中的错误
                // sseEmitter.completeWithError(err);
                sseEmitter.complete();
            }, () -> {
                // 流完成时调用
                sseEmitter.complete();
            });

        }).start();

        return sseEmitter;
    }


    @GetMapping("/ai/chatWithTool")
    public Object chatWithTool(@RequestParam(value = "msg", defaultValue = "查询天气") String msg) {
        // 注意这里是查询三个城市，会调三次tool
        UserMessage userMessage = new UserMessage("查询 杭州, 上海, 和北京 的天气?");
        ChatResponse response = this.chatModel.call(
                new Prompt(userMessage,
                        OpenAiChatOptions.builder()
                                .function("currentWeather")
                                .build()
                )
        ); // Enable the function
        return ChatResponseUtil.getResStr(response);
    }

    @GetMapping("/ai/chatWithTool2")
    public Object chatWithTool2(@RequestParam(value = "msg", defaultValue = "查询价格") String msg) {
        UserMessage userMessage = new UserMessage("查询白银和黄金的价格是多少");
        record QueryDateRequest(@JsonPropertyDescription("type类型只能是[黄金,白银]") String type) {
        }


        FunctionToolCallback<QueryDateRequest, String> weatherTool = FunctionToolCallback.<QueryDateRequest, String>builder("queryMetalPrice", (request, toolContext) -> {
                    if ("黄金".equals(request.type)) {
                        return "600人民币";
                    }
                    return "7人民币";
                })
                .description("查询黄金白金贵金属价格")
                .inputType(QueryDateRequest.class)
                .build();


        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .functionCallbacks(List.of(weatherTool))
                .build();
        ChatResponse response = this.chatModel.call(new Prompt(userMessage, chatOptions));
        return ChatResponseUtil.getResStr(response);
    }


    @GetMapping("/ai/chatWithRag")
    public String chat(@RequestParam(value = "msg", defaultValue = "你好") String msg) {

        // 向量搜索
        List<Document> documentList = localDocumentService.search(msg);
        // List<Document> documentList = redisDocumentService.search(msg);

        // 提示词模板
        PromptTemplate promptTemplate = new PromptTemplate("""
                {userMessage}
                                
                请参考以下信息回答问题:
                {documentList}
                 """);

        // 组装提示词
        Prompt prompt = promptTemplate.create(Map.of("userMessage", msg, "documentList", documentList));

        // 调用大模型
        String content = chatModel.call(prompt).getResult().getOutput().getText();
        return content;
    }

    @GetMapping("/ai/chatWithMemory")
    public String chatWithMemory(
            @RequestParam(value = "conversationId", defaultValue = "102") String conversationId,
            @RequestParam(value = "msg", defaultValue = "你好") String msg) {

        // 创建AI模型客户端
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(inMemoryChatMemory))
                .build();

        // 对话记忆的唯一标识
        String conversationIdStr = conversationId;

        // 发送第一个请求
        ChatResponse response = chatClient
                .prompt()
                .user(msg)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationIdStr)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        System.out.println("AI回应: " + content);

        return content;
    }


    @GetMapping("/ai/sseChatWithMemory")
    public SseEmitter sseChatWithMemory(
            @RequestParam(value = "conversationId", defaultValue = "102") String conversationId,
            @RequestParam(value = "msg", defaultValue = "你好") String msg) {
        SseEmitterUTF8 sseEmitter = new SseEmitterUTF8(1000 * 60L);

        // 对话记忆的唯一标识
        if (StringUtils.isBlank(conversationId)) {
            conversationId = UUID.randomUUID().toString();
        }
        String finalConversationId = conversationId;

        new Thread(() -> {
            // 创建AI模型客户端
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(new MessageChatMemoryAdvisor(inMemoryChatMemory))
                    .build();

            // 发送第一个请求
            Flux<ChatResponse> stream = chatClient
                    .prompt()
                    .user(msg)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, finalConversationId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .stream().chatResponse();


            stream.subscribe(e -> {
                try {
                    String text = e.getResult().getOutput().getText();
                    sseEmitter.send(text);
                } catch (Exception ex) {
                    sseEmitter.complete();
                }

            }, err -> {
                // 处理流中的错误
                // sseEmitter.completeWithError(err);
                sseEmitter.complete();
            }, () -> {
                // 流完成时调用
                sseEmitter.complete();
            });

        }).start();

        return sseEmitter;
    }
}
