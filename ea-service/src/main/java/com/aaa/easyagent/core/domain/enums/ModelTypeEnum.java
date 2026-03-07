package com.aaa.easyagent.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * todo
 *
 * @author liuzhen.tian
 * @version 1.0 ModelType.java  2025/5/25 22:54
 */
@Getter
@AllArgsConstructor
public enum ModelTypeEnum {
    deepseek("deepseek", "deepseek", "https://api.deepseek.com/v1", "https://api.deepseek.com", "https://www.deepseek.com/favicon.ico"),
    siliconflow("siliconflow", "硅基流动", "https://cloud.siliconflow.cn/v1", "https://api.siliconflow.cn", "https://cloud.siliconflow.cn/favicon.ico"),
    openai("openai", "openai", "https://openai.com/", "https://api.openai.com/v1", "https://openai.com/favicon.ico"),
    ollama("ollama", "ollama", "https://ollama.com/", "http://localhost:11434", "https://ollama.com/public/ollama.png");

    private String model;
    private String desc;
    private String links;
    private String defaultBaseUrl;
    private String icon;

    /**
     * 查询所有大模型配置，并且返回所有的枚举信息
     *
     * @return Map<模型名称, Map < 属性名, 属性值>>
     */
    public static Map<String, HashMap<String, String>> getAll() {
        Map<String, HashMap<String, String>> result = new HashMap<>();

        for (ModelTypeEnum modelType : values()) {
            HashMap<String, String> modelInfo = new HashMap<>();
            modelInfo.put("model", modelType.getModel());
            modelInfo.put("desc", modelType.getDesc());
            modelInfo.put("links", modelType.getLinks());
            modelInfo.put("defaultBaseUrl", modelType.getDefaultBaseUrl());
            modelInfo.put("icon", modelType.getIcon());

            result.put(modelType.getModel(), modelInfo);
        }

        return result;
    }

    public static ModelTypeEnum getByModel(String model) {
        for (ModelTypeEnum modelType : values()) {
            if (modelType.getModel().equals(model)) {
                return modelType;
            }
        }
        return null;
    }
}
