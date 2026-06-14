package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.FunFiledHelper;
import com.aaa.easyagent.core.domain.DO.EaAgentQuickPromptDO;
import com.aaa.easyagent.core.domain.request.QuickPromptItem;
import com.aaa.easyagent.core.domain.request.QuickPromptReq;
import com.aaa.easyagent.core.mapper.EaAgentQuickPromptDAO;
import com.aaa.easyagent.core.service.QuickPromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Agent 浮选提示词配置服务实现
 *
 * @author liuzhen.tian
 */
@Slf4j
@Service
public class QuickPromptServiceImpl implements QuickPromptService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private EaAgentQuickPromptDAO eaAgentQuickPromptDAO;

    @Override
    public List<QuickPromptItem> listByAgentId(Long agentId) {
        if (agentId == null) {
            return new ArrayList<>();
        }

        Example example = new Example(EaAgentQuickPromptDO.class);
        example.createCriteria()
                .andEqualTo(FunFiledHelper.getFieldName(EaAgentQuickPromptDO::getAgentId), agentId)
                .andEqualTo(FunFiledHelper.getFieldName(EaAgentQuickPromptDO::getIsActive), true);
        example.orderBy(FunFiledHelper.getFieldName(EaAgentQuickPromptDO::getSortOrder)).asc();

        List<EaAgentQuickPromptDO> list = eaAgentQuickPromptDAO.selectByExample(example);
        List<QuickPromptItem> result = new ArrayList<>();
        for (EaAgentQuickPromptDO promptDO : list) {
            QuickPromptItem item = new QuickPromptItem();
            item.setId(promptDO.getId());
            item.setLabel(promptDO.getLabel());
            item.setSortOrder(promptDO.getSortOrder());
            item.setQuestions(parseQuestions(promptDO.getQuestions()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(QuickPromptReq req) {
        Long agentId = req.getAgentId();
        if (agentId == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }

        // 全量替换：先删除该 Agent 的旧配置
        Example example = new Example(EaAgentQuickPromptDO.class);
        example.createCriteria()
                .andEqualTo(FunFiledHelper.getFieldName(EaAgentQuickPromptDO::getAgentId), agentId);
        eaAgentQuickPromptDAO.deleteByExample(example);

        List<QuickPromptItem> prompts = req.getPrompts();
        if (prompts == null || prompts.isEmpty()) {
            return;
        }

        Date now = new Date();
        int sortOrder = 0;
        for (QuickPromptItem item : prompts) {
            if (item.getLabel() == null || item.getLabel().trim().isEmpty()) {
                continue;
            }
            EaAgentQuickPromptDO promptDO = new EaAgentQuickPromptDO();
            promptDO.setAgentId(agentId);
            promptDO.setLabel(item.getLabel().trim());
            promptDO.setQuestions(writeQuestions(item.getQuestions()));
            promptDO.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : sortOrder);
            promptDO.setIsActive(true);
            promptDO.setCreatedAt(now);
            promptDO.setUpdatedAt(now);
            eaAgentQuickPromptDAO.insertSelective(promptDO);
            sortOrder++;
        }
    }

    private List<String> parseQuestions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Arrays.asList(objectMapper.readValue(questionsJson, String[].class)));
        } catch (Exception e) {
            log.warn("解析浮选提示词 questions 失败: {}", questionsJson, e);
            return new ArrayList<>();
        }
    }

    private String writeQuestions(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return "[]";
        }
        try {
            List<String> cleaned = new ArrayList<>();
            for (String q : questions) {
                if (q != null && !q.trim().isEmpty()) {
                    cleaned.add(q.trim());
                }
            }
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            log.warn("序列化浮选提示词 questions 失败", e);
            return "[]";
        }
    }
}
