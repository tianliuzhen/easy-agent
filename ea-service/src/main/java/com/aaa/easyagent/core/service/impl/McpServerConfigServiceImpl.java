package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.request.McpServerConfigRequest;
import com.aaa.easyagent.core.domain.result.McpServerConfigResult;
import com.aaa.easyagent.core.domain.result.McpToolInfoResult;
import com.aaa.easyagent.core.mapper.EaMcpConfigDAO;
import com.aaa.easyagent.core.service.McpServerConfigService;
import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Server 配置管理服务实现
 *
 * @author liuzhen.tian
 * @version 1.0 McpServerConfigServiceImpl.java  2026/4/3
 */
@Slf4j
@Service
public class McpServerConfigServiceImpl implements McpServerConfigService {

    @Resource
    private EaMcpConfigDAO mcpConfigDAO;

    @Override
    @Transactional
    public Long createConfig(McpServerConfigRequest request) {
        EaMcpConfigDO config = new EaMcpConfigDO();
        config.setServerName(request.getServerName());
        config.setServerUrl(request.getServerUrl());
        config.setTransportType(request.getTransportType());
        config.setCommand(request.getCommand());
        config.setEnvVars(request.getEnvVars() != null ? JSON.toJSONString(request.getEnvVars()) : null);
        config.setToolName(request.getToolName());
        config.setToolDisplayName(request.getToolDisplayName());
        config.setToolDescription(request.getToolDescription());
        config.setInputSchema(request.getInputSchema());
        config.setOutputSchema(request.getOutputSchema());
        config.setToolMetadata(request.getToolMetadata());
        config.setConnectionTimeout(request.getConnectionTimeout());
        config.setMaxRetries(request.getMaxRetries());
        config.setStatus(request.getStatus());
        config.setDescription(request.getDescription());
        config.setCreatedAt(new Date());
        config.setUpdatedAt(new Date());

        mcpConfigDAO.insertSelective(config);
        return config.getId();
    }

    @Override
    @Transactional
    public boolean updateConfig(Long id, McpServerConfigRequest request) {
        EaMcpConfigDO config = mcpConfigDAO.selectByPrimaryKey(id);
        if (config == null) {
            return false;
        }

        config.setServerName(request.getServerName());
        config.setServerUrl(request.getServerUrl());
        config.setTransportType(request.getTransportType());
        config.setCommand(request.getCommand());
        config.setEnvVars(request.getEnvVars() != null ? JSON.toJSONString(request.getEnvVars()) : null);
        config.setToolName(request.getToolName());
        config.setToolDisplayName(request.getToolDisplayName());
        config.setToolDescription(request.getToolDescription());
        config.setInputSchema(request.getInputSchema());
        config.setOutputSchema(request.getOutputSchema());
        config.setToolMetadata(request.getToolMetadata());
        config.setConnectionTimeout(request.getConnectionTimeout());
        config.setMaxRetries(request.getMaxRetries());
        config.setStatus(request.getStatus());
        config.setDescription(request.getDescription());
        config.setUpdatedAt(new Date());

        return mcpConfigDAO.updateByPrimaryKeySelective(config) > 0;
    }

    @Override
    @Transactional
    public boolean deleteConfig(Long id) {
        return mcpConfigDAO.deleteByPrimaryKey(id) > 0;
    }

    @Override
    public McpServerConfigResult getConfigById(Long id) {
        EaMcpConfigDO config = mcpConfigDAO.selectByPrimaryKey(id);
        if (config == null) {
            return null;
        }
        return convertToResult(config);
    }

