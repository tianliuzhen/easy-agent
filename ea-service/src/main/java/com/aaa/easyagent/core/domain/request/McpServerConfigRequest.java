package com.aaa.easyagent.core.domain.request;

import lombok.Data;

import java.util.List;

/**
 * MCP Server 配置请求
 *
 * @author liuzhen.tian
 * @version 1.0 McpServerConfigRequest.java  2026/4/3
 */
@Data
public class McpServerConfigRequest {

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器URL（SSE模式使用）
     */
    private String serverUrl;

    /**
     * 传输类型：STDIO/SSE/STREAMABLE
     * STDIO: 本地进程通信
     * SSE: 传统 SSE (已废弃但兼容)
     * STREAMABLE: 新的 Streamable HTTP (推荐，默认)
     */
    private String transportType = "STREAMABLE";

    /**
     * 启动命令（STDIO模式使用）
     */
    private String command;

    /**
     * 环境变量（JSON数组格式）
     */
    private List<String> envVars;

    /**
     * 工具名称（MCP Server中的原始名称）
     */
    private String toolName;

    /**
     * 工具显示名称
     */
    private String toolDisplayName;

    /**
     * 工具描述
     */
    private String toolDescription;

    /**
     * 输入参数Schema（JSON格式）
     */
    private String inputSchema;

    /**
     * 输出参数Schema（JSON格式）
     */
    private String outputSchema;

    /**
     * 连接超时时间（秒）
     */
    private Integer connectionTimeout = 30;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 状态：active/inactive/error
     */
    private String status = "active";

    /**
     * 描述信息
     */
    private String description;

    /**
     * 工具元数据（JSON格式）
     */
    private String toolMetadata;
}
