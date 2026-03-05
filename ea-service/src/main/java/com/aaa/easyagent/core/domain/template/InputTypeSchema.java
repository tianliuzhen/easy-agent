package com.aaa.easyagent.core.domain.template;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liuzhen.tian
 * @version 1.0 InputTypeSchema.java  2025/5/26 20:37
 */
@Data
@NoArgsConstructor
public class InputTypeSchema {
    /**
     * 参数名
     */
    private String name;
    /**
     * 参数描述
     */
    private String description;
    /**
     * 参数类型
     */
    private String type;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 引用值
     * jsonPath也可以
     */
    private String referenceValue;

    public InputTypeSchema(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public InputTypeSchema(String name, String description, String type, String defaultValue) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public InputTypeSchema(String name, String description, String type, String defaultValue, String referenceValue) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        this.referenceValue = referenceValue;
    }
}
