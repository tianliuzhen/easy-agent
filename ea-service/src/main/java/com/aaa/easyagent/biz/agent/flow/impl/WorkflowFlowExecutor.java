package com.aaa.easyagent.biz.agent.flow.impl;

import com.aaa.easyagent.biz.agent.BaseAgent;
import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.flow.AgentContextFactory;
import com.aaa.easyagent.biz.agent.flow.FlowContext;
import com.aaa.easyagent.biz.agent.flow.FlowExecutor;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;

/**
 * WORKFLOW（流水线）编排：成员 Agent 按 order_index 串行，前者输出作为后者输入。
 * <p>
 * 节点间仅传「上一步纯文本输出」，避免上下文膨胀；子 Agent 由本编排器统一收尾 SSE 与保存会话记录。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowFlowExecutor implements FlowExecutor {

    private final AgentContextFactory agentContextFactory;

    @Override
    public FlowStrategyEnum strategy() {
        return FlowStrategyEnum.WORKFLOW;
    }

    @Override
    public String exec(FlowContext ctx, String question) {
        SseEmitter sse = ctx.getSse();
        List<EaAgentResult> members = ctx.getMembers();
        if (members == null || members.isEmpty()) {
            throw new AgentException("编排无成员 Agent");
        }

        int total = members.size();
        String input = question;
        String finalAnswer = null;
        long accIn = ctx.getInitInputTokens();
        long accOut = ctx.getInitOutputTokens();
        String lastModelVersion = null;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            for (int i = 0; i < total; i++) {
                EaAgentResult agent = members.get(i);
                SseHelper.sendStep(sse, agent.getAgentName(), i + 1, total);

                // 仅首节点透传图片（多模态）
                String imageBase64 = (i == 0) ? ctx.getImageBase64() : null;
                AgentContext nodeCtx = agentContextFactory.build(
                        agent, sse, ctx.getSessionId(), imageBase64, ctx.isSyncMode());

                // 子 Agent 不自行收尾 SSE、不单独保存记录，由编排器统一收口
                BaseAgent executor = buildExecutor(nodeCtx);
                executor.setOwnSse(false);
                executor.setSaveRecord(false);

                String output = executor.exec(input);

                accIn += executor.getAccumulateInputTokenCount();
                accOut += executor.getAccumulateOutputTokenCount();
                if (agent.getAgentModelConfig() != null) {
                    lastModelVersion = agent.getAgentModelConfig().getModelVersion();
                }

                // 仅传上一步纯文本输出作为下一步输入
                input = output;
                finalAnswer = output;
            }
        } finally {
            stopWatch.stop();
        }

        SseHelper.sendFinalAnswer(sse, finalAnswer);

        // 编排统一保存一条会话记录（复用首节点 agent_id，token 汇总各子 Agent）
        ChatRecordSaver.saveAgentFinish(
                new AgentFinish(finalAnswer),
                lastModelVersion,
                accIn, accOut,
                BigDecimal.valueOf(stopWatch.getTotalTimeSeconds()));

        if (sse != null) {
            sse.complete();
        }

        return finalAnswer;
    }

    private BaseAgent buildExecutor(AgentContext nodeCtx) {
        if (nodeCtx.getToolRunMode() == ToolRunMode.ReAct) {
            return new ReActAgentExecutor(nodeCtx);
        }
        return new ToolAgentExecutor(nodeCtx);
    }
}