    @Override
    public List<McpServerConfigResult> listAllConfigs() {
        List<EaMcpConfigDO> configs = mcpConfigDAO.selectAll();
        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<McpServerConfigResult> listConfigsByServerName(String serverName) {
        List<EaMcpConfigDO> configs = mcpConfigDAO.selectByServerName(serverName);
        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    @Override
    public List<McpToolInfoResult> testConnection(Long id) {
        EaMcpConfigDO config = mcpConfigDAO.selectByPrimaryKey(id);
        if (config == null) {
            throw new RuntimeException("MCP 配置不存在: id=" + id);
        }

        return doTestConnection(config);
    }

    @Override
    public List<McpToolInfoResult> testConnection(McpServerConfigRequest request) {
        EaMcpConfigDO tempConfig = new EaMcpConfigDO();
        tempConfig.setServerName(request.getServerName());
        tempConfig.setServerUrl(request.getServerUrl());
        tempConfig.setTransportType(request.getTransportType());
        tempConfig.setCommand(request.getCommand());
        tempConfig.setEnvVars(request.getEnvVars() != null ? JSON.toJSONString(request.getEnvVars()) : null);
        tempConfig.setConnectionTimeout(request.getConnectionTimeout());
        tempConfig.setMaxRetries(request.getMaxRetries());

        return doTestConnection(tempConfig);
    }

    private List<McpToolInfoResult> doTestConnection(EaMcpConfigDO config) {
        McpSyncClient client = null;
        try {
            client = createTempClient(config);

            // 获取工具列表
            McpSchema.ListToolsResult toolsResult = client.listTools();

            // 更新最后连接时间
            config.setLastConnectedAt(new Date());
            config.setStatus("active");
            config.setLastError(null);
            mcpConfigDAO.updateByPrimaryKeySelective(config);

            return convertToToolInfoList(toolsResult.tools());

        } catch (Exception e) {
            log.error("MCP Server 连接测试失败: serverName={}", config.getServerName(), e);

            // 更新错误信息
            config.setStatus("error");
            config.setLastError(e.getMessage());
            mcpConfigDAO.updateByPrimaryKeySelective(config);

            throw new RuntimeException("MCP Server 连接测试失败: " + e.getMessage(), e);
        } finally {
            if (client != null) {
                try {
                    client.closeGracefully();
                } catch (Exception e) {
                    log.warn("关闭 MCP Client 失败", e);
                }
            }
        }
    }

    @Override
    public List<McpToolInfoResult> listTools(Long id) {
        return testConnection(id);
    }

    @Override
    @Transactional
    public int syncTools(Long id) {
        EaMcpConfigDO config = mcpConfigDAO.selectByPrimaryKey(id);
        if (config == null) {
            throw new RuntimeException("MCP 配置不存在: id=" + id);
        }

        List<McpToolInfoResult> tools = testConnection(id);
        if (tools == null || tools.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (McpToolInfoResult tool : tools) {
            // 检查是否已存在
            List<EaMcpConfigDO> existing = mcpConfigDAO.selectByServerNameAndToolName(
                    config.getServerName(), tool.getName());

            if (existing == null || existing.isEmpty()) {
                // 创建新的工具配置
                EaMcpConfigDO newConfig = new EaMcpConfigDO();
                newConfig.setServerName(config.getServerName());
                newConfig.setServerUrl(config.getServerUrl());
                newConfig.setTransportType(config.getTransportType());
                newConfig.setCommand(config.getCommand());
                newConfig.setEnvVars(config.getEnvVars());
                newConfig.setToolName(tool.getName());
                newConfig.setToolDisplayName(tool.getName());
                newConfig.setToolDescription(tool.getDescription());
                newConfig.setInputSchema(tool.getInputSchema());
                newConfig.setConnectionTimeout(config.getConnectionTimeout());
                newConfig.setMaxRetries(config.getMaxRetries());
                newConfig.setStatus("active");
                newConfig.setCreatedAt(new Date());
                newConfig.setUpdatedAt(new Date());

                mcpConfigDAO.insertSelective(newConfig);
                count++;
            }
        }

        return count;
    }

    /**
     * 创建临时 MCP Client（用于测试连接）
     */
    private McpSyncClient createTempClient(EaMcpConfigDO config) {
        String transportType = config.getTransportType();
        Integer timeout = config.getConnectionTimeout() != null ? config.getConnectionTimeout() : 30;

        McpTransportType type = parseTransportType(transportType);
        return switch (type) {
            case STDIO -> createStdioClient(config, timeout);
            case SSE -> createSseClient(config, timeout);
            case STREAMABLE -> createStreamableHttpClient(config, timeout);
        };
    }

    private McpSyncClient createStdioClient(EaMcpConfigDO config, int timeout) {
        if (StringUtils.isBlank(config.getCommand())) {
            throw new RuntimeException("STDIO 模式需要配置启动命令");
        }

        String[] commandParts = config.getCommand().split("\\s+");

        Map<String, String> envVars = Map.of();
        if (StringUtils.isNotBlank(config.getEnvVars())) {
            try {
                List<String> envList = JSON.parseArray(config.getEnvVars(), String.class);
                envVars = parseEnvVars(envList);
            } catch (Exception e) {
                log.warn("解析环境变量失败: {}", config.getEnvVars());
            }
        }

        ServerParameters serverParams = ServerParameters.builder(commandParts[0])
                .args(List.of(commandParts).subList(1, commandParts.length))
                .env(envVars)
                .build();

        StdioClientTransport transport = new StdioClientTransport(serverParams,new JacksonMcpJsonMapper(new JsonMapper()));

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        client.initialize();
        return client;
    }

    private McpSyncClient createSseClient(EaMcpConfigDO config, int timeout) {
        if (StringUtils.isBlank(config.getServerUrl())) {
            throw new RuntimeException("SSE 模式需要配置 Server URL");
        }

        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder(config.getServerUrl())
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        client.initialize();
        return client;
    }

    private McpSyncClient createStreamableHttpClient(EaMcpConfigDO config, int timeout) {
        if (StringUtils.isBlank(config.getServerUrl())) {
            throw new RuntimeException("Streamable HTTP 模式需要配置 Server URL");
        }

        String endpoint = extractEndpointFromUrl(config.getServerUrl());
        String baseUrl = extractBaseUrl(config.getServerUrl());

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .build();

        client.initialize();
        return client;
    }

    private McpTransportType parseTransportType(String transportType) {
        if (StringUtils.isBlank(transportType)) {
            return McpTransportType.STREAMABLE;
        }
        return switch (transportType.toUpperCase()) {
            case "STDIO" -> McpTransportType.STDIO;
            case "SSE", "HTTP" -> McpTransportType.SSE;
            case "STREAMABLE", "STREAMABLE_HTTP" -> McpTransportType.STREAMABLE;
            default -> McpTransportType.STREAMABLE;
        };
    }

    private String extractBaseUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return serverUrl;
        }
        int lastSlashIndex = serverUrl.lastIndexOf('/');
        if (lastSlashIndex > 8) {
            return serverUrl.substring(0, lastSlashIndex + 1);
        }
        return serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    private String extractEndpointFromUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return "mcp";
        }
        String withoutProtocol = serverUrl.replaceFirst("^https?://", "");
        int firstSlashIndex = withoutProtocol.indexOf('/');
        if (firstSlashIndex > 0 && firstSlashIndex < withoutProtocol.length() - 1) {
            String endpoint = withoutProtocol.substring(firstSlashIndex + 1);
            return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        }
        return "mcp";
    }

