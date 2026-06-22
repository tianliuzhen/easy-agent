package com.aaa.easyagent.core.domain.result;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 多 Agent 编排结果（含成员节点）。
 *
 * @author liuzhen.tian
 */
@Data
public class EaFlowResult {

    private Long id;

    private String flowName;

    private String flowKey;

    /**
     * 编排策略：SUPERVISOR/ROUTER/WORKFLOW
     */
    private String strategy;

    /**
     * 主管/路由 Agent ID
     */
    private Long supervisorAgentId;

    private String avatar;

    private String flowDesc;

    /**
     * 编排级提示词（主管/路由指令）
     */
    private String prompt;

    private String welcomeMessage;

    private Date createdAt;

    private Date updatedAt;

    /**
     * 成员节点（按 orderIndex 升序）
     */
    private List<EaFlowNodeResult> nodes;
}
