package com.aaa.easyagent.common.util;

/**
 * @author liuzhen.tian
 * @version 1.0 JsonSchemaGenerator.java  2025/7/22 22:35
 */

import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public class JsonSchemaGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成 JSON Schema
     *
     * @param schemaDefinitions 输入参数定义列表
     * @param fieldDescriptions 特定字段的描述覆盖（key为字段名，value为覆盖的描述）
     * @return JSON Schema 字符串
     */
    public static String generateJsonSchema(
            List<InputTypeSchema> schemaDefinitions,
            Map<String, String> fieldDescriptions) {

        // 创建根 Schema 对象
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");

        // 创建 properties 对象
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode required = objectMapper.createObjectNode();
        required.putArray("required");

        // 处理每个字段定义
        for (InputTypeSchema field : schemaDefinitions) {
            ObjectNode property = objectMapper.createObjectNode();
            property.put("type", field.getType());

            // 设置描述（优先使用覆盖描述）
            String description = field.getDesc();
            if (fieldDescriptions != null && fieldDescriptions.containsKey(field.getName())) {
                description = fieldDescriptions.get(field.getName());
            }
            if (description != null && !description.isEmpty()) {
                property.put("description", description);
            }

            properties.set(field.getName(), property);
            required.withArray("required").add(field.getName());
        }

        // 添加 properties 和 required 到 schema
        schema.set("properties", properties);
        if (schemaDefinitions.size() > 0) {
            schema.set("required", required.get("required"));
        }

        // 禁止额外属性
        schema.put("additionalProperties", false);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON Schema", e);
        }
    }

    /**
     * 简化版生成方法（不覆盖描述）
     */
    public static String generateJsonSchema(List<InputTypeSchema> schemaDefinitions) {
        return generateJsonSchema(schemaDefinitions, null);
    }
}
