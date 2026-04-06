package com.aaa.easyagent.core.domain.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * Skill 工具参数模板
 *
 * @author liuzhen.tian
 * @version 1.0 SkillParamsTemplate.java  2026/4/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillParamsTemplate extends ParamsTemplate {

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能类型
     */
    private String skillType;

    /**
     * 执行模式：sync/async
     */
    private String executionMode;

    /**
     * 执行超时时间（秒）
     */
    private Integer timeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 技能配置（JSON格式）
     */
    private String skillConfig;

    /**
     * 环境变量列表
     */
    private List<String> envVars;

    /**
     * 将环境变量列表转换为 Map
     */
    public Map<String, String> buildEnvVarsMap() {
        if (envVars == null || envVars.isEmpty()) {
            return Map.of();
        }
        Map<String, String> envMap = new java.util.HashMap<>();
        for (String env : envVars) {
            if (env != null && env.contains("=")) {
                String[] parts = env.split("=", 2);
                if (parts.length == 2) {
                    envMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return envMap;
    }
}