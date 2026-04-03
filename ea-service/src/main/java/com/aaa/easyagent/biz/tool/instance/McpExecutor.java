package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.McpParamsTemplate;
import com.aaa.easyagent.core.mapper.EaMcpConfigDAO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 传输类型枚举
 */
enum McpTransportType {
    STDIO,      // 本地进程通信
    SSE,        // 传统 SSE (已废弃但兼容)
    STREAMABLE  // 新的 Streamable HTTP
}

/**
 * MCP 工具执行器
 * 支持 STDIO 和 SSE 两种传输方式连接 MCP Server
 *
 * @author liuzhen.tian
 * @version 1.0 McpExecutor.java  2026/4/3
 */
@Slf4j
@Component
@ToolTypeChooser(ToolTypeEnum.MCP)
public class McpExecutor implements ToolExecutor<McpParamsTemplate> {

    @Autowired
    private EaMcpConfigDAO mcpConfigDAO;

    /**
     * MCP Client 连接池缓存
     * key: serverName + "_" + transportType + "_" + (serverUrl|command)
     */
    private final Map<String, McpSyncClient> clientCache = new ConcurrentHashMap<>();

    @Override
    public String call(String functionInput, ToolDefinition<McpParamsTemplate> toolDefinition) {
        boolean debug = toolDefinition.isDebug();
        log.info("McpExecutor call {}: toolName={}, input={}",
                debug ? "debug" : "", toolDefinition.getToolName(), functionInput);

        if (StringUtils.isBlank(functionInput) && !debug) {
            throw new AgentToolException("functionInput 不能为空");
        }

        McpParamsTemplate paramsTemplate = toolDefinition.getParamsTemplate();
        if (paramsTemplate == null) {
            throw new AgentToolException("MCP 参数模板不能为空");
        }

        // 从数据库获取完整的 MCP 配置
        EaMcpConfigDO mcpConfig = getMcpConfig(paramsTemplate);
        if (mcpConfig == null) {
            throw new AgentToolException("未找到 MCP 配置: serverName=" + paramsTemplate.getServerName() +
                    ", toolName=" + paramsTemplate.getToolName());
        }

        // 获取或创建 MCP Client
        McpSyncClient client = getOrCreateClient(mcpConfig, paramsTemplate);

        try {
            // 调用 MCP 工具
            String toolName = StringUtils.isNotBlank(paramsTemplate.getToolName())
                    ? paramsTemplate.getToolName()
                    : mcpConfig.getToolName();

            if (StringUtils.isBlank(toolName)) {
                throw new AgentToolException("MCP 工具名称不能为空");
            }

            // 解析 functionInput 为 JSON 对象
            Map<String, Object> arguments = parseArguments(functionInput);

            log.info("McpExecutor calling tool: serverName={}, toolName={}, arguments={}",
                    mcpConfig.getServerName(), toolName, arguments);

            // 构建 CallToolRequest
            McpSchema.CallToolRequest callRequest = new McpSchema.CallToolRequest(toolName, arguments);

            // 执行工具调用
            McpSchema.CallToolResult result = client.callTool(callRequest);

            // 处理结果
            return processResult(result);

        } catch (Exception e) {
            log.error("MCP 工具调用失败: serverName={}, toolName={}",
                    mcpConfig.getServerName(), paramsTemplate.getToolName(), e);
            throw new AgentToolException("MCP 工具调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从数据库获取 MCP 配置
     */
    private EaMcpConfigDO getMcpConfig(McpParamsTemplate paramsTemplate) {
        // 优先使用 serverName + toolName 查询
        if (StringUtils.isNotBlank(paramsTemplate.getServerName()) &&
                StringUtils.isNotBlank(paramsTemplate.getToolName())) {
            List<EaMcpConfigDO> configs = mcpConfigDAO.selectByServerNameAndToolName(
                    paramsTemplate.getServerName(), paramsTemplate.getToolName());
            if (configs != null && !configs.isEmpty()) {
                return configs.get(0);
            }
        }

        // 仅使用 serverName 查询
        if (StringUtils.isNotBlank(paramsTemplate.getServerName())) {
            List<EaMcpConfigDO> configs = mcpConfigDAO.selectByServerName(paramsTemplate.getServerName());
            if (configs != null && !configs.isEmpty()) {
                return configs.get(0);
            }
        }

        return null;
    }

    /**
     * 获取或创建 MCP Client
     */
    private McpSyncClient getOrCreateClient(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate) {
        String cacheKey = buildCacheKey(mcpConfig, paramsTemplate);

        return clientCache.computeIfAbsent(cacheKey, k -> createClient(mcpConfig, paramsTemplate));
    }

    /**
     * 构建缓存 key
     */
    private String buildCacheKey(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate) {
        String transportType = StringUtils.isNotBlank(paramsTemplate.getTransportType())
                ? paramsTemplate.getTransportType()
                : mcpConfig.getTransportType();

        if ("STDIO".equalsIgnoreCase(transportType)) {
            String command = StringUtils.isNotBlank(paramsTemplate.getCommand())
                    ? paramsTemplate.getCommand()
                    : mcpConfig.getCommand();
            return mcpConfig.getServerName() + "_STDIO_" + command;
        } else {
            String serverUrl = StringUtils.isNotBlank(paramsTemplate.getServerUrl())
                    ? paramsTemplate.getServerUrl()
                    : mcpConfig.getServerUrl();
            return mcpConfig.getServerName() + "_SSE_" + serverUrl;
        }
    }

    /**
     * 创建 MCP Client
     */
    private McpSyncClient createClient(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate) {
        String transportType = StringUtils.isNotBlank(paramsTemplate.getTransportType())
                ? paramsTemplate.getTransportType()
                : mcpConfig.getTransportType();

        Integer timeout = paramsTemplate.getConnectionTimeout() != null
                ? paramsTemplate.getConnectionTimeout()
                : (mcpConfig.getConnectionTimeout() != null ? mcpConfig.getConnectionTimeout() : 30);

        log.info("Creating MCP Client: serverName={}, transportType={}, timeout={}",
                mcpConfig.getServerName(), transportType, timeout);

        try {
            McpTransportType type = parseTransportType(transportType);
            return switch (type) {
                case STDIO -> createStdioClient(mcpConfig, paramsTemplate, timeout);
                case SSE -> createSseClient(mcpConfig, paramsTemplate, timeout);
                case STREAMABLE -> createStreamableHttpClient(mcpConfig, paramsTemplate, timeout);
            };
        } catch (Exception e) {
            log.error("创建 MCP Client 失败: serverName={}", mcpConfig.getServerName(), e);
            throw new AgentToolException("创建 MCP Client 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析传输类型字符串为枚举
     */
    private McpTransportType parseTransportType(String transportType) {
        if (StringUtils.isBlank(transportType)) {
            return McpTransportType.STREAMABLE; // 默认使用新的 Streamable HTTP
        }

        return switch (transportType.toUpperCase()) {
            case "STDIO" -> McpTransportType.STDIO;
            case "SSE", "HTTP" -> McpTransportType.SSE;
            case "STREAMABLE", "STREAMABLE_HTTP" -> McpTransportType.STREAMABLE;
            default -> McpTransportType.STREAMABLE; // 默认使用新的 Streamable HTTP
        };
    }

    /**
     * 创建 STDIO 模式的 MCP Client
     * 参考 WeatherServiceTest.stdioTest()
     */
    private McpSyncClient createStdioClient(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate, int timeout) {
        String command = StringUtils.isNotBlank(paramsTemplate.getCommand())
                ? paramsTemplate.getCommand()
                : mcpConfig.getCommand();

        if (StringUtils.isBlank(command)) {
            throw new AgentToolException("STDIO 模式需要配置启动命令");
        }

        // 解析命令和环境变量
        String[] commandParts = command.split("\\s+");
        Map<String, String> envVars = paramsTemplate.buildEnvVarsMap();

        // 如果 paramsTemplate 中没有环境变量，尝试从数据库配置解析
        if (envVars.isEmpty() && StringUtils.isNotBlank(mcpConfig.getEnvVars())) {
            try {
                List<String> envList = JSON.parseArray(mcpConfig.getEnvVars(), String.class);
                if (envList != null) {
                    envVars = parseEnvVars(envList);
                }
            } catch (Exception e) {
                log.warn("解析环境变量失败: {}", mcpConfig.getEnvVars());
            }
        }

        ServerParameters serverParams = ServerParameters.builder(commandParts[0])
                .args(List.of(commandParts).subList(1, commandParts.length))
                .env(envVars)
                .build();

        // Spring AI 2.0.0-M2 需要使用 McpJsonMapper
        StdioClientTransport transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper(new JsonMapper()));

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        // 初始化连接
        client.initialize();

        return client;
    }

    /**
     * 创建 SSE 模式的 MCP Client (传统 SSE，已废弃但兼容)
     * 参考 WeatherServiceTest.sseHttpTest()
     */
    private McpSyncClient createSseClient(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate, int timeout) {
        String serverUrl = StringUtils.isNotBlank(paramsTemplate.getServerUrl())
                ? paramsTemplate.getServerUrl()
                : mcpConfig.getServerUrl();

        if (StringUtils.isBlank(serverUrl)) {
            throw new AgentToolException("SSE 模式需要配置 Server URL");
        }

        // 使用 builder 模式创建 SSE 传输
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder(serverUrl)
                // .sseEndpoint("/sse") // 默认 /sse，可通过配置覆盖
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        // 初始化连接
        client.initialize();

        return client;
    }

    /**
     * 创建 Streamable HTTP 模式的 MCP Client (推荐)
     * 参考 WeatherServiceTest.streamableHTTP()
     * 这是 Spring AI 2.0.0-M2 推荐的新传输方式
     */
    private McpSyncClient createStreamableHttpClient(EaMcpConfigDO mcpConfig, McpParamsTemplate paramsTemplate, int timeout) {
        String serverUrl = StringUtils.isNotBlank(paramsTemplate.getServerUrl())
                ? paramsTemplate.getServerUrl()
                : mcpConfig.getServerUrl();

        if (StringUtils.isBlank(serverUrl)) {
            throw new AgentToolException("Streamable HTTP 模式需要配置 Server URL");
        }

        // 从 URL 解析 endpoint，默认使用 /mcp
        // 例如: http://localhost:8083/api/mcp -> endpoint = api/mcp
        String endpoint = extractEndpointFromUrl(serverUrl);
        String baseUrl = extractBaseUrl(serverUrl);

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint) // 默认: /mcp
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        // 初始化连接
        client.initialize();

        return client;
    }

    /**
     * 从完整 URL 提取基础 URL
     * 例如: http://localhost:8083/api/mcp -> http://localhost:8083/
     */
    private String extractBaseUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return serverUrl;
        }

        // 移除末尾的路径部分
        int lastSlashIndex = serverUrl.lastIndexOf('/');
        if (lastSlashIndex > 8) { // 跳过 http:// 或 https://
            return serverUrl.substring(0, lastSlashIndex + 1);
        }
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    /**
     * 从完整 URL 提取 endpoint
     * 例如: http://localhost:8083/api/mcp -> api/mcp
     */
    private String extractEndpointFromUrl(String serverUrl) {
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

        return "mcp"; // 默认 endpoint
    }

    /**
     * 解析环境变量列表为 Map
     */
    private Map<String, String> parseEnvVars(List<String> envVars) {
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
     * 解析函数输入参数
     */
    private Map<String, Object> parseArguments(String functionInput) {
        if (StringUtils.isBlank(functionInput)) {
            return Map.of();
        }
        try {
            JSONObject jsonObject = JSON.parseObject(functionInput);
            return jsonObject.getInnerMap();
        } catch (Exception e) {
            log.warn("解析 functionInput 失败: {}", functionInput);
            return Map.of();
        }
    }

    /**
     * 处理 MCP 调用结果
     */
    private String processResult(McpSchema.CallToolResult result) {
        if (result == null) {
            return "{}";
        }

        List<McpSchema.Content> contentList = result.content();
        if (contentList == null || contentList.isEmpty()) {
            return "{}";
        }

        // 提取文本内容
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : contentList) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }

        String resultText = sb.toString();
        if (StringUtils.isBlank(resultText)) {
            return "{}";
        }

        // 尝试解析为 JSON，如果不是则包装为 JSON
        try {
            JSON.parse(resultText);
            return resultText;
        } catch (Exception e) {
            JSONObject wrapper = new JSONObject();
            wrapper.put("result", resultText);
            return wrapper.toJSONString();
        }
    }

    /**
     * 关闭 Client 并清理资源
     */
    private void closeClient(McpSyncClient client) {
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                log.warn("关闭 MCP Client 失败", e);
            }
        }
    }

    /**
     * 清理指定缓存的 Client
     */
    public void invalidateClient(String serverName) {
        clientCache.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(serverName + "_")) {
                closeClient(entry.getValue());
                return true;
            }
            return false;
        });
    }

    /**
     * 清理所有缓存的 Client
     */
    public void invalidateAllClients() {
        clientCache.values().forEach(this::closeClient);
        clientCache.clear();
    }
}
