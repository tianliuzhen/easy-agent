package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.request.EaFlowReq;
import com.aaa.easyagent.core.domain.result.EaFlowResult;

import java.util.List;

/**
 * 多 Agent 编排（Flow）后台管理服务：列表 / 详情 / 保存 / 删除。
 *
 * @author liuzhen.tian
 */
public interface FlowAdminService {

    /**
     * 编排列表（不含成员节点）。
     */
    List<EaFlowResult> listFlow();

    /**
     * 编排详情（含有序成员节点）。
     *
     * @param flowId 编排 ID
     */
    EaFlowResult getFlow(Long flowId);

    /**
     * 保存编排（id 为空新增，否则更新），成员节点全量替换。
     *
     * @param req 编排请求
     * @return 编排 ID
     */
    Long saveFlow(EaFlowReq req);

    /**
     * 删除编排及其成员节点。
     *
     * @param flowId 编排 ID
     * @return 是否成功
     */
    boolean deleteFlow(Long flowId);
}
