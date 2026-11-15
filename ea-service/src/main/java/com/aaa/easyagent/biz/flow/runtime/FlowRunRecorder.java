package com.aaa.easyagent.biz.flow.runtime;

import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowNodeRunDO;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowRunDO;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.mapper.EaAgentFlowNodeRunDAO;
import com.aaa.easyagent.core.mapper.EaAgentFlowRunDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/** Flow 运行审计写入器。审计失败只告警，不中断用户的编排执行。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowRunRecorder {
    private final EaAgentFlowRunDAO flowRunDAO;
    private final EaAgentFlowNodeRunDAO nodeRunDAO;

    public void startFlow(FlowRunContext context) {
        if (flowRunDAO == null) return;
        try {
            EaAgentFlowRunDO run = new EaAgentFlowRunDO().setRunId(context.getRunId())
                    .setFlowId(context.getFlowId()).setRevisionId(context.getRevisionId())
                    .setSessionId(context.getSessionId()).setRunType(context.isDebug() ? "DEBUG" : "CHAT")
                    .setStatus(FlowRunStatus.RUNNING.code()).setInputJson(json(context.getInput()))
                    .setInputTokens(0L).setOutputTokens(0L).setStartedAt(new Date());
            flowRunDAO.insertSelective(run);
        } catch (Exception ex) {
            log.warn("记录 Flow 开始失败，runId={}", context.getRunId(), ex);
        }
    }

    public Long startNode(FlowRunContext context, FlowNodeDefinition node, Long agentId, Object input) {
        if (nodeRunDAO == null) return null;
        try {
            EaAgentFlowNodeRunDO run = new EaAgentFlowNodeRunDO().setRunId(context.getRunId())
                    .setNodeId(node.getId()).setNodeType(node.getType()).setAgentId(agentId)
                    .setStatus(FlowRunStatus.RUNNING.code()).setAttempt(1).setInputJson(json(input))
                    .setInputTokens(0L).setOutputTokens(0L).setStartedAt(new Date());
            nodeRunDAO.insertSelective(run);
            return run.getId();
        } catch (Exception ex) {
            log.warn("记录 Flow 节点开始失败，runId={}, nodeId={}", context.getRunId(), node.getId(), ex);
            return null;
        }
    }

    public void finishNode(Long id, FlowRunStatus status, Object output, long inputTokens,
                           long outputTokens, String error) {
        if (id == null) return;
        if (nodeRunDAO == null) return;
        try {
            nodeRunDAO.updateByPrimaryKeySelective(new EaAgentFlowNodeRunDO().setId(id).setStatus(status.code())
                    .setOutputJson(json(output)).setErrorMessage(error).setInputTokens(inputTokens)
                    .setOutputTokens(outputTokens).setFinishedAt(new Date()));
        } catch (Exception ex) {
            log.warn("记录 Flow 节点结束失败，nodeRunId={}", id, ex);
        }
    }

    public void finishFlow(FlowRunContext context, FlowRunStatus status, Object output, long inputTokens,
                           long outputTokens, String error) {
        if (flowRunDAO == null) return;
        try {
            EaAgentFlowRunDO existing = flowRunDAO.selectOne(new EaAgentFlowRunDO().setRunId(context.getRunId()));
            if (existing == null) return;
            flowRunDAO.updateByPrimaryKeySelective(new EaAgentFlowRunDO().setId(existing.getId()).setStatus(status.code())
                    .setOutputJson(json(output)).setErrorMessage(error).setInputTokens(inputTokens)
                    .setOutputTokens(outputTokens).setFinishedAt(new Date()));
        } catch (Exception ex) {
            log.warn("记录 Flow 结束失败，runId={}", context.getRunId(), ex);
        }
    }

    private String json(Object value) {
        return value == null ? null : JacksonUtil.beanToStr(value);
    }
}
