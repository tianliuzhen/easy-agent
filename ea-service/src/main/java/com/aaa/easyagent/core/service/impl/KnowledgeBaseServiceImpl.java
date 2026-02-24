package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.mapper.EaKnowledgeBaseDAO;
import com.aaa.easyagent.core.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库管理服务实现类 - DAO层
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseServiceImpl.java  2026/2/1 0:00
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private  EaKnowledgeBaseDAO knowledgeBaseDAO;

    @Override
    public void saveKnowledgeBase(EaKnowledgeBaseDO knowledgeBase) {
        try {
            knowledgeBaseDAO.insertSelective(knowledgeBase);
            log.info("知识库记录保存成功: {}", knowledgeBase.getFileName());
        } catch (Exception e) {
            log.error("保存知识库记录失败", e);
            throw new RuntimeException("保存知识库记录失败: " + e.getMessage());
        }
    }

    @Override
    public EaKnowledgeBaseDO getKnowledgeBaseById(Long id) {
        try {
            return knowledgeBaseDAO.selectByPrimaryKey(id);
        } catch (Exception e) {
            log.error("根据ID获取知识库记录失败: {}", id, e);
            throw new RuntimeException("获取知识库记录失败: " + e.getMessage());
        }
    }

    @Override
    public void updateKnowledgeBase(EaKnowledgeBaseDO knowledgeBase) {
        try {
            knowledgeBaseDAO.updateByPrimaryKeySelective(knowledgeBase);
            log.info("知识库记录更新成功: {}", knowledgeBase.getFileName());
        } catch (Exception e) {
            log.error("更新知识库记录失败", e);
            throw new RuntimeException("更新知识库记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<EaKnowledgeBaseDO> listKnowledgeBase() {
        try {
            EaKnowledgeBaseDO query = new EaKnowledgeBaseDO();
            query.setStatus((byte) 1);
            return knowledgeBaseDAO.select(query);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            throw new RuntimeException("查询知识库列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<EaKnowledgeBaseDO> listKnowledgeBaseByAgentId(String agentId) {
        try {
            EaKnowledgeBaseDO query = new EaKnowledgeBaseDO();
            query.setStatus((byte) 1);
            query.setAgentId(Long.valueOf(agentId));
            return knowledgeBaseDAO.select(query);
        } catch (Exception e) {
            log.error("根据Agent ID查询知识库列表失败", e);
            throw new RuntimeException("查询知识库列表失败: " + e.getMessage());
        }
    }
}