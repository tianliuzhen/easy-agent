package com.aaa.easyagent.core.domain.enums;

/**
 * 工具的回调方式
 *
 * @author liuzhen.tian
 * @version 1.0 ToolRunMode.java  2025/7/21 23:10
 */
public enum ToolRunMode {
    ReAct,
    Tool

    ;

    public static ToolRunMode getByMode(String mode) {
        for (ToolRunMode value : values()) {
            if (value.name().equals(mode)) {
                return value;
            }
        }
        return null;
    }
}
