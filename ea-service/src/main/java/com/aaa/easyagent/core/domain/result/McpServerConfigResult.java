package com.aaa.easyagent.core.domain.result;

import lombok.Data;

import java.util.Date;

/**
 * MCP Server 配置结果
 *
 * @author liuzhen.tian
 * @version 1.0 McpServerConfigResult.java  2026/4/3
 */
@Data
public class McpServerConfigResult {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器URL（SSE/STREAMABLE模式使用）
     */
    private String serverUrl;

    /**
     * 传输类型：STDIO/SSE/STREAMABLE
     */
    private String transportType;

    /**
     * 启动命令（STDIO模式使用）
     */
    private String command;

    /**
     * 环境变量（JSON数组格式）
     */
    private String envVars;

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
     * 工具元数据（JSON格式）
     */
    private String toolMetadata;

    /**
     * 连接超时时间（秒）
     */
    private Integer connectionTimeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 状态：active/inactive/error
     */
    private String status;

    /**
     * 最后连接时间
     */
    private Date lastConnectedAt;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
