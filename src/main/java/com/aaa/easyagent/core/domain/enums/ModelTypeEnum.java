package com.aaa.easyagent.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * todo
 *
 * @author liuzhen.tian
 * @version 1.0 ModelType.java  2025/5/25 22:54
 */
@Getter
@AllArgsConstructor
public enum ModelTypeEnum {
    deepseek("deepseek", "123"),
    openai("openai", "123"),
    ollama("ollama", "345");

    private String model;
    private String version;
}
