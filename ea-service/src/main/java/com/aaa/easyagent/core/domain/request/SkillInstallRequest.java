package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * Skill 安装请求
 */
@Data
public class SkillInstallRequest {

    /**
     * Skill 配置 ID（官方 Skill）
     */
    private Long skillConfigId;

    /**
     * 用户自定义配置（JSON 格式，可选）
     */
    private String customConfig;
}
