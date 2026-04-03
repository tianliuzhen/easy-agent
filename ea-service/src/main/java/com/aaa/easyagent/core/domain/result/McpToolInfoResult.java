package com.aaa.easyagent.core.domain.result;

import lombok.Data;

/**
 * MCP 工具信息结果
 *
 * @author liuzhen.tian
 * @version 1.0 McpToolInfoResult.java  2026/4/3
 */
@Data
public class McpToolInfoResult {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 输入参数 Schema
     */
    private String inputSchema;

    /**
     * 是否已在本地配置
     */
    private Boolean isConfigured;

    /**
     * 本地配置ID（如果已配置）
     */
    private Long configId;
}
