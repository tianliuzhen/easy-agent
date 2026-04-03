package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.DO.EaToolRelationDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.request.ToolBindRequest;
import com.aaa.easyagent.core.domain.request.ToolUnbindRequest;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.domain.template.McpParamsTemplate;
import com.aaa.easyagent.core.mapper.EaToolConfigDAO;
import com.aaa.easyagent.core.mapper.EaToolRelationDAO;
import com.aaa.easyagent.core.service.McpToolIntegrationService;
import com.aaa.easyagent.core.service.ToolMangerService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqlsUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolMangerServiceImpl.java  2025/12/27 21:23
 */
@Service
public class ToolMangerServiceImpl implements ToolMangerService {

    @Resource
    private EaToolConfigDAO eaToolConfigDAO;

    @Resource
    private EaToolRelationDAO eaToolRelationDAO;

    @Resource
    private McpToolIntegrationService mcpToolIntegrationService;

    @Override
    public List<EaToolConfigResult> getDefaultTools() {
        List<EaToolConfigDO> selectByWeekendSql = eaToolConfigDAO.selectByExample(
                new Example.Builder(EaToolConfigDO.class)
                        .where(WeekendSqlsUtils.andEqualTo(EaToolConfigDO::getAgentId, 0L))
                        .build());
        return BeanConvertUtil.beanTo(selectByWeekendSql, EaToolConfigResult.class);
    }

    @Override
    public List<EaToolConfigResult> getToolConfigByAgentId(Long agentId) {
        List<EaToolConfigDO> selectByWeekendSql = eaToolConfigDAO.selectByExample(
                new Example.Builder(EaToolConfigDO.class)
                        .where(WeekendSqlsUtils.andEqualTo(EaToolConfigDO::getAgentId, agentId))
                        .build());
        return BeanConvertUtil.beanTo(selectByWeekendSql, EaToolConfigResult.class);
    }

    @Override
    public int saveTool(EaToolConfigReq eaToolConfigReq) {
        if (eaToolConfigReq.getId() == null) {
            return eaToolConfigDAO.insertSelective(eaToolConfigReq);
        }

        return eaToolConfigDAO.updateByPrimaryKeySelective(eaToolConfigReq);
    }

    @Override
    public int delTool(EaToolConfigReq eaToolConfigReq) {
        return eaToolConfigDAO.deleteByPrimaryKey(eaToolConfigReq.getId());
    }

    @Override
    public int copyTool(EaToolConfigReq eaToolConfigReq) {
        return eaToolConfigDAO.insertSelective(eaToolConfigReq);
    }

    @Override
    public EaToolConfigResult debug(EaToolConfigReq eaToolConfigReq) {
        return null;
    }

    @Override
    public List<EaToolConfigResult> listBoundToolsByAgentId(Long agentId) {
        // 1. 查询agentId关联的所有工具关系
        Example relationExample = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, agentId))
                .build();
        List<EaToolRelationDO> relations = eaToolRelationDAO.selectByExample(relationExample);

        // 2. 获取所有关联的工具配置ID
        List<Long> toolConfigIds = relations.stream()
                .map(EaToolRelationDO::getToolConfigId)
                .collect(Collectors.toList());

        List<EaToolConfigResult> results = new ArrayList<>();

        // 3. 查询对应的工具配置（HTTP、SQL 等传统工具）
        if (!toolConfigIds.isEmpty()) {
            Example toolExample = new Example.Builder(EaToolConfigDO.class)
                    .where(WeekendSqlsUtils.andIn(EaToolConfigDO::getId, toolConfigIds))
                    .build();
            List<EaToolConfigDO> toolConfigs = eaToolConfigDAO.selectByExample(toolExample);
            results.addAll(BeanConvertUtil.beanTo(toolConfigs, EaToolConfigResult.class));
        }

        // 4. 查询 MCP 工具
        List<EaMcpConfigDO> mcpConfigs = mcpToolIntegrationService.getBoundMcpConfigsByAgentId(agentId);
        for (EaMcpConfigDO mcpConfig : mcpConfigs) {
            EaToolConfigResult mcpToolResult = convertMcpConfigToResult(mcpConfig);
            results.add(mcpToolResult);
        }

        return results;
    }

    /**
     * 将 MCP 配置转换为 EaToolConfigResult
     */
    private EaToolConfigResult convertMcpConfigToResult(EaMcpConfigDO mcpConfig) {
        EaToolConfigResult result = new EaToolConfigResult();
        result.setId(mcpConfig.getId());
        result.setToolType(ToolTypeEnum.MCP.getType());
        result.setToolInstanceName(mcpConfig.getToolDisplayName() != null ?
                mcpConfig.getToolDisplayName() : mcpConfig.getToolName());
        result.setToolInstanceDesc(mcpConfig.getToolDescription());
        result.setInputTemplate(mcpConfig.getInputSchema());
        result.setOutTemplate(mcpConfig.getOutputSchema());
        result.setToolValue(buildMcpToolValue(mcpConfig));
        result.setIsActive("active".equals(mcpConfig.getStatus()));
        result.setCreatedAt(mcpConfig.getCreatedAt());
        result.setUpdatedAt(mcpConfig.getUpdatedAt());
        return result;
    }

    /**
     * 构建 MCP 工具的 toolValue JSON
     */
    private String buildMcpToolValue(EaMcpConfigDO mcpConfig) {
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

        return JSON.toJSONString(paramsTemplate);
    }

    @Override
    @Transactional
    public int bindTool(ToolBindRequest request) {
        // 检查是否已存在相同的绑定关系
        Example example = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, Long.parseLong(request.getAgentId()))
                .andEqualTo(EaToolRelationDO::getToolConfigId, request.getToolConfigId()))
                .build();

        List<EaToolRelationDO> existingRelations = eaToolRelationDAO.selectByExample(example);
        if (!existingRelations.isEmpty()) {
            // 已存在绑定关系，返回成功
            return 1;
        }

        // 创建新的绑定关系
        EaToolRelationDO relation = new EaToolRelationDO();
        relation.setAgentId(Long.parseLong(request.getAgentId()));
        relation.setToolConfigId(request.getToolConfigId());
        relation.setCreateTime(new Date());
        relation.setUpdateTime(new Date());
        relation.setCreator(request.getCreator());

        return eaToolRelationDAO.insertSelective(relation);
    }

    @Override
    @Transactional
    public int unbindTool(ToolUnbindRequest request) {
        // 删除绑定关系
        Example example = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, Long.parseLong(request.getAgentId()))
                .andEqualTo(EaToolRelationDO::getToolConfigId, request.getToolConfigId()))
                .build();

        return eaToolRelationDAO.deleteByExample(example);
    }
}
