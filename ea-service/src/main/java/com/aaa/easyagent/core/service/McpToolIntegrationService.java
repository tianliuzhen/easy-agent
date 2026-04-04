package com.aaa.easyagent.core.service;

import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.DO.EaMcpRelationDO;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.result.McpServerConfigResult;
import com.aaa.easyagent.core.domain.result.McpToolInfoResult;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.McpParamsTemplate;
import com.aaa.easyagent.core.mapper.EaMcpConfigDAO;
import com.aaa.easyagent.core.mapper.EaMcpRelationDAO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 工具集成服务
 * 负责将 MCP 工具转换为标准 ToolConfig，以及 Agent 绑定 MCP 工具
 *
 * @author liuzhen.tian
 * @version 1.0 McpToolIntegrationService.java  2026/4/3
 */
@Slf4j
@Service
public class McpToolIntegrationService {

    @Resource
    private EaMcpConfigDAO eaMcpConfigDAO;

    @Resource
    private EaMcpRelationDAO mcpRelationDAO;

    @Resource
    private McpServerConfigService mcpServerConfigService;

    /**
     * 将 MCP 配置转换为 ToolDefinition 列表
     * 通过调用 fetchToolsFromServer 获取真实的工具列表（包含 inputSchema 和 outputSchema）
     *
     * @param agentId Agent ID
     * @return ToolDefinition 列表
     */
    public List<ToolDefinition<?>> getMcpToolsForAgent(Long agentId) {
        // 1. 查询 Agent 绑定的 MCP 关系
        Example relationExample = new Example(EaMcpRelationDO.class);
        relationExample.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("isActive", true);
        List<EaMcpRelationDO> relations = mcpRelationDAO.selectByExample(relationExample);

        if (relations.isEmpty()) {
            return List.of();
        }

        // 2. 查询对应的 MCP 配置
        List<Long> mcpConfigIds = relations.stream()
                .map(EaMcpRelationDO::getMcpConfigId)
                .collect(Collectors.toList());

        Example configExample = new Example(EaMcpConfigDO.class);
        configExample.createCriteria().andIn("id", mcpConfigIds);
        List<EaMcpConfigDO> configs = eaMcpConfigDAO.selectByExample(configExample);

        // 3. 对每个配置调用 fetchToolsFromServer 获取工具列表，并转换为 ToolDefinition
        List<ToolDefinition<?>> allTools = new ArrayList<>();
        for (EaMcpConfigDO config : configs) {
            try {
                // 调用 fetchToolsFromServer 获取真实的工具列表
                List<McpToolInfoResult> tools = mcpServerConfigService.fetchToolsFromServer(config.getId());
                if (tools != null && !tools.isEmpty()) {
                    // 将每个工具转换为 ToolDefinition
                    for (McpToolInfoResult tool : tools) {
                        ToolDefinition<?> toolDef = convertMcpToolToToolDefinition(config, tool);
                        allTools.add(toolDef);
                    }
                }
            } catch (Exception e) {
                log.warn("获取 MCP 工具列表失败: configId={}, serverName={}", config.getId(), config.getServerName(), e);
                // 如果 fetchToolsFromServer 失败，回退到使用配置中的单个工具
                allTools.add(convertToToolDefinition(config));
            }
        }

        return allTools;
    }

    /**
     * 将 MCP 工具信息转换为 ToolDefinition
     */
    private ToolDefinition<McpParamsTemplate> convertMcpToolToToolDefinition(EaMcpConfigDO config, McpToolInfoResult tool) {
        // 构建 McpParamsTemplate
        McpParamsTemplate paramsTemplate = new McpParamsTemplate();
        paramsTemplate.setServerName(config.getServerName());
        paramsTemplate.setServerUrl(config.getServerUrl());
        paramsTemplate.setTransportType(config.getTransportType());
        paramsTemplate.setCommand(config.getCommand());
        paramsTemplate.setToolName(tool.getName()); // 使用工具的真实名称
        paramsTemplate.setConnectionTimeout(config.getConnectionTimeout());
        paramsTemplate.setMaxRetries(config.getMaxRetries());

        // 解析环境变量
        if (StringUtils.isNotBlank(config.getEnvVars())) {
            try {
                List<String> envVars = JSON.parseArray(config.getEnvVars(), String.class);
                paramsTemplate.setEnvVars(envVars);
            } catch (Exception e) {
                log.warn("解析环境变量失败: {}", config.getEnvVars());
            }
        }

        // 解析 inputSchema 为 InputTypeSchema 列表
        List<InputTypeSchema> inputSchemas = parseInputSchema(tool.getInputSchema());

        return ToolDefinition.<McpParamsTemplate>builder()
                .toolId(config.getId())
                .toolName(tool.getName())
                .toolDesc(tool.getDescription())
                .toolType(ToolTypeEnum.MCP)
                .inputTypeSchemas(inputSchemas)
                .outputTypeSchema(tool.getOutputSchema())
                .paramsTemplate(paramsTemplate)
                .build();
    }

