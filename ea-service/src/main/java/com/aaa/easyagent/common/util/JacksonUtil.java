package com.aaa.easyagent.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 JacksonUtil.java  2025/4/19 22:14
 */
public class JacksonUtil {
    // 创建一个静态的 ObjectMapper 实例
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 自动注册 classpath 上的模块（含 jackson-datatype-jsr310），支持 Java 8 日期时间类型
        objectMapper.findAndRegisterModules();
        // 日期时间序列化为 ISO 字符串而非时间戳数字
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 反序列化时忽略未知字段，避免 SQL 结果等动态结构反序列化失败
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 将对象转换为 JSON 字符串
     * <p>
     * <h1> Object.class 类默认无法序列化
     * com.fasterxml.jackson.databind.ser.BeanSerializerFactory#constructBeanOrAddOnSerializer(com.fasterxml.jackson.databind.SerializerProvider, com.fasterxml.jackson.databind.JavaType, com.fasterxml.jackson.databind.BeanDescription, boolean)
     * if (beanDesc.getBeanClass() == Object.class) {
     * return prov.getUnknownTypeSerializer(Object.class);
     * //            throw new IllegalArgumentException("Ca nnot create bean serializer for Object.class");
     * }
     * </h1>
     *
     * @param object 要转换的对象
     * @return JSON 字符串，如果转换失败则返回 null
     */
    public static String beanToStr(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // 在实际应用中，可以考虑使用日志记录工具记录异常
            return null;
        }
    }

    public static <T> T strToBean(String json, Class<T> clazz) throws JsonProcessingException {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON to Map: " + json, e);
        }
    }

}
