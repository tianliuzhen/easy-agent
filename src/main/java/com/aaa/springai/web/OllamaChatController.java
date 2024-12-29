package com.aaa.springai.web;

import com.aaa.springai.web.docs.LocalDocumentService;
import com.aaa.springai.web.docs.RedisDocumentService;
import com.aaa.springai.web.util.ChatResponseUtil;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 OllamaChatController.java  2024/10/27 18:36
 */
@RestController
@RequestMapping("ollama")
public class OllamaChatController {
    private final OllamaChatModel chatModel;

    @Autowired
    public OllamaChatController(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {
        return Map.of("generation", chatModel.call(message));
    }

    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return chatModel.stream(prompt);
    }

    @GetMapping("/ai/chatWithTool")
    public Object chatWithTool(@RequestParam(value = "msg", defaultValue = "查询天气") String msg) {
        // 注意这里是查询三个城市，会调三次tool
        UserMessage userMessage = new UserMessage("查询 杭州, 上海, 和北京 的天气?");
        ChatResponse response = this.chatModel.call(
                new Prompt(userMessage,
                        OpenAiChatOptions.builder()
                                .withFunction("currentWeather")
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

        //
        FunctionCallbackWrapper<QueryDateRequest, String> weatherTool
                = FunctionCallbackWrapper.<QueryDateRequest, String>builder(
                        (request, toolContext) -> {
                            if (request.type.equals("黄金")) {
                                return "600人民币";
                            }
                            return "7人民币";
                        })
                .withName("queryMetalPrice")
                .withDescription("查询黄金白金贵金属价格")
                .withInputType(QueryDateRequest.class)
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withFunctionCallbacks(List.of(weatherTool))
                .build();
        ChatResponse response = this.chatModel.call(new Prompt(userMessage, chatOptions));
        return ChatResponseUtil.getResStr(response);
    }

    @Autowired
    private LocalDocumentService localDocumentService;
    @Autowired
    private RedisDocumentService redisDocumentService;

    @GetMapping("/ai/chatWithRag")
    public String chat(@RequestParam(value = "msg", defaultValue = "你好") String msg) {

        // 向量搜索
        // List<Document> documentList = localDocumentService.search(msg);
        List<Document> documentList = redisDocumentService.search(msg);

        // 提示词模板
        PromptTemplate promptTemplate = new PromptTemplate("""
                {userMessage}
                                
                请参考以下信息回答问题:
                {documentList}
                 """);

        // 组装提示词
        Prompt prompt = promptTemplate.create(Map.of("userMessage", msg, "documentList", documentList));

        // 调用大模型
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
