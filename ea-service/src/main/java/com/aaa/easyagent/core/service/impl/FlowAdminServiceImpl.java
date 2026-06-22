package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.core.domain.DO.EaAgentFlowDO;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowNodeDO;
import com.aaa.easyagent.core.domain.request.EaFlowNodeReq;
import com.aaa.easyagent.core.domain.request.EaFlowReq;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.domain.result.EaFlowNodeResult;
import com.aaa.easyagent.core.domain.result.EaFlowResult;
import com.aaa.easyagent.core.mapper.EaAgentFlowDAO;
import com.aaa.easyagent.core.mapper.EaAgentFlowNodeDAO;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.service.FlowAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 多 Agent 编排后台管理服务实现。
 *
 * @author liuzhen.tian
 */
@Service
@RequiredArgsConstructor
public class FlowAdminServiceImpl implements FlowAdminService {

    private final EaAgentFlowDAO flowDAO;
    private final EaAgentFlowNodeDAO nodeDAO;
    private final AgentManagerService agentManagerService;

    @Override
    public List<EaFlowResult> listFlow() {
        Example example = new Example(EaAgentFlowDO.class);
        example.orderBy("id").desc();
        List<EaAgentFlowDO> flows = flowDAO.selectByExample(example);
        List<EaFlowResult> results = new ArrayList<>();
        for (EaAgentFlowDO flow : flows) {
            EaFlowResult result = new EaFlowResult();
            BeanUtils.copyProperties(flow, result);
            results.add(result);
        }
        return results;
    }

    @Override
    public EaFlowResult getFlow(Long flowId) {
        EaAgentFlowDO flow = flowDAO.selectByPrimaryKey(flowId);
        if (flow == null) {
            return null;
        }
        EaFlowResult result = new EaFlowResult();
        BeanUtils.copyProperties(flow, result);
        result.setNodes(loadNodeResults(flowId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFlow(EaFlowReq req) {
        Date now = new Date();
        EaAgentFlowDO flow = new EaAgentFlowDO();
        BeanUtils.copyProperties(req, flow);
        flow.setUpdatedAt(now);

        if (req.getId() == null) {
            flow.setCreatedAt(now);
            flowDAO.insertSelective(flow);
        } else {
            flowDAO.updateByPrimaryKeySelective(flow);
        }

        Long flowId = flow.getId();

        // 成员节点全量替换：先删后插
        Example delExample = new Example(EaAgentFlowNodeDO.class);
        delExample.createCriteria().andEqualTo("flowId", flowId);
        nodeDAO.deleteByExample(delExample);

        if (!CollectionUtils.isEmpty(req.getNodes())) {
            int order = 0;
            for (EaFlowNodeReq nodeReq : req.getNodes()) {
                EaAgentFlowNodeDO node = new EaAgentFlowNodeDO();
                node.setFlowId(flowId);
                node.setAgentId(nodeReq.getAgentId());
                node.setNodeRole(nodeReq.getNodeRole());
                node.setOrderIndex(nodeReq.getOrderIndex() != null ? nodeReq.getOrderIndex() : order);
                node.setCreatedAt(now);
                node.setUpdatedAt(now);
                nodeDAO.insertSelective(node);
                order++;
            }
        }

        return flowId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFlow(Long flowId) {
        Example delExample = new Example(EaAgentFlowNodeDO.class);
        delExample.createCriteria().andEqualTo("flowId", flowId);
        nodeDAO.deleteByExample(delExample);
        flowDAO.deleteByPrimaryKey(flowId);
        return true;
    }

    private List<EaFlowNodeResult> loadNodeResults(Long flowId) {
        Example example = new Example(EaAgentFlowNodeDO.class);
        example.createCriteria().andEqualTo("flowId", flowId);
        example.orderBy("orderIndex").asc();
        List<EaAgentFlowNodeDO> nodes = nodeDAO.selectByExample(example);

        List<EaFlowNodeResult> results = new ArrayList<>();
        for (EaAgentFlowNodeDO node : nodes) {
            EaFlowNodeResult nodeResult = new EaFlowNodeResult();
            nodeResult.setId(node.getId());
            nodeResult.setFlowId(node.getFlowId());
            nodeResult.setAgentId(node.getAgentId());
            nodeResult.setNodeRole(node.getNodeRole());
            nodeResult.setOrderIndex(node.getOrderIndex());

            EaAgentResult agent = agentManagerService.getAgent(node.getAgentId());
            if (agent != null) {
                nodeResult.setAgentName(agent.getAgentName());
                nodeResult.setAvatar(agent.getAvatar());
            }
            results.add(nodeResult);
        }
        return results;
    }
}
