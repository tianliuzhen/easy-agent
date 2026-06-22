package com.aaa.easyagent.core.domain.result;

import lombok.Data;

/**
 * 编排成员节点结果。
 *
 * @author liuzhen.tian
 */
@Data
public class EaFlowNodeResult {

    private Long id;

    private Long flowId;

    /**
     * 成员 Agent ID
     */
    private Long agentId;

    /**
     * 成员 Agent 名称（冗余展示用）
     */
    private String agentName;

    /**
     * 成员 Agent 头像（冗余展示用）
     */
    private String avatar;

    /**
     * 节点角色描述
     */
    private String nodeRole;

    /**
     * 执行顺序：WORKFLOW 按此串行
     */
    private Integer orderIndex;
}