    /**
     * MCP 传输类型枚举
     */
    private enum McpTransportType {
        STDIO, SSE, STREAMABLE
    }

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

    private List<McpToolInfoResult> convertToToolInfoList(List<McpSchema.Tool> tools) {
        if (tools == null) {
            return new ArrayList<>();
        }

        return tools.stream()
                .map(tool -> {
                    McpToolInfoResult result = new McpToolInfoResult();
                    result.setName(tool.name());
                    result.setDescription(tool.description());
                    result.setInputSchema(tool.inputSchema() != null ?
                            JSON.toJSONString(tool.inputSchema()) : null);
                    result.setIsConfigured(false);
                    return result;
                })
                .collect(Collectors.toList());
    }

    private McpServerConfigResult convertToResult(EaMcpConfigDO config) {
        McpServerConfigResult result = new McpServerConfigResult();
        result.setId(config.getId());
        result.setServerName(config.getServerName());
        result.setServerUrl(config.getServerUrl());
        result.setTransportType(config.getTransportType());
        result.setCommand(config.getCommand());
        result.setEnvVars(config.getEnvVars());
        result.setToolName(config.getToolName());
        result.setToolDisplayName(config.getToolDisplayName());
        result.setToolDescription(config.getToolDescription());
        result.setInputSchema(config.getInputSchema());
        result.setOutputSchema(config.getOutputSchema());
        result.setToolMetadata(config.getToolMetadata());
        result.setConnectionTimeout(config.getConnectionTimeout());
        result.setMaxRetries(config.getMaxRetries());
        result.setStatus(config.getStatus());
        result.setLastConnectedAt(config.getLastConnectedAt());
        result.setLastError(config.getLastError());
        result.setDescription(config.getDescription());
        result.setCreatedAt(config.getCreatedAt());
        result.setUpdatedAt(config.getUpdatedAt());
        return result;
    }
}
