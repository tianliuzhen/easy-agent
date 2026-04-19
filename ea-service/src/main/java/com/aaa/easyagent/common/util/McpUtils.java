package com.aaa.easyagent.common.util;

import com.aaa.easyagent.core.domain.enums.McpTransportTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具类
 * 提供 MCP 相关的通用工具方法
 *
 * @author liuzhen.tian
 * @version 1.0 McpUtils.java  2026/4/4
 */
public class McpUtils {

    /**
     * 解析传输类型字符串为枚举
     *
     * @param transportType 传输类型字符串
     * @return 传输类型枚举，默认为 STREAMABLE
     */
    public static McpTransportTypeEnum parseTransportType(String transportType) {
        return McpTransportTypeEnum.getByCode(transportType);
    }

    /**
     * 从完整 URL 提取基础 URL
     * 例如: http://localhost:8083/api/mcp -> http://localhost:8083/
     *
     * @param serverUrl 完整 URL
     * @return 基础 URL
     */
    public static String extractBaseUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return serverUrl;
        }

        // 移除协议部分
        String withoutProtocol = serverUrl.replaceFirst("^https?://", "");

        // 找到第一个 / 的位置（即 host:port 后面的路径开始位置）
        int firstSlashIndex = withoutProtocol.indexOf('/');
        if (firstSlashIndex > 0) {
            // 在原始 URL 中找到对应的位置
            int protocolLength = serverUrl.length() - withoutProtocol.length();
            return serverUrl.substring(0, protocolLength + firstSlashIndex + 1);
        }

        return serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    /**
     * 从完整 URL 提取 endpoint
     * 例如: http://localhost:8083/api/mcp -> api/mcp
     *
     * @param serverUrl 完整 URL
     * @return endpoint，默认返回 "mcp"
     */
    public static String extractEndpointFromUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return "mcp";
        }

        // 移除协议部分
        String withoutProtocol = serverUrl.replaceFirst("^https?://", "");

        // 找到第一个 / 之后的路径
        int firstSlashIndex = withoutProtocol.indexOf('/');
        if (firstSlashIndex > 0 && firstSlashIndex < withoutProtocol.length() - 1) {
            String endpoint = withoutProtocol.substring(firstSlashIndex + 1);
            // 移除末尾的 /
            return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        }

        return "mcp";
    }

    /**
     * 解析环境变量列表为 Map
     *
     * @param envVars 环境变量列表，格式为 "KEY=VALUE"
     * @return 环境变量 Map
     */
    public static Map<String, String> parseEnvVars(List<String> envVars) {
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
     * 构建缓存 key
     *
     * @param serverName    服务器名称
     * @param transportType 传输类型
     * @param identifier    标识符（URL 或命令）
     * @return 缓存 key
     */
    public static String buildCacheKey(String serverName, String transportType, String identifier) {
        return serverName + "_" + transportType.toUpperCase() + "_" + identifier;
    }
}
