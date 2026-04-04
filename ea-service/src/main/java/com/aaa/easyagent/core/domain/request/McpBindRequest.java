package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * MCP绑定请求
 *
 * @author liuzhen.tian
 * @version 1.0 McpBindRequest.java  2026/4/4
 */
@Data
public class McpBindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * MCP配置ID
     */
    private Long mcpConfigId;

    /**
     * MCP名称（冗余存储，方便查询）
     */
    private String mcpName;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 绑定配置（可选，用于覆盖默认参数）
     */
    private String bindingConfig;
}
