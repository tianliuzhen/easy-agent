package com.aaa.easyagent.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 多 Agent 编排请求（含成员节点，全量替换）。
 *
 * @author liuzhen.tian
 */
@Data
@Accessors(chain = true)
public class EaFlowReq {

    /**
     * 主键；为空表示新增
     */
    private Long id;

    private String flowName;

    private String flowKey;

    /**
     * 编排策略：SUPERVISOR/ROUTER/WORKFLOW
     */
    private String strategy;

    /**
     * 主管/路由 Agent ID；SUPERVISOR/ROUTER 使用，WORKFLOW 留空
     */
    private Long supervisorAgentId;

    private String avatar;

    private String flowDesc;

    /**
     * 编排级提示词（主管/路由指令）
     */
    private String prompt;

    private String welcomeMessage;

    /**
     * 成员节点（保存时全量替换）
     */
    private List<EaFlowNodeReq> nodes;
}
