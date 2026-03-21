package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.domain.DO.EaKnowledgeRelationDO;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseBindRequest;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseQueryRequest;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseUnbindRequest;
import com.aaa.easyagent.core.mapper.EaKnowledgeBaseDAO;
import com.aaa.easyagent.core.mapper.EaKnowledgeRelationDAO;
import com.aaa.easyagent.core.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

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
    private EaKnowledgeBaseDAO knowledgeBaseDAO;

    @Resource
    private EaKnowledgeRelationDAO knowledgeRelationDAO;

    @Override
    public void saveKnowledgeBase(EaKnowledgeBaseDO knowledgeBase) {
        try {
            knowledgeBase.setCreator(UserContextHolder.getUserId());
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
            query.setCreator(UserContextHolder.getUserId());
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
            // 先通过关联表查询知识库 ID 列表
            EaKnowledgeRelationDO relationQuery = new EaKnowledgeRelationDO();
            relationQuery.setAgentId(Long.valueOf(agentId));
            List<EaKnowledgeRelationDO> relations = knowledgeRelationDAO.select(relationQuery);

            if (relations.isEmpty()) {
                return List.of();
            }

            // 根据知识库 ID 查询知识库详情
            List<Long> knowledgeBaseIds = relations.stream()
                    .map(EaKnowledgeRelationDO::getKnowledgeBaseId)
                    .toList();

            Example example = new Example(EaKnowledgeBaseDO.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("status", (byte) 1);
            criteria.andIn("id", knowledgeBaseIds);

            return knowledgeBaseDAO.selectByExample(example);
        } catch (Exception e) {
            log.error("根据 Agent ID 查询知识库列表失败", e);
            throw new RuntimeException("查询知识库列表失败：" + e.getMessage());
        }
    }

    @Override
    public List<EaKnowledgeBaseDO> listKnowledgeBaseByCondition(KnowledgeBaseQueryRequest request) {
        try {
            EaKnowledgeBaseDO query = new EaKnowledgeBaseDO();
            query.setStatus((byte) 1);

            if (request.getAgentId() != null && !request.getAgentId().isEmpty()) {
                query.setAgentId(Long.valueOf(request.getAgentId()));
            }

            if (request.getKbName() != null && !request.getKbName().isEmpty()) {
                query.setKbName(request.getKbName());
            }

            if (request.getType() != null && !request.getType().isEmpty()) {
                query.setKbType(request.getType());
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                query.setStatus(Byte.valueOf(request.getStatus()));
            }

            return knowledgeBaseDAO.select(query);
        } catch (Exception e) {
            log.error("根据条件查询知识库列表失败", e);
            throw new RuntimeException("查询知识库列表失败: " + e.getMessage());
        }
    }

    @Override
    public void bindKnowledgeBaseToAgent(KnowledgeBaseBindRequest request) {
        try {
            // 检查是否已存在关联
            EaKnowledgeRelationDO existRelation = new EaKnowledgeRelationDO();
            existRelation.setAgentId(Long.valueOf(request.getAgentId()));
            existRelation.setKnowledgeBaseId(request.getKnowledgeBaseId());
            List<EaKnowledgeRelationDO> existing = knowledgeRelationDAO.select(existRelation);

            if (!existing.isEmpty()) {
                log.warn("知识库已关联到Agent: agentId={}, knowledgeBaseId={}",
                        request.getAgentId(), request.getKnowledgeBaseId());
                return;
            }

            // 创建新的关联
            EaKnowledgeRelationDO relation = new EaKnowledgeRelationDO();
            relation.setAgentId(Long.valueOf(request.getAgentId()));
            relation.setKnowledgeBaseId(request.getKnowledgeBaseId());
            relation.setCreator(request.getCreator() != null ? request.getCreator() : UserContextHolder.getUserId());

            knowledgeRelationDAO.insertSelective(relation);
            log.info("知识库绑定成功: agentId={}, knowledgeBaseId={}, kbName={}",
                    request.getAgentId(), request.getKnowledgeBaseId(), request.getKbName());
        } catch (Exception e) {
            log.error("绑定知识库到Agent失败", e);
            throw new RuntimeException("绑定知识库失败: " + e.getMessage());
        }
    }

    @Override
    public void unbindKnowledgeBaseFromAgent(KnowledgeBaseUnbindRequest request) {
        try {
            // 查找关联记录
            EaKnowledgeRelationDO query = new EaKnowledgeRelationDO();
            query.setAgentId(Long.valueOf(request.getAgentId()));
            query.setKnowledgeBaseId(request.getKnowledgeBaseId());

            List<EaKnowledgeRelationDO> relations = knowledgeRelationDAO.select(query);

            if (relations.isEmpty()) {
                log.warn("未找到关联记录: agentId={}, knowledgeBaseId={}",
                        request.getAgentId(), request.getKnowledgeBaseId());
                return;
            }

            // 删除关联记录
            for (EaKnowledgeRelationDO relation : relations) {
                knowledgeRelationDAO.deleteByPrimaryKey(relation.getId());
            }

            log.info("知识库解绑成功: agentId={}, knowledgeBaseId={}",
                    request.getAgentId(), request.getKnowledgeBaseId());
        } catch (Exception e) {
            log.error("从Agent解绑知识库失败", e);
            throw new RuntimeException("解绑知识库失败: " + e.getMessage());
        }
    }
}
