package com.aaa.easyagent.core.service;

import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.DO.EaSkillConfigDO;
import com.aaa.easyagent.core.domain.DO.EaSkillRelationDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.result.SkillConfigResult;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.SkillParamsTemplate;
import com.aaa.easyagent.core.mapper.EaSkillConfigDAO;
import com.aaa.easyagent.core.mapper.EaSkillRelationDAO;
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
 * Skill 工具集成服务
 * 负责将 Skill 工具转换为标准 ToolDefinition，以及 Agent 绑定 Skill 工具
 *
 * @author liuzhen.tian
 * @version 1.0 SkillIntegrationService.java  2026/4/6
 */
@Slf4j
@Service
public class SkillIntegrationService {

    @Resource
    private EaSkillConfigDAO eaSkillConfigDAO;

    @Resource
    private EaSkillRelationDAO skillRelationDAO;


    /**
     * 将 Skill 配置转换为 ToolDefinition 列表
     *
     * @param agentId Agent ID
     * @return ToolDefinition 列表
     */
    public List<ToolDefinition<?>> getSkillsForAgent(Long agentId) {
        // 1. 查询 Agent 绑定的 Skill 关系
        Example relationExample = new Example(EaSkillRelationDO.class);
        relationExample.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("isActive", true);
        List<EaSkillRelationDO> relations = skillRelationDAO.selectByExample(relationExample);

        if (relations.isEmpty()) {
            return List.of();
        }

        // 2. 查询对应的 Skill 配置
        List<Long> skillConfigIds = relations.stream()
                .map(EaSkillRelationDO::getSkillConfigId)
                .collect(Collectors.toList());

        Example configExample = new Example(EaSkillConfigDO.class);
        configExample.createCriteria().andIn("id", skillConfigIds);
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectByExample(configExample);

        // 3. 转换为 ToolDefinition
        return configs.stream()
                .map(this::convertToToolDefinition)
                .collect(Collectors.toList());
    }

    /**
     * 将 Skill 配置转换为 ToolDefinition
     */
    public ToolDefinition<SkillParamsTemplate> convertToToolDefinition(EaSkillConfigDO config) {
        // 构建 SkillParamsTemplate
        SkillParamsTemplate paramsTemplate = new SkillParamsTemplate();
        paramsTemplate.setSkillName(config.getSkillName());
        paramsTemplate.setSkillType(config.getSkillType());
        paramsTemplate.setExecutionMode(config.getExecutionMode());
        paramsTemplate.setTimeout(config.getTimeout());
        paramsTemplate.setMaxRetries(config.getMaxRetries());
        paramsTemplate.setSkillConfig(config.getSkillConfig());

        // 解析环境变量
        if (StringUtils.isNotBlank(config.getSkillConfig())) {
            try {
                JSONObject configJson = JSON.parseObject(config.getSkillConfig());
                if (configJson.containsKey("envVars")) {
                    List<String> envVars = configJson.getJSONArray("envVars").toJavaList(String.class);
                    paramsTemplate.setEnvVars(envVars);
                }
            } catch (Exception e) {
                log.warn("解析技能配置失败: {}", config.getSkillConfig());
            }
        }

        // 解析 inputSchema 为 InputTypeSchema 列表
        List<InputTypeSchema> inputSchemas = parseInputSchema(config.getInputSchema());

        return ToolDefinition.<SkillParamsTemplate>builder()
                .toolId(config.getId())
                .toolName(config.getSkillName())
                .toolDesc(config.getSkillDescription())
                .toolType(ToolTypeEnum.SKILL)
                .inputTypeSchemas(inputSchemas)
                .outputTypeSchema(config.getOutputSchema())
                .paramsTemplate(paramsTemplate)
                .build();
    }

    /**
     * 绑定 Skill 到 Agent
     *
     * @param agentId       Agent ID
     * @param skillConfigId Skill 配置 ID
     * @param bindingConfig 绑定配置（可选，用于覆盖默认参数）
     * @return 是否成功
     */
    public boolean bindSkillToAgent(Long agentId, Long skillConfigId, String bindingConfig) {
        // 检查 Skill 配置是否存在
        EaSkillConfigDO config = eaSkillConfigDAO.selectByPrimaryKey(skillConfigId);
        if (config == null) {
            throw new RuntimeException("Skill 配置不存在: id=" + skillConfigId);
        }

        // 检查是否已绑定
        Example example = new Example(EaSkillRelationDO.class);
        example.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("skillConfigId", skillConfigId);
        List<EaSkillRelationDO> existing = skillRelationDAO.selectByExample(example);

        if (!existing.isEmpty()) {
            // 已存在，更新绑定配置
            EaSkillRelationDO relation = existing.get(0);
            relation.setBindingConfig(bindingConfig);
            relation.setUpdatedAt(new Date());
            relation.setIsActive(true);
            return skillRelationDAO.updateByPrimaryKeySelective(relation) > 0;
        }

        // 创建新的绑定关系
        EaSkillRelationDO relation = new EaSkillRelationDO();
        relation.setAgentId(agentId);
        relation.setSkillConfigId(skillConfigId);
        relation.setBindingConfig(bindingConfig);
        relation.setSortOrder(0);
        relation.setIsActive(true);
        relation.setCreatedAt(new Date());
        relation.setUpdatedAt(new Date());

        return skillRelationDAO.insertSelective(relation) > 0;
    }

