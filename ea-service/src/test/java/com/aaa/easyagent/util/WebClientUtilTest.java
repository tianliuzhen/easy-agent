package com.aaa.easyagent.util;

import com.aaa.easyagent.common.util.WebClientUtil;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * @author liuzhen.tian
 * @version 1.0 WebClientUtilTest.java  2025/5/25 21:33
 */
public class WebClientUtilTest {

    @Test
    public void springWebClient() {
        String s = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);
        System.out.println(s);
    }

    @Test
    public void springWebClient2() {

        HashMap<Object, Object> map = new HashMap<>();
        map.put("test", "123");
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(111L);
        Object o = new Object();
        String s = WebClientUtil.post("http://localhost:8080/example/testRequestBody", o, String.class);
        System.out.println(s);
    }

    @Test
    public void writeValueAsString() {
        // 创建ObjectMapper实例
        ObjectMapper objectMapper = new ObjectMapper();

        // 可选：启用漂亮的打印（格式化输出）
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 创建一个Person对象

        try {
            // 将Java对象序列化为JSON字符串
            String jsonString = objectMapper.writeValueAsString(new Object());

            // 输出结果
            System.out.println("序列化后的JSON字符串:");
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            /**
             * com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found
             * for class java.lang.Object and no properties
             * discovered to create BeanSerializer
             * (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
             */
            e.printStackTrace();
        }
    }

    @Test
    public void equalObject() {
        Class<?> stringClass = String.class;
        Class<?> objectClass = Object.class;
        Class<?> integerClass = Integer.class;

        System.out.println(stringClass);
        System.out.println(Object.class);
        System.out.println(stringClass == Object.class);  // false（String 是 Object 的子类）
        System.out.println(objectClass == Object.class);  // true（就是 Object 类本身）
        System.out.println(integerClass == Object.class); // false（Integer 是 Object 的子类）

    }
}
