package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;

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
}
