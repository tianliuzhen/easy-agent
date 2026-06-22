package com.aaa.easyagent.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 编排成员节点请求。
 *
 * @author liuzhen.tian
 */
@Data
@Accessors(chain = true)
public class EaFlowNodeReq {

    private Long id;

    /**
     * 成员 Agent ID
     */
    private Long agentId;

    /**
     * 节点角色描述（供主管选工具 / 路由分类用）
     */
    private String nodeRole;

    /**
     * 执行顺序：WORKFLOW 按此串行
     */
    private Integer orderIndex;
}
