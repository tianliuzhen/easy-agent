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
import com.aaa.easyagent.common.llm.LLmModelSelector;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowNodeDO;
import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ROUTER（路由/分诊）编排：路由 Agent 判断意图后，把整个对话一次性转交给最合适的某个成员 Agent。
 * <p>
 * 路由决策为一次性 LLM 调用（输出成员下标）；选中后由该成员独立完成应答，本编排器统一收尾 SSE 与保存会话记录。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouterFlowExecutor implements FlowExecutor {

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final AgentContextFactory agentContextFactory;
    private final AgentManagerService agentManagerService;

    @Override
    public FlowStrategyEnum strategy() {
        return FlowStrategyEnum.ROUTER;
    }

    @Override
    public String exec(FlowContext ctx, String question) {
        SseEmitter sse = ctx.getSse();
        List<EaAgentResult> members = ctx.getMembers();
        List<EaAgentFlowNodeDO> nodes = ctx.getNodes();
        if (members == null || members.isEmpty()) {
            throw new AgentException("编排无成员 Agent");
        }

        // 1. 路由决策：选出目标成员下标
        int targetIndex = decideTarget(ctx, question);
        EaAgentResult target = members.get(targetIndex);
        String nodeRole = nodes != null && targetIndex < nodes.size() ? nodes.get(targetIndex).getNodeRole() : null;
        SseHelper.sendHandoff(sse, target.getAgentName(), nodeRole);

        // 2. 把整个对话转交给目标成员执行（子 Agent 不自行收尾、不单独保存，由编排器统一收口）
        AgentContext nodeCtx = agentContextFactory.build(
                target, sse, ctx.getSessionId(), ctx.getImageBase64(), ctx.isSyncMode());
        BaseAgent executor = buildExecutor(nodeCtx);
        executor.setOwnSse(false);
        executor.setSaveRecord(false);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String answer;
        try {
            answer = executor.exec(question);
        } finally {
            stopWatch.stop();
        }

        long accIn = ctx.getInitInputTokens() + executor.getAccumulateInputTokenCount();
        long accOut = ctx.getInitOutputTokens() + executor.getAccumulateOutputTokenCount();
        String modelVersion = target.getAgentModelConfig() != null
                ? target.getAgentModelConfig().getModelVersion() : null;

        SseHelper.sendFinalAnswer(sse, answer);
        ChatRecordSaver.saveAgentFinish(
                new AgentFinish(answer), modelVersion, accIn, accOut,
                BigDecimal.valueOf(stopWatch.getTotalTimeSeconds()));

        if (sse != null) {
            sse.complete();
        }
        return answer;
    }

    /**
     * 用路由 Agent 的模型 + 成员清单，让 LLM 输出最合适成员的下标。失败或越界时回退首成员。
     */
    private int decideTarget(FlowContext ctx, String question) {
        List<EaAgentResult> members = ctx.getMembers();
        if (members.size() == 1) {
            return 0;
        }

        // 路由 Agent：优先用编排配置的 supervisorAgentId，缺省回退首成员（仅借用其模型做决策）
        EaAgentResult router = null;
        Long routerId = ctx.getFlow().getSupervisorAgentId();
        if (routerId != null) {
            router = agentManagerService.getAgent(routerId);
        }
        if (router == null) {
            router = members.get(0);
        }

        try {
            AgentContext routerCtx = agentContextFactory.build(router, null, ctx.getSessionId(), null, true);
            ChatModel chatModel = LLmModelSelector.buildChatModel(routerCtx);

            String instruction = StringUtils.defaultIfBlank(ctx.getFlow().getPrompt(),
                    "你是一个路由分诊器。根据用户的问题，从下列成员中选择最合适的一个来处理。");
            String system = instruction
                    + "\n\n可选成员列表（编号从 0 开始）：\n" + buildMembersMenu(ctx)
                    + "\n\n只输出最合适成员的编号数字，不要输出任何其他内容。";

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(system));
            messages.add(new UserMessage(question));

            ChatResponse resp = chatModel.call(new Prompt(messages));
            int idx = parseIndex(ChatResponseUtil.getResStr(resp), members.size());
            SseHelper.sendThink(ctx.getSse(), "路由决策：选择成员 #{} {}", idx, members.get(idx).getAgentName());
            return idx;
        } catch (Exception e) {
            log.error("路由决策失败，回退首成员", e);
            return 0;
        }
    }

    private String buildMembersMenu(FlowContext ctx) {
        List<EaAgentResult> members = ctx.getMembers();
        List<EaAgentFlowNodeDO> nodes = ctx.getNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            EaAgentResult m = members.get(i);
            String role = nodes != null && i < nodes.size() ? nodes.get(i).getNodeRole() : null;
            String desc = StringUtils.defaultIfBlank(role, m.getAgentDesc());
            sb.append(i).append(". ").append(m.getAgentName());
            if (StringUtils.isNotBlank(desc)) {
                sb.append("：").append(desc);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private int parseIndex(String text, int size) {
        if (text != null) {
            Matcher matcher = DIGITS.matcher(text);
            if (matcher.find()) {
                int idx = Integer.parseInt(matcher.group());
                if (idx >= 0 && idx < size) {
                    return idx;
                }
            }
        }
        return 0;
    }

    private BaseAgent buildExecutor(AgentContext nodeCtx) {
        if (nodeCtx.getToolRunMode() == ToolRunMode.ReAct) {
            return new ReActAgentExecutor(nodeCtx);
        }
        return new ToolAgentExecutor(nodeCtx);
    }
}
