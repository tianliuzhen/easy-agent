package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.domain.result.EaAgentResult;

import java.util.List;

/**
 * 智能体管理服务接口
 *
 * @author liuzhen.tian
 * @version 1.0 AgentService.java  2025/12/27 21:05
 */
public interface AgentManagerService {
    /**
     * 保存智能体信息
     *
     * @param req 智能体请求对象，包含要保存的智能体信息
     * @return 保存成功返回1，否则返回0
     */
    int save(EaAgentReq req);

    /**
     * 查询所有智能体
     *
     * @return 智能体结果列表，包含所有智能体的信息
     */
    List<EaAgentResult> selectAll();

    /**
     * 删除智能体
     *
     * @param req 智能体请求对象，包含要删除的智能体信息
     * @return 删除成功返回1，否则返回0
     */
    int delAgent(EaAgentReq req);
}
