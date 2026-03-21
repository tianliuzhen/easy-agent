package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.request.ToolBindRequest;
import com.aaa.easyagent.core.domain.request.ToolUnbindRequest;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;

import java.util.List;

/**
 * 工具配置 服务
 *
 * @author liuzhen.tian
 * @version 1.0 ToolMangerService.java  2025/12/27 21:23
 */
public interface ToolMangerService {

    /**
     * 查询官方工具列表
     *
     * @return
     */
    List<EaToolConfigResult> getDefaultTools();

    /**
     * 根据智能体ID获取工具配置列表
     *
     * @param agentId 智能体ID，用于查询关联的工具配置
     * @return 工具配置结果列表，包含所有与指定智能体关联的工具配置信息
     */
    List<EaToolConfigResult> getToolConfigByAgentId(Long agentId);

    /**
     * 根据智能体ID获取已绑定的工具列表
     *
     * @param agentId 智能体ID
     * @return 已绑定的工具配置列表
     */
    List<EaToolConfigResult> listBoundToolsByAgentId(Long agentId);


    /**
     * 绑定工具到智能体
     *
     * @param request 工具绑定请求
     * @return 绑定成功返回1，否则返回0
     */
    int bindTool(ToolBindRequest request);

    /**
     * 从智能体解绑工具
     *
     * @param request 工具解绑请求
     * @return 解绑成功返回1，否则返回0
     */
    int unbindTool(ToolUnbindRequest request);

    /**
     * 保存工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象，包含要保存的工具配置信息
     * @return 保存成功返回1，否则返回0
     */
    int saveTool(EaToolConfigReq eaToolConfigReq);

    /**
     * 删除工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象，包含要删除的工具配置信息
     * @return 删除成功返回1，否则返回0
     */
    int delTool(EaToolConfigReq eaToolConfigReq);

    /**
     * 复制工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象，包含要复制的工具配置信息
     * @return 复制成功返回1，否则返回0
     */
    int copyTool(EaToolConfigReq eaToolConfigReq);

    /**
     * 调试工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象，包含要调试的工具配置信息
     * @return 调试结果，包含调试过程中的输出和状态信息
     */
    EaToolConfigResult debug(EaToolConfigReq eaToolConfigReq);
}
