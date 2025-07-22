package com.aaa.springai.web.example.llm;

import com.aaa.springai.llm.common.CommonLlmChatModel;
import com.aaa.springai.llm.deepseek.OpenAiChatOptions;
import com.aaa.springai.util.ChatResponseUtil;
import com.aaa.springai.web.example.sse.SseEmitterUTF8;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * @author liuzhen.tian
 * @version 1.0 CommonLlmChatController.java  2025/7/9 21:34
 */
@RestController
@RequestMapping("commonLlm")
public class CommonLlmChatController {
    @Resource
    private CommonLlmChatModel dpChatModel;


    /**
     * 聊天的方法。底层调用的openAi的方法
     * RequestParam 接受参数
     * msg 就是我们提的问题
     *
     * @return
     */
    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        // String called = dpChatModel.call(msg);
        ChatResponse call = dpChatModel.call(new Prompt(new UserMessage(msg)));
        // 大模型思考内容
        Optional.ofNullable(call.getResult()).map(Generation::getOutput).map(AbstractMessage::getMetadata)
                .ifPresent(metadata -> {
                    String reasoningContent = (String) metadata.get("reasoningContent");
                    if (StringUtils.isNotBlank(reasoningContent)) {
                        System.out.println("reasoningContent = " + reasoningContent);
                    }
                });

        String resStr = ChatResponseUtil.getResStr(call);
        if (StringUtils.isNotBlank(resStr)) {
            System.out.println("resStr = " + resStr);
        }
        return resStr;
    }


    @GetMapping("/ai/sseEmitter")
    public SseEmitter sseEmitter(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        SseEmitterUTF8 sseEmitter = new SseEmitterUTF8(1000 * 60L);
        Prompt prompt = new Prompt(new UserMessage(msg));
        new Thread(() -> {
            Flux<ChatResponse> stream = dpChatModel.stream(prompt);
            stream.subscribe(e -> {
                try {
                    // 大模型思考内容
                    Optional.ofNullable(e.getResult()).map(Generation::getOutput).map(AbstractMessage::getMetadata).ifPresent(metadata -> {
                        String reasoningContent = (String) metadata.get("reasoningContent");
                        if (StringUtils.isNotBlank(reasoningContent)) {
                            System.out.println("reasoningContent = " + reasoningContent);
                        }
                    });

                    String resStr = ChatResponseUtil.getResStr(e);
                    if (StringUtils.isNotBlank(resStr)) {
                        System.out.println("resStr = " + resStr);
                    }
                    sseEmitter.send(e);
                } catch (Exception ex) {
                    sseEmitter.complete();
                }
            }, err -> {
                // 处理流中的错误
                // sseEmitter.completeWithError(err);
                err.printStackTrace();
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
        UserMessage userMessage = new UserMessage("查询 杭州 的天气?");
        ChatResponse response = this.dpChatModel.call(
                new Prompt(userMessage,
                        OpenAiChatOptions.builder()
                                .function("currentWeather")
                                .internalToolExecutionEnabled(false)
                                .build()
                )
        ); // Enable the function
        String resStr = ChatResponseUtil.getResStr(response);
        System.out.println(resStr);
        return resStr;
    }

    @GetMapping("/ai/chatWithToolSse")
    public SseEmitter chatWithToolSse(@RequestParam(value = "msg", defaultValue = "查询价格") String msg) {
        SseEmitterUTF8 sseEmitter = new SseEmitterUTF8(1000 * 60L);
        UserMessage userMessage = new UserMessage("查询白银价格");
        record QueryDateRequest(@JsonPropertyDescription("type类型只能是[黄金,白银]") String type) {
        }

        FunctionToolCallback<QueryDateRequest, String> weatherTool = FunctionToolCallback.<QueryDateRequest, String>builder("queryMetalPrice", (request, toolContext) -> {
                    if ("黄金".equals(request.type)) {
                        return "600人民币";
                    }
                    return "7人民币";
                })
                // .inputSchema(FunctionToolCallback.SchemaType.JSON_SCHEMA)
                .description("查询黄金白金贵金属价格")
                .inputType(QueryDateRequest.class)
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .toolCallbacks(List.of(weatherTool))
                .build();
        Flux<ChatResponse> stream = this.dpChatModel.stream(new Prompt(userMessage, chatOptions));
        stream.subscribe(e -> {
            try {
                // 大模型思考内容
                Optional.ofNullable(e.getResult()).map(Generation::getOutput).map(AbstractMessage::getMetadata).ifPresent(metadata -> {
                    String reasoningContent = (String) metadata.get("reasoningContent");
                    if (StringUtils.isNotBlank(reasoningContent)) {
                        System.err.print(reasoningContent);
                    }
                });

                String resStr = ChatResponseUtil.getResStr(e);
                if (StringUtils.isNotBlank(resStr)) {
                    System.out.println("resStr = " + resStr);
                }
                sseEmitter.send(e);
            } catch (Exception ex) {
                sseEmitter.complete();
            }
        }, err -> {
            // 处理流中的错误
            // sseEmitter.completeWithError(err);
            err.printStackTrace();
            sseEmitter.complete();
        }, () -> {
            // 流完成时调用
            sseEmitter.complete();
        });
        return sseEmitter;
    }

}
