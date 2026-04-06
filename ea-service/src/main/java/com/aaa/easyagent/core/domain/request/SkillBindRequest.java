package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * Skill 绑定请求
 *
 * @author liuzhen.tian
 * @version 1.0 SkillBindRequest.java  2026/4/6
 */
@Data
public class SkillBindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * Skill 配置 ID
     */
    private Long skillConfigId;

    /**
     * 绑定配置（JSON格式，可覆盖默认参数）
     */
    private String bindingConfig;
}