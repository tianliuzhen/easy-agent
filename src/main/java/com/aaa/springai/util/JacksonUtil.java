package com.aaa.springai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author liuzhen.tian
 * @version 1.0 JacksonUtil.java  2025/4/19 22:14
 */
public class JacksonUtil {
    // 创建一个静态的 ObjectMapper 实例
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将对象转换为 JSON 字符串
     * <p>
     * <h1> Object.class 类默认无法序列化
     * com.fasterxml.jackson.databind.ser.BeanSerializerFactory#constructBeanOrAddOnSerializer(com.fasterxml.jackson.databind.SerializerProvider, com.fasterxml.jackson.databind.JavaType, com.fasterxml.jackson.databind.BeanDescription, boolean)
     * if (beanDesc.getBeanClass() == Object.class) {
     * return prov.getUnknownTypeSerializer(Object.class);
     * //            throw new IllegalArgumentException("Cannot create bean serializer for Object.class");
     * }
     * </h1>
     *
     * @param object 要转换的对象
     * @return JSON 字符串，如果转换失败则返回 null
     */
    public static String toStr(Object object) {
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

    public static <T> T strToBean(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
