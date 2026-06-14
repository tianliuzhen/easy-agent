package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.request.QuickPromptItem;
import com.aaa.easyagent.core.domain.request.QuickPromptReq;

import java.util.List;

/**
 * Agent 浮选提示词配置服务
 *
 * @author liuzhen.tian
 */
public interface QuickPromptService {

    /**
     * 查询指定 Agent 的浮选提示词（按 sortOrder 升序）
     */
    List<QuickPromptItem> listByAgentId(Long agentId);

    /**
     * 全量替换保存指定 Agent 的浮选提示词
     */
    void save(QuickPromptReq req);
}
