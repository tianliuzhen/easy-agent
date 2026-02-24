package com.aaa.easyagent.web.example.llm;

import com.aaa.easyagent.common.llm.deepseek.OpenAiChatModel;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.web.example.sse.SseEmitterUTF8;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
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
 * 13k star! 获取免费ChatGPT API Key的开源项目，亲测可用！:  https://mp.weixin.qq.com/s?__biz=MzU0MzI3MzU1Ng==&mid=2247484719&idx=1&sn=33b4f623d421216fae26ddcae446e4da&chksm=fb0ca214cc7b2b0215d12a29ff091c5840798b8b15657212d248f73ae6b7c63b2fc28e72bf4a&scene=21#wechat_redirect
 * Free ChatGPT API Key: https://mp.weixin.qq.com/s?__biz=MzU0MzI3MzU1Ng==&mid=2247484719&idx=1&sn=33b4f623d421216fae26ddcae446e4da&chksm=fb0ca214cc7b2b0215d12a29ff091c5840798b8b15657212d248f73ae6b7c63b2fc28e72bf4a&scene=21#wechat_redirect
 *
 * @author liuzhen.tian
 * @version 1.0 OpenApiChatController.java  2024/10/27 18:52
 */
@RestController
@RequestMapping("deepseek")
public class DeepSeekChatController {
    @Resource
    private OpenAiChatModel deepSeekChatModel;


    @Resource
    private OpenAiImageModel imageModel;

    /**
     * 聊天的方法。底层调用的openAi的方法
     * RequestParam 接受参数
     * msg 就是我们提的问题
     *
     * @return
     */
    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        // String called = deepSeekChatModel.call(msg);
        ChatResponse call = deepSeekChatModel.call(new Prompt(new UserMessage(msg)));
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
            Flux<ChatResponse> stream = deepSeekChatModel.stream(prompt);
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


    /**
     * CommonLlmChatOptions.builder() 传入的一个参数，可以控制大模型的设置
     *
     * @param msg
     * @return
     */
    @GetMapping("/ai/chat3")
    public Object chat3(@RequestParam("msg") String msg) {
        ChatResponse chatResponse = deepSeekChatModel.call(new Prompt(msg, OpenAiChatOptions.builder()
                //.withModel("gpt-4-32k")  //gpt的版本 ，32K是参数，参数越高，回答问题越准确
                .temperature(0.4)  // 温度值，温度越高，回答的准确率越低，温度越低，回答的准确率越高
                .build()));
        return chatResponse.getResult().getOutput().getText();
    }

    @GetMapping("/ai/chatWithTool")
    public Object chatWithTool(@RequestParam(value = "msg", defaultValue = "查询天气") String msg) {
        // 注意这里是查询三个城市，会调三次tool
        UserMessage userMessage = new UserMessage("分别查询 杭州, 上海, 和北京 的天气?");
        Prompt currentWeather = new Prompt(userMessage,
                OpenAiChatOptions.builder()
                        .function("currentWeather")
                        .build()
        );
        ChatResponse response = this.deepSeekChatModel.call(
                currentWeather
        ); // Enable the function
        return ChatResponseUtil.getResStr(response);
    }

    @GetMapping("/ai/chatWithTool2")
    public Object chatWithTool2(@RequestParam(value = "msg", defaultValue = "查询价格") String msg) {
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
        ChatResponse response = this.deepSeekChatModel.call(new Prompt(userMessage, chatOptions));
        System.out.println("ChatResponseUtil.getResStr(response) = " + ChatResponseUtil.getResStr(response));
        return ChatResponseUtil.getResStr(response);
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
        Flux<ChatResponse> stream = this.deepSeekChatModel.stream(new Prompt(userMessage, chatOptions));
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


    /**
     * 聊天的方法。底层调用的openAi的方法
     * RequestParam 接受参数
     * msg 就是我们提的问题
     *
     * @return
     */
    @GetMapping("/ai/image/chat")
    public Object chat() {
        ImageResponse response = imageModel.call(
                new ImagePrompt("A light cream colored mini golden doodle",
                        OpenAiImageOptions.builder()
                                .withQuality("hd")
                                .withN(4)
                                .withHeight(1024)
                                .withWidth(1024).build())

        );
        return response;
    }
}
