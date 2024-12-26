package com.aaa.springai.web;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * 13k star! 获取免费ChatGPT API Key的开源项目，亲测可用！:  https://mp.weixin.qq.com/s?__biz=MzU0MzI3MzU1Ng==&mid=2247484719&idx=1&sn=33b4f623d421216fae26ddcae446e4da&chksm=fb0ca214cc7b2b0215d12a29ff091c5840798b8b15657212d248f73ae6b7c63b2fc28e72bf4a&scene=21#wechat_redirect
 * Free ChatGPT API Key: https://mp.weixin.qq.com/s?__biz=MzU0MzI3MzU1Ng==&mid=2247484719&idx=1&sn=33b4f623d421216fae26ddcae446e4da&chksm=fb0ca214cc7b2b0215d12a29ff091c5840798b8b15657212d248f73ae6b7c63b2fc28e72bf4a&scene=21#wechat_redirect
 *
 * @author liuzhen.tian
 * @version 1.0 OpenApiChatController.java  2024/10/27 18:52
 */
@RestController
public class OpenApiChatController {
    @Resource
    private OpenAiChatModel openAiChatModel;

    /**
     * 聊天的方法。底层调用的openAi的方法
     * RequestParam 接受参数
     * msg 就是我们提的问题
     *
     * @return
     */
    @RequestMapping("/ai/chat")
    public String chat(@RequestParam(value = "msg",defaultValue = "你好") String msg) {
        String called = openAiChatModel.call(msg);
        return called;
    }

    /**
     *OpenAiChatOptions.builder() 传入的一个参数，可以控制大模型的设置
     * @param msg
     * @return
     */
    @RequestMapping("/ai/chat3")
    public Object chat3(@RequestParam("msg") String msg){
        ChatResponse chatResponse = openAiChatModel.call(new Prompt(msg, OpenAiChatOptions.builder()
                //.withModel("gpt-4-32k")  //gpt的版本 ，32K是参数，参数越高，回答问题越准确
                .withTemperature(0.4)  //温度值，温度越高，回答的准确率越低，温度越低，回答的准确率越高
                .build()));
        return chatResponse.getResult().getOutput().getContent();
    }
}