    /**
     * 解绑 Skill
     *
     * @param agentId       Agent ID
     * @param skillConfigId Skill 配置 ID
     * @return 是否成功
     */
    public boolean unbindSkillFromAgent(Long agentId, Long skillConfigId) {
        Example example = new Example(EaSkillRelationDO.class);
        example.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("skillConfigId", skillConfigId);
        return skillRelationDAO.deleteByExample(example) > 0;
    }

    /**
     * 获取所有 Skill 配置列表
     *
     * @return Skill 配置结果列表
     */
    public List<SkillConfigResult> getAllSkills() {
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectAll();
        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * 获取官方 Skill 配置列表（user_id = 0）
     *
     * @return 官方 Skill 配置结果列表
     */
    public List<SkillConfigResult> getOfficialSkills() {
        Example example = new Example(EaSkillConfigDO.class);
        example.createCriteria().andEqualTo("userId", 0);
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectByExample(example);

        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * 根据分类获取 Skill 列表
     *
     * @param category 技能分类
     * @return Skill 配置结果列表
     */
    public List<SkillConfigResult> getSkillsByCategory(String category) {
        Example example = new Example(EaSkillConfigDO.class);
        example.createCriteria()
                .andEqualTo("skillCategory", category)
                .andEqualTo("status", "active");
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectByExample(example);

        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * 获取 Agent 绑定的 Skill 列表
     *
     * @param agentId Agent ID
     * @return Skill 配置结果列表
     */
    public List<SkillConfigResult> getBoundSkillsByAgentId(Long agentId) {
        // 1. 查询绑定关系
        Example relationExample = new Example(EaSkillRelationDO.class);
        relationExample.createCriteria()
                .andEqualTo("agentId", agentId)
                .andEqualTo("isActive", true);
        List<EaSkillRelationDO> relations = skillRelationDAO.selectByExample(relationExample);

        if (relations.isEmpty()) {
            return List.of();
        }

        // 2. 查询配置
        List<Long> skillConfigIds = relations.stream()
                .map(EaSkillRelationDO::getSkillConfigId)
                .collect(Collectors.toList());

        Example configExample = new Example(EaSkillConfigDO.class);
        configExample.createCriteria().andIn("id", skillConfigIds);
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectByExample(configExample);

        // 3. 转换为 Result
        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * 创建 Skill 配置
     *
     * @param request 配置请求
     * @return 配置 ID
     */
    public Long createSkillConfig(com.aaa.easyagent.core.domain.request.SkillConfigRequest request) {
        EaSkillConfigDO config = new EaSkillConfigDO();
        BeanUtils.copyProperties(request, config);
        config.setUserId(0L); // 默认官方技能
        config.setStatus("active");
        config.setCreatedAt(new Date());
        config.setUpdatedAt(new Date());

        eaSkillConfigDAO.insertSelective(config);
        return config.getId();
    }

    /**
     * 更新 Skill 配置
     *
     * @param id      配置 ID
     * @param request 配置请求
     * @return 是否成功
     */
    public boolean updateSkillConfig(Long id, com.aaa.easyagent.core.domain.request.SkillConfigRequest request) {
        EaSkillConfigDO existing = eaSkillConfigDAO.selectByPrimaryKey(id);
        if (existing == null) {
            throw new RuntimeException("Skill 配置不存在: id=" + id);
        }

        EaSkillConfigDO config = new EaSkillConfigDO();
        config.setId(id);
        BeanUtils.copyProperties(request, config);
        config.setUpdatedAt(new Date());

        return eaSkillConfigDAO.updateByPrimaryKeySelective(config) > 0;
    }

    /**
     * 删除 Skill 配置
     *
     * @param id 配置 ID
     * @return 是否成功
     */
    public boolean deleteSkillConfig(Long id) {
        // 检查是否有绑定关系
        Example relationExample = new Example(EaSkillRelationDO.class);
        relationExample.createCriteria().andEqualTo("skillConfigId", id);
        int relationCount = skillRelationDAO.selectCountByExample(relationExample);

        if (relationCount > 0) {
            throw new RuntimeException("该 Skill 已被绑定到 Agent，无法删除");
        }

        return eaSkillConfigDAO.deleteByPrimaryKey(id) > 0;
    }

    /**
     * 根据 ID 查询 Skill 配置
     *
     * @param id 配置 ID
     * @return 配置信息
     */
    public SkillConfigResult getSkillConfigById(Long id) {
        EaSkillConfigDO config = eaSkillConfigDAO.selectByPrimaryKey(id);
        if (config == null) {
            return null;
        }
        return convertToResult(config);
    }

    // ==================== 用户安装管理 ====================

    /**
     * 用户安装 Skill（从市场安装到"我的 Skill"）
     * 复制官方 Skill 配置，设置 user_id 为当前用户
     *
     * @param userId        用户ID
     * @param skillConfigId 官方 Skill 配置ID
     * @param customConfig  用户自定义配置（JSON格式，可选）
     * @return 新创建的 Skill 配置ID
     */
    public Long installSkill(Long userId, Long skillConfigId, String customConfig) {
        // 1. 检查 Skill 配置是否存在且是官方 Skill
        EaSkillConfigDO officialConfig = eaSkillConfigDAO.selectByPrimaryKey(skillConfigId);
        if (officialConfig == null) {
            throw new RuntimeException("Skill 配置不存在: id=" + skillConfigId);
        }
        if (officialConfig.getUserId() != null && officialConfig.getUserId() != 0) {
            throw new RuntimeException("只能安装官方 Skill");
        }

        // 2. 检查用户是否已安装过（根据 skill_name + user_id 判断）
        Example existingExample = new Example(EaSkillConfigDO.class);
        existingExample.createCriteria()
                .andEqualTo("skillName", officialConfig.getSkillName())
                .andEqualTo("userId", userId);
        int existingCount = eaSkillConfigDAO.selectCountByExample(existingExample);
        if (existingCount > 0) {
            throw new RuntimeException("该 Skill 已安装");
        }

        // 3. 复制官方配置，设置 user_id
        EaSkillConfigDO userConfig = new EaSkillConfigDO();
        BeanUtils.copyProperties(officialConfig, userConfig);
        userConfig.setId(null); // 新记录
        userConfig.setUserId(userId);
        userConfig.setCreatedAt(new Date());
        userConfig.setUpdatedAt(new Date());

        // 4. 合并用户自定义配置
        if (StringUtils.isNotBlank(customConfig)) {
            try {
                JSONObject customJson = JSON.parseObject(customConfig);
                if (customJson.containsKey("skillConfig")) {
                    userConfig.setSkillConfig(customJson.getString("skillConfig"));
                }
                // 可以添加更多自定义字段的覆盖
            } catch (Exception e) {
                log.warn("解析自定义配置失败: {}", customConfig);
            }
        }

        eaSkillConfigDAO.insertSelective(userConfig);
        return userConfig.getId();
    }

    /**
     * 用户卸载 Skill
     * 删除 user_id = 当前用户 且 skill_name = 官方Skill名称 的记录
     *
     * @param userId        用户ID
     * @param skillConfigId 用户安装的 Skill 配置ID
     * @return 是否成功
     */
    public boolean uninstallSkill(Long userId, Long skillConfigId) {
        // 1. 查询要卸载的配置
        EaSkillConfigDO config = eaSkillConfigDAO.selectByPrimaryKey(skillConfigId);
        if (config == null) {
            throw new RuntimeException("Skill 配置不存在: id=" + skillConfigId);
        }

        // 2. 检查是否是当前用户的配置
        if (!userId.equals(config.getUserId())) {
            throw new RuntimeException("只能卸载自己的 Skill");
        }

        // 3. 检查是否有 Agent 绑定关系
        Example relationExample = new Example(EaSkillRelationDO.class);
        relationExample.createCriteria().andEqualTo("skillConfigId", skillConfigId);
        int relationCount = skillRelationDAO.selectCountByExample(relationExample);
        if (relationCount > 0) {
            throw new RuntimeException("该 Skill 已被绑定到 Agent，请先解绑");
        }

        // 4. 删除配置
        return eaSkillConfigDAO.deleteByPrimaryKey(skillConfigId) > 0;
    }

    /**
     * 获取用户已安装的 Skill 列表（我的 Skill）
     *
     * @param userId 用户ID
     * @return Skill 配置结果列表
     */
    public List<SkillConfigResult> getUserInstalledSkills(Long userId) {
        Example example = new Example(EaSkillConfigDO.class);
        example.createCriteria()
                .andEqualTo("userId", userId);
        List<EaSkillConfigDO> configs = eaSkillConfigDAO.selectByExample(example);

        return configs.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否已安装指定 Skill
     *
     * @param userId   用户ID
     * @param skillName Skill 名称
     * @return 是否已安装
     */
    public boolean isSkillInstalled(Long userId, String skillName) {
        Example example = new Example(EaSkillConfigDO.class);
        example.createCriteria()
                .andEqualTo("skillName", skillName)
                .andEqualTo("userId", userId);
        return eaSkillConfigDAO.selectCountByExample(example) > 0;
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
     * 转换为 Result 对象
     */
    private SkillConfigResult convertToResult(EaSkillConfigDO config) {
        SkillConfigResult result = new SkillConfigResult();
        BeanUtils.copyProperties(config, result);

        // 解析 capabilities
        if (StringUtils.isNotBlank(config.getSkillCapabilities())) {
            try {
                List<String> capabilities = JSON.parseArray(config.getSkillCapabilities(), String.class);
                result.setSkillCapabilities(capabilities);
            } catch (Exception e) {
                log.warn("解析 capabilities 失败: {}", config.getSkillCapabilities());
            }
        }

        return result;
    }
}
