package com.aaa.easyagent.core.domain.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具参数模板类
 * 用于封装 MCP Server 调用所需的各种参数
 *
 * @author liuzhen.tian
 * @version 1.0 McpParamsTemplate.java  2026/4/3
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpParamsTemplate extends ParamsTemplate {

    /**
     * MCP Server 名称/标识
     */
    private String serverName;

    /**
     * MCP Server URL (SSE 模式使用)
     */
    private String serverUrl;

    /**
     * 传输类型：STDIO / SSE / STREAMABLE
     * STDIO: 本地进程通信
     * SSE: 传统 SSE (已废弃但兼容)
     * STREAMABLE: 新的 Streamable HTTP (推荐)
     */
    private String transportType = "STREAMABLE";

    /**
     * STDIO 模式下的启动命令
     */
    private String command;

    /**
     * 环境变量列表，格式为 key=value
     */
    private List<String> envVars;

    /**
     * 要调用的具体工具名
     */
    private String toolName;

    /**
     * 连接超时时间（秒）
     */
    private Integer connectionTimeout = 30;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 将环境变量列表转换为 Map
     *
     * @return 环境变量 Map
     */
    public Map<String, String> buildEnvVarsMap() {
        if (envVars == null || envVars.isEmpty()) {
            return Map.of();
        }
        return envVars.stream()
                .map(env -> env.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(java.util.HashMap::new,
                        (m, parts) -> m.put(parts[0].trim(), parts[1].trim()),
                        java.util.HashMap::putAll);
    }

    /**
     * 是否为 STDIO 传输类型
     *
     * @return true 如果是 STDIO 模式
     */
    public boolean isStdioTransport() {
        return "STDIO".equalsIgnoreCase(transportType);
    }

    /**
     * 是否为 SSE 传输类型
     *
     * @return true 如果是 SSE 模式
     */
    public boolean isSseTransport() {
        return "SSE".equalsIgnoreCase(transportType);
    }

    /**
     * 是否为 Streamable HTTP 传输类型
     *
     * @return true 如果是 Streamable HTTP 模式
     */
    public boolean isStreamableTransport() {
        return transportType == null ||
                "STREAMABLE".equalsIgnoreCase(transportType) ||
                "STREAMABLE_HTTP".equalsIgnoreCase(transportType);
    }
}