    /**
     * 将 MCP 配置转换为 ToolDefinition
     */
    public ToolDefinition<McpParamsTemplate> convertToToolDefinition(EaMcpConfigDO config) {
        // 构建 McpParamsTemplate
        McpParamsTemplate paramsTemplate = new McpParamsTemplate();
        paramsTemplate.setServerName(config.getServerName());
        paramsTemplate.setServerUrl(config.getServerUrl());
        paramsTemplate.setTransportType(config.getTransportType());
        paramsTemplate.setCommand(config.getCommand());
        paramsTemplate.setToolName(config.getToolName());
        paramsTemplate.setConnectionTimeout(config.getConnectionTimeout());
        paramsTemplate.setMaxRetries(config.getMaxRetries());

        // 解析环境变量
        if (StringUtils.isNotBlank(config.getEnvVars())) {
            try {
                List<String> envVars = JSON.parseArray(config.getEnvVars(), String.class);
                paramsTemplate.setEnvVars(envVars);
            } catch (Exception e) {
                log.warn("解析环境变量失败: {}", config.getEnvVars());
            }
        }

        // 解析 inputSchema 为 InputTypeSchema 列表
        List<InputTypeSchema> inputSchemas = parseInputSchema(config.getInputSchema());

        return ToolDefinition.<McpParamsTemplate>builder()
                .toolId(config.getId())
                .toolName(config.getToolDisplayName() != null ? config.getToolDisplayName() : config.getToolName())
                .toolDesc(config.getToolDescription())
                .toolType(ToolTypeEnum.MCP)
                .inputTypeSchemas(inputSchemas)
                .outputTypeSchema(config.getOutputSchema())
                .paramsTemplate(paramsTemplate)
                .build();
    }

    /**
     * 将 MCP 配置转换为标准的 EaToolConfigDO
     * 用于在工具管理界面显示
     */
    public EaToolConfigDO convertToToolConfig(EaMcpConfigDO config) {
        EaToolConfigDO toolConfig = new EaToolConfigDO();
        toolConfig.setId(config.getId()); // 使用 MCP 配置 ID
        toolConfig.setToolType(ToolTypeEnum.MCP.getType());
        toolConfig.setToolInstanceName(config.getToolDisplayName() != null ? config.getToolDisplayName() : config.getToolName());
        toolConfig.setToolInstanceDesc(config.getToolDescription());
        toolConfig.setInputTemplate(config.getInputSchema());
        toolConfig.setOutTemplate(config.getOutputSchema());
        toolConfig.setToolValue(JSON.toJSONString(buildParamsTemplate(config)));
        toolConfig.setIsActive("active".equals(config.getStatus()));
        toolConfig.setCreatedAt(config.getCreatedAt());
        toolConfig.setUpdatedAt(config.getUpdatedAt());
        return toolConfig;
    }

    /**
     * 绑定 MCP 工具到 Agent
     *
     * @param agentId      Agent ID
     * @param mcpConfigId  MCP 配置 ID
     * @param bindingConfig 绑定配置（可选，用于覆盖默认参数）
     * @return 是否成功
     */
    public boolean bindMcpToolToAgent(Long agentId, Long mcpConfigId, String bindingConfig) {
        // 检查 MCP 配置是否存在
        EaMcpConfigDO config = eaMcpConfigDAO.selectByPrimaryKey(mcpConfigId);
        if (config == null) {
            throw new RuntimeException("MCP 配置不存在: id=" + mcpConfigId);
        }

        // 检查是否已绑定
        Example example = new Example(EaMcpRelationDO.class);
        example.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("mcpConfigId", mcpConfigId);
        List<EaMcpRelationDO> existing = mcpRelationDAO.selectByExample(example);

        if (!existing.isEmpty()) {
            // 已存在，更新绑定配置
            EaMcpRelationDO relation = existing.get(0);
            relation.setBindingConfig(bindingConfig);
            relation.setUpdatedAt(new Date());
            relation.setIsActive(true);
            return mcpRelationDAO.updateByPrimaryKeySelective(relation) > 0;
        }

        // 创建新的绑定关系
        EaMcpRelationDO relation = new EaMcpRelationDO();
        relation.setAgentId(agentId);
        relation.setMcpConfigId(mcpConfigId);
        relation.setBindingConfig(bindingConfig);
        relation.setSortOrder(0);
        relation.setIsActive(true);
        relation.setCreatedAt(new Date());
        relation.setUpdatedAt(new Date());

        return mcpRelationDAO.insertSelective(relation) > 0;
    }

