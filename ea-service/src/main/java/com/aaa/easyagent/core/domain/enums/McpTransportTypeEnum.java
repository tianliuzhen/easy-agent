package com.aaa.easyagent.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 传输类型枚举
 *
 * @author liuzhen.tian
 * @version 1.0 McpTransportTypeEnum.java  2026/4/4
 */
@Getter
@AllArgsConstructor
public enum McpTransportTypeEnum {
    STDIO("STDIO", "本地进程通信"),
    SSE("SSE", "传统 SSE (已废弃但兼容)"),
    STREAMABLE("STREAMABLE", "新的 Streamable HTTP");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     *
     * @param code 传输类型代码
     * @return 枚举值，未找到返回 STREAMABLE
     */
    public static McpTransportTypeEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return STREAMABLE;
        }
        for (McpTransportTypeEnum value : values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        // 兼容旧值
        if ("HTTP".equalsIgnoreCase(code)) {
            return SSE;
        }
        if ("STREAMABLE_HTTP".equalsIgnoreCase(code)) {
            return STREAMABLE;
        }
        return STREAMABLE;
    }
}
