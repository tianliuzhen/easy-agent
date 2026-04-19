package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseQueryRequest;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseBindRequest;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseUnbindRequest;

import java.util.List;

/**
 * 知识库管理服务接口 - 业务层
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseService.java  2026/2/1 0:00
 */
public interface KnowledgeBaseService {

    /**
     * 保存知识库记录
     *
     * @param knowledgeBase 知识库记录
     */
    void saveKnowledgeBase(EaKnowledgeBaseDO knowledgeBase);

    /**
     * 根据ID获取知识库记录
     *
     * @param id 知识库ID
     * @return 知识库记录
     */
    EaKnowledgeBaseDO getKnowledgeBaseById(Long id);

    /**
     * 更新知识库记录
     *
     * @param knowledgeBase 知识库记录
     */
    void updateKnowledgeBase(EaKnowledgeBaseDO knowledgeBase);

    /**
     * 查询知识库列表
     *
     * @return 知识库列表
     */
    List<EaKnowledgeBaseDO> listKnowledgeBase();

    /**
     * 根据Agent ID查询知识库列表
     *
     * @param agentId Agent ID
     * @return 知识库列表
     */
    List<EaKnowledgeBaseDO> listKnowledgeBaseByAgentId(String agentId);

    /**
     * 根据查询条件查询知识库列表（使用Request对象）
     *
     * @param request 查询请求
     * @return 知识库列表
     */
    List<EaKnowledgeBaseDO> listKnowledgeBaseByCondition(KnowledgeBaseQueryRequest request);

    /**
     * 绑定知识库到Agent
     *
     * @param request 绑定请求
     */
    void bindKnowledgeBaseToAgent(KnowledgeBaseBindRequest request);

    /**
     * 从Agent解绑知识库
     *
     * @param request 解绑请求
     */
    void unbindKnowledgeBaseFromAgent(KnowledgeBaseUnbindRequest request);
}
