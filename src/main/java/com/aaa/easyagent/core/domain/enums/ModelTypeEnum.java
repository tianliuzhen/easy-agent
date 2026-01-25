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
    deepseek("deepseek", "deepseek", "https://chat.deepseek.com/"),
    siliconflow("siliconflow", "硅基流动", "https://cloud.siliconflow.cn/"),
    openai("openai", "openai", "https://openApi.com/"),
    ollama("ollama", "ollama", "https://ollama.com/");

    private String model;
    private String desc;
    private String links;

    /**
     * 查询所有大模型配置，并且返回所有的枚举信息
     *
     * @return Map<模型名称, Map<属性名, 属性值>>
     */
    public static Map<String, HashMap<String, String>> getAll() {
        Map<String, HashMap<String, String>> result = new HashMap<>();

        for (ModelTypeEnum modelType : values()) {
            HashMap<String, String> modelInfo = new HashMap<>();
            modelInfo.put("model", modelType.getModel());
            modelInfo.put("desc", modelType.getDesc());
            modelInfo.put("links", modelType.getLinks());

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
