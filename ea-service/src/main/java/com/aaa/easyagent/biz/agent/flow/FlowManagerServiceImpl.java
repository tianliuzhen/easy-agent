package com.aaa.easyagent.biz.agent.flow;

import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowDO;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowNodeDO;
import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.mapper.EaAgentFlowDAO;
import com.aaa.easyagent.core.mapper.EaAgentFlowNodeDAO;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuzhen.tian
 */
@Service
@RequiredArgsConstructor
public class FlowManagerServiceImpl implements FlowManagerService {

    private final EaAgentFlowDAO flowDAO;
    private final EaAgentFlowNodeDAO nodeDAO;
    private final AgentManagerService agentManagerService;

    @Override
    public FlowContext loadFlow(Long flowId) {
        EaAgentFlowDO flow = flowDAO.selectByPrimaryKey(flowId);
        if (flow == null) {
            throw new AgentException("编排不存在：" + flowId);
        }

        // 成员节点按 order_index 升序
        Example example = new Example(EaAgentFlowNodeDO.class);
        example.createCriteria().andEqualTo("flowId", flowId);
        example.orderBy("orderIndex").asc();
        List<EaAgentFlowNodeDO> nodes = nodeDAO.selectByExample(example);

        List<EaAgentResult> members = new ArrayList<>();
        for (EaAgentFlowNodeDO node : nodes) {
            EaAgentResult agent = agentManagerService.getAgent(node.getAgentId());
            if (agent == null) {
                throw new AgentException("编排成员 Agent 不存在：" + node.getAgentId());
            }
            members.add(agent);
        }

        FlowStrategyEnum strategy = FlowStrategyEnum.getByName(flow.getStrategy());
        if (strategy == null) {
            throw new AgentException("未知编排策略：" + flow.getStrategy());
        }

        return new FlowContext()
                .setFlow(flow)
                .setNodes(nodes)
                .setMembers(members)
                .setStrategy(strategy);
    }
}
