package com.aaa.easyagent.llm.common;

import com.aaa.easyagent.llm.deepseek.DeepSeekTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/**
 * 源码解析： org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor#augmentWithFormatInstructions
 *
 * @author liuzhen.tian
 * @version 1.0 StructuredOutputTest.java  2026/5/17 1:03
 */
public class StructuredOutputTest {

    @Test
    public  void chatModel() {
        DeepSeekChatModel chatModel = DeepSeekTest.createChatModel();
        BeanOutputConverter<ActorsFilms> beanOutputConverter = new BeanOutputConverter<>(ActorsFilms.class);

        String format = beanOutputConverter.getFormat();

        String actor = "Tom Hanks";

        String template = """
        Generate the filmography of 5 movies for {actor}.
        {format}
        """;

        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).variables(Map.of("actor", actor, "format", format)).build();
        Generation generation = chatModel.call(promptTemplate.create()).getResult();

        ActorsFilms actorsFilms = beanOutputConverter.convert(generation.getOutput().getText());
    }

    record ActorsFilms(String actor, List<String> movies) {
    }

    @Test
    public  void chatModel2() {
        DeepSeekChatModel chatModel = DeepSeekTest.createChatModel();
        Map<String, Object> result = ChatClient.create(chatModel).prompt()
                .user(u -> u.text("Provide me a List of {subject}")
                        .param("subject", "an array of numbers from 1 to 9 under their key name 'numbers'"))
                .call()
                .entity(new ParameterizedTypeReference<Map<String, Object>>() {});
        System.out.println(result);
    }

    @Test
    public  void chatModel3() {
        DeepSeekChatModel chatModel = DeepSeekTest.createChatModel();
        ActorsFilms actorsFilms = ChatClient.create(chatModel).prompt()
                .user(u -> u.text("Generate the filmography of 5 movies for {actor}.")
                        .param("actor", "Tom Hanks"))
                .call()
                .entity(ActorsFilms.class);
        System.out.println(actorsFilms);
    }
}