    /**
     * 解绑 MCP 工具
     *
     * @param agentId     Agent ID
     * @param mcpConfigId MCP 配置 ID
     * @return 是否成功
     */
    public boolean unbindMcpToolFromAgent(Long agentId, Long mcpConfigId) {
        Example example = new Example(EaMcpRelationDO.class);
        example.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("mcpConfigId", mcpConfigId);
        return mcpRelationDAO.deleteByExample(example) > 0;
    }

    /**
     * 获取所有MCP配置列表
     *
     * @return MCP配置结果列表
     */
    public List<McpServerConfigResult> getAllMcpConfigs() {
        List<EaMcpConfigDO> configs = eaMcpConfigDAO.selectAll();
        return configs.stream()
                .map(config -> {
                    McpServerConfigResult result = new McpServerConfigResult();
                    BeanUtils.copyProperties(config, result);
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取官方MCP配置列表（user_id = 0）
     *
     * @return 官方MCP配置结果列表
     */
    public List<McpServerConfigResult> getOfficialMcpConfigs() {
        Example example = new Example(EaMcpConfigDO.class);
        example.createCriteria().andEqualTo("userId", 0);
        List<EaMcpConfigDO> configs = eaMcpConfigDAO.selectByExample(example);
        
        return configs.stream()
                .map(config -> {
                    McpServerConfigResult result = new McpServerConfigResult();
                    BeanUtils.copyProperties(config, result);
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取 Agent 绑定的 MCP 工具列表
     *
     * @param agentId Agent ID
     * @return MCP 配置结果列表
     */
    public List<McpServerConfigResult> getBoundMcpConfigsByAgentId(Long agentId) {
        // 1. 查询绑定关系
        Example relationExample = new Example(EaMcpRelationDO.class);
        relationExample.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("isActive", true);
        List<EaMcpRelationDO> relations = mcpRelationDAO.selectByExample(relationExample);

        if (relations.isEmpty()) {
            return List.of();
        }

        // 2. 查询配置
        List<Long> mcpConfigIds = relations.stream()
                .map(EaMcpRelationDO::getMcpConfigId)
                .collect(Collectors.toList());

        Example configExample = new Example(EaMcpConfigDO.class);
        configExample.createCriteria().andIn("id", mcpConfigIds);
        List<EaMcpConfigDO> configs = eaMcpConfigDAO.selectByExample(configExample);

        // 3. 转换为 Result
        return configs.stream()
                .map(config -> {
                    McpServerConfigResult result = new McpServerConfigResult();
                    BeanUtils.copyProperties(config, result);
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析 inputSchema JSON 为 InputTypeSchema 列表
     */
    private List<InputTypeSchema> parseInputSchema(String inputSchema) {
        if (StringUtils.isBlank(inputSchema)) {
            return List.of();
        }

        try {
            JSONObject schema = JSON.parseObject(inputSchema);
            JSONObject properties = schema.getJSONObject("properties");

            if (properties == null || properties.isEmpty()) {
                return List.of();
            }

            List<InputTypeSchema> schemas = new ArrayList<>();
            JSONArray required = schema.getJSONArray("required");

            for (String key : properties.keySet()) {
                JSONObject prop = properties.getJSONObject(key);
                InputTypeSchema inputTypeSchema = new InputTypeSchema();
                inputTypeSchema.setName(key);
                inputTypeSchema.setType(prop.getString("type"));
                inputTypeSchema.setDescription(prop.getString("description"));
                // inputTypeSchema.setRequired(required != null && required.contains(key));
                // MCP 工具的 referenceValue 使用参数名直接映射
                inputTypeSchema.setReferenceValue(key);
                schemas.add(inputTypeSchema);
            }

            return schemas;
        } catch (Exception e) {
            log.warn("解析 inputSchema 失败: {}", inputSchema, e);
            return List.of();
        }
    }

    /**
     * 构建 McpParamsTemplate
     */
    private McpParamsTemplate buildParamsTemplate(EaMcpConfigDO config) {
        McpParamsTemplate template = new McpParamsTemplate();
        template.setServerName(config.getServerName());
        template.setServerUrl(config.getServerUrl());
        template.setTransportType(config.getTransportType());
        template.setCommand(config.getCommand());
        template.setToolName(config.getToolName());
        template.setConnectionTimeout(config.getConnectionTimeout());
        template.setMaxRetries(config.getMaxRetries());

        if (StringUtils.isNotBlank(config.getEnvVars())) {
            try {
                List<String> envVars = JSON.parseArray(config.getEnvVars(), String.class);
                template.setEnvVars(envVars);
            } catch (Exception e) {
                log.warn("解析环境变量失败: {}", config.getEnvVars());
            }
        }

        return template;
    }
}
