package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * MCP解绑请求
 *
 * @author liuzhen.tian
 * @version 1.0 McpUnbindRequest.java  2026/4/4
 */
@Data
public class McpUnbindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * MCP配置ID
     */
    private Long mcpConfigId;
}
