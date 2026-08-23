package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.biz.agent.orchestration.FlowCompiler;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowDO;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowRevisionDO;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowValidationResult;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import com.aaa.easyagent.core.domain.request.FlowCreateRequest;
import com.aaa.easyagent.core.domain.result.FlowDetailResult;
import com.aaa.easyagent.core.mapper.EaAgentFlowDAO;
import com.aaa.easyagent.core.mapper.EaAgentFlowRevisionDAO;
import com.aaa.easyagent.core.service.FlowStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowStudioServiceImpl implements FlowStudioService {
    private final EaAgentFlowDAO flowDAO;
    private final EaAgentFlowRevisionDAO revisionDAO;
    private final FlowCompiler compiler;

    @Override
    public List<FlowDetailResult> list() {
        return flowDAO.selectAll().stream().map(this::toResult).toList();
    }

    @Override
    public FlowDetailResult detail(Long flowId) {
        EaAgentFlowDO flow = requireFlow(flowId);
        FlowDetailResult result = toResult(flow);
        result.setDraft(draft(flowId));
        if (flow.getPublishedRevisionId() != null) {
            EaAgentFlowRevisionDO revision = revisionDAO.selectByPrimaryKey(flow.getPublishedRevisionId());
            if (revision != null) result.setPublishedRevisionNo(revision.getRevisionNo());
        }
        return result;
    }

    @Override
    @Transactional
    public Long create(FlowCreateRequest request) {
        EaAgentFlowDO flow = new EaAgentFlowDO().setFlowName(request.getFlowName())
                .setFlowKey(request.getFlowKey()).setFlowDesc(request.getFlowDesc())
                .setAvatar(request.getAvatar()).setWelcomeMessage(request.getWelcomeMessage())
                .setCreatedBy(UserContextHolder.getUserId()).setCreatedAt(new Date()).setUpdatedAt(new Date());
        flowDAO.insertSelective(flow);
        FlowDefinition definition = new FlowDefinition().setFlowId(flow.getId()).setName(flow.getFlowName());
        saveDraft(flow.getId(), definition);
        return flow.getId();
    }

    @Override
    @Transactional
    public void delete(Long flowId) {
        requireFlow(flowId);
        Example example = new Example(EaAgentFlowRevisionDO.class);
        example.createCriteria().andEqualTo("flowId", flowId);
        revisionDAO.deleteByExample(example);
        flowDAO.deleteByPrimaryKey(flowId);
    }

    @Override
    public FlowDefinition draft(Long flowId) {
        EaAgentFlowDO flow = requireFlow(flowId);
        if (flow.getCurrentDraftId() == null) return new FlowDefinition().setFlowId(flowId).setName(flow.getFlowName());
        EaAgentFlowRevisionDO revision = revisionDAO.selectByPrimaryKey(flow.getCurrentDraftId());
        if (revision == null || revision.getDefinitionJson() == null) return new FlowDefinition().setFlowId(flowId);
        try {
            return JacksonUtil.strToBean(revision.getDefinitionJson(), FlowDefinition.class)
                    .setRevisionId(revision.getId());
        } catch (Exception e) {
            throw new AgentException("Flow 草稿解析失败：" + flowId);
        }
    }

    @Override
    public FlowDefinition published(Long flowId) {
        EaAgentFlowDO flow = requireFlow(flowId);
        if (flow.getPublishedRevisionId() == null) {
            throw new AgentException("Flow 尚未发布：" + flowId);
        }
        return revision(flowId, flow.getPublishedRevisionId());
    }

    @Override
    public FlowDefinition revision(Long flowId, Long revisionId) {
        requireFlow(flowId);
        EaAgentFlowRevisionDO revision = revisionDAO.selectByPrimaryKey(revisionId);
        if (revision == null || !flowId.equals(revision.getFlowId()) || !"PUBLISHED".equals(revision.getStatus())) {
            throw new AgentException("Flow 发布版本不存在：" + flowId + "@" + revisionId);
        }
        try {
            return JacksonUtil.strToBean(revision.getDefinitionJson(), FlowDefinition.class)
                    .setRevisionId(revision.getId());
        } catch (Exception e) {
            throw new AgentException("Flow 发布版本解析失败：" + flowId);
        }
    }

    @Override
    @Transactional
    public void saveDraft(Long flowId, FlowDefinition definition) {
        EaAgentFlowDO flow = requireFlow(flowId);
        definition.setFlowId(flowId).setSchemaVersion(FlowDefinition.CURRENT_SCHEMA_VERSION);
        String json = JacksonUtil.beanToStr(definition);
        String checksum = compiler.checksum(definition);
        Date now = new Date();
        EaAgentFlowRevisionDO revision;
        if (flow.getCurrentDraftId() != null) {
            revision = revisionDAO.selectByPrimaryKey(flow.getCurrentDraftId());
        } else {
            revision = null;
        }
        if (revision == null || "PUBLISHED".equals(revision.getStatus())) {
            revision = new EaAgentFlowRevisionDO().setFlowId(flowId).setRevisionNo(nextRevisionNo(flowId))
                    .setStatus("DRAFT").setSchemaVersion(definition.getSchemaVersion())
                    .setDefinitionJson(json).setChecksum(checksum)
                    .setValidationJson(JacksonUtil.beanToStr(compiler.validate(definition)))
                    .setCreatedAt(now).setCreatedBy(UserContextHolder.getUserId());
            revisionDAO.insertSelective(revision);
            flow.setCurrentDraftId(revision.getId());
        }
        revision.setDefinitionJson(json).setChecksum(checksum).setSchemaVersion(definition.getSchemaVersion())
                .setValidationJson(JacksonUtil.beanToStr(compiler.validate(definition)));
        revisionDAO.updateByPrimaryKeySelective(revision);
        flow.setUpdatedAt(now);
        flowDAO.updateByPrimaryKeySelective(flow);
    }

    @Override
    public FlowValidationResult validate(Long flowId, FlowDefinition definition) {
        requireFlow(flowId);
        definition.setFlowId(flowId);
        return compiler.validate(definition);
    }

    @Override
    @Transactional
    public Long publish(Long flowId) {
        EaAgentFlowDO flow = requireFlow(flowId);
        FlowDefinition definition = draft(flowId);
        FlowValidationResult validation = compiler.validate(definition);
        if (!validation.isValid()) throw new AgentException("Flow 校验失败：" + validation.getIssues());
        EaAgentFlowRevisionDO draft = revisionDAO.selectByPrimaryKey(flow.getCurrentDraftId());
        draft.setStatus("PUBLISHED").setPublishedAt(new Date()).setValidationJson(JacksonUtil.beanToStr(validation));
        revisionDAO.updateByPrimaryKeySelective(draft);
        flow.setPublishedRevisionId(draft.getId()).setUpdatedAt(new Date());
        flowDAO.updateByPrimaryKeySelective(flow);
        return draft.getId();
    }

    @Override
    public List<NodeMetadata> nodeTypes() {
        return compiler.nodeTypes();
    }

    private int nextRevisionNo(Long flowId) {
        Example example = new Example(EaAgentFlowRevisionDO.class);
        example.createCriteria().andEqualTo("flowId", flowId);
        return revisionDAO.selectByExample(example).stream().map(EaAgentFlowRevisionDO::getRevisionNo)
                .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    }

    private EaAgentFlowDO requireFlow(Long flowId) {
        EaAgentFlowDO flow = flowDAO.selectByPrimaryKey(flowId);
        if (flow == null) throw new AgentException("Flow 不存在：" + flowId);
        return flow;
    }

    private FlowDetailResult toResult(EaAgentFlowDO flow) {
        FlowDetailResult result = new FlowDetailResult();
        BeanUtils.copyProperties(flow, result);
        result.setId(flow.getId());
        return result;
    }
}
