package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * Skill 解绑请求
 *
 * @author liuzhen.tian
 * @version 1.0 SkillUnbindRequest.java  2026/4/6
 */
@Data
public class SkillUnbindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * Skill 配置 ID
     */
    private Long skillConfigId;
}