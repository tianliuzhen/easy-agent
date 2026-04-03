package com.aaa.easyagent.biz.agent.data;

import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.McpParamsTemplate;
import com.aaa.easyagent.core.domain.template.ParamsTemplate;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于存储工具相关的信息
 * 包括工具ID、名称、描述、类型以及输入输出参数模式等
 * 供AI代理调用工具时使用
 *
 * @author liuzhen.tian
 * @version 1.0 ToolDefinition.java  2025/5/25 21:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolDefinition<T extends ParamsTemplate> {
    /**
     * 工具Id
     */
    private Long toolId;
    /**
     * 工具名称
     */
    private String toolName;
    /**
     * 工具描述
     */
    private String toolDesc;

    /**
     * 工具类型
     */
    private ToolTypeEnum toolType;

    /**
     * 请求参数结构体
     */
    private List<InputTypeSchema> inputTypeSchemas;

    /**
     * 返回参数结构体
     */
    private String outputTypeSchema;


    /**
     * 参数实例
     * HttpReqParamsTemplate
     * SqlParamsTemplate
     */
    private T paramsTemplate;

    /**
     * 调试模式
     */
    private boolean isDebug;


    public static ToolDefinition buildToolDefinition(EaToolConfigDO eaToolConfigReq) {
        ToolTypeEnum toolTypeEnum = ToolTypeEnum.getByType(eaToolConfigReq.getToolType());
        String toolValue = eaToolConfigReq.getToolValue();
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .toolId(eaToolConfigReq.getId())
                .toolName(eaToolConfigReq.getToolInstanceName())
                .toolDesc(eaToolConfigReq.getToolInstanceDesc())
                // 工具类型
                .toolType(toolTypeEnum)
                // 解析大模型识别的参数
                .inputTypeSchemas(JSON.parseArray(eaToolConfigReq.getInputTemplate(), InputTypeSchema.class))
                // .outputTypeSchema(JSON.parseArray(eaToolConfigReq.getOutTemplate(), InputTypeSchema.class))
                // 解析工具参数元数据
                .paramsTemplate(JSON.parseObject(toolValue, toolTypeEnum.getParamsTemplate()))
                .build();
        return toolDefinition;
    }

    /**
     * 从 MCP 配置构建 ToolDefinition
     *
     * @param mcpConfig MCP 配置
     * @return ToolDefinition
     */
    public static ToolDefinition<McpParamsTemplate> buildToolDefinitionFromMcp(EaMcpConfigDO mcpConfig) {
        // 构建 McpParamsTemplate
        McpParamsTemplate paramsTemplate = new McpParamsTemplate();
        paramsTemplate.setServerName(mcpConfig.getServerName());
        paramsTemplate.setServerUrl(mcpConfig.getServerUrl());
        paramsTemplate.setTransportType(mcpConfig.getTransportType());
        paramsTemplate.setCommand(mcpConfig.getCommand());
        paramsTemplate.setToolName(mcpConfig.getToolName());
        paramsTemplate.setConnectionTimeout(mcpConfig.getConnectionTimeout());
        paramsTemplate.setMaxRetries(mcpConfig.getMaxRetries());

        // 解析环境变量
        if (StringUtils.isNotBlank(mcpConfig.getEnvVars())) {
            try {
                List<String> envVars = JSON.parseArray(mcpConfig.getEnvVars(), String.class);
                paramsTemplate.setEnvVars(envVars);
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        // 解析 inputSchema 为 InputTypeSchema 列表
        List<InputTypeSchema> inputSchemas = parseMcpInputSchema(mcpConfig.getInputSchema());

        return ToolDefinition.<McpParamsTemplate>builder()
                .toolId(mcpConfig.getId())
                .toolName(mcpConfig.getToolDisplayName() != null ? mcpConfig.getToolDisplayName() : mcpConfig.getToolName())
                .toolDesc(mcpConfig.getToolDescription())
                .toolType(ToolTypeEnum.MCP)
                .inputTypeSchemas(inputSchemas)
                .outputTypeSchema(mcpConfig.getOutputSchema())
                .paramsTemplate(paramsTemplate)
                .build();
    }

    /**
     * 解析 MCP inputSchema JSON 为 InputTypeSchema 列表
     */
    private static List<InputTypeSchema> parseMcpInputSchema(String inputSchema) {
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
            return List.of();
        }
    }
}
