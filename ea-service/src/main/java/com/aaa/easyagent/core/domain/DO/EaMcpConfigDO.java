package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_mcp_config
 * 表注释：MCP服务配置表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_mcp_config")
public class EaMcpConfigDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 用户Id
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 服务器名称
     */
    @Column(name = "server_name")
    private String serverName;

    /**
     * 服务器URL（SSE模式使用）
     */
    @Column(name = "server_url")
    private String serverUrl;

    /**
     * 传输类型：SSE/STDIO
     */
    @Column(name = "transport_type")
    private String transportType;

    /**
     * 工具名称（MCP Server中的原始名称）
     */
    @Column(name = "tool_name")
    private String toolName;

    /**
     * 工具显示名称
     */
    @Column(name = "tool_display_name")
    private String toolDisplayName;

    /**
     * 连接超时时间（秒）
     */
    @Column(name = "connection_timeout")
    private Integer connectionTimeout;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries")
    private Integer maxRetries;

    /**
     * 状态：active/inactive/error
     */
    @Column(name = "status")
    private String status;

    /**
     * 最后连接时间
     */
    @Column(name = "last_connected_at")
    private Date lastConnectedAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 启动命令（STDIO模式使用）
     */
    @Column(name = "command")
    private String command;

    /**
     * 环境变量（JSON数组）
     */
    @Column(name = "env_vars")
    private String envVars;

    /**
     * 工具描述
     */
    @Column(name = "tool_description")
    private String toolDescription;

    /**
     * 输入参数Schema（JSON格式）
     */
    @Column(name = "input_schema")
    private String inputSchema;

    /**
     * 输出参数Schema（JSON格式）
     */
    @Column(name = "output_schema")
    private String outputSchema;

    /**
     * 工具元数据（JSON格式）
     */
    @Column(name = "tool_metadata")
    private String toolMetadata;

    /**
     * 最后错误信息
     */
    @Column(name = "last_error")
    private String lastError;

    /**
     * 描述信息
     */
    @Column(name = "description")
    private String description;
}