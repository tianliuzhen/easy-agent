package com.aaa.easyagent.biz.agent.service;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.trace.ChatTraceEntry;
import com.aaa.easyagent.biz.agent.trace.ChatTurn;
import com.aaa.easyagent.biz.agent.trace.ChatTraceRecorder;
import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.core.domain.DO.EaChatMessageTraceDO;
import com.aaa.easyagent.core.domain.result.StartNewConversationResp;
import com.aaa.easyagent.core.mapper.EaChatMessageTraceDAO;
import com.aaa.easyagent.core.service.ChatRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 一轮对话的持久化入口。
 * <p>
 * 取代原先的 {@code ChatRecordSaver}：那套用 static ThreadLocal 累积轨迹、
 * 用 {@code SpringContextUtil} 静态拿 service，既无法注入也无法测试，
 * 并且在 Tool 模式的 reactor 线程上收不到轨迹。
 * 现在轨迹由 {@link ChatTraceRecorder} 实例随 {@code AgentContext} 传递，
 * 本类只负责把它写进库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTurnRecorder {

    private final ChatRecordService chatRecordService;
    private final EaChatMessageTraceDAO traceDAO;

    /**
     * 开启一轮对话。
     * <p>
     * 1. 创建或复用会话（会话ID 取 sessionId）
     * 2. 先把用户提问落库，模型随后失败也不会丢提问
     *
     * @param agentContext 本轮的 Agent 上下文，提供 agentId、flowId、sessionId
     * @param question     用户提问
     * @return 持久化句柄；失败时返回不可用的句柄，不抛出以免阻断对话
     */
    public ChatTurn begin(AgentContext agentContext, String question) {
        try {
            StartNewConversationResp resp = chatRecordService.startNewConversation(
                    agentContext.getAgentId(),
                    agentContext.getFlowId(),
                    agentContext.getFlowRevisionId(),
                    agentContext.getSessionId(),
                    UserContextHolder.getUserId(),
                    question);
            log.info("开启对话，会话ID={}, 消息ID={}, agentId={}",
                    resp.conversationId(), resp.messageId(), agentContext.getAgentId());
            return new ChatTurn(resp.conversationId(), resp.messageId(), question);
        } catch (Exception ex) {
            log.error("开启对话失败，sessionId={}, agentId={}",
                    agentContext.getSessionId(), agentContext.getAgentId(), ex);
            return new ChatTurn(null, null, question);
        }
    }

    /**
     * 完成一轮对话：回填答案、批量落轨迹、累加会话 token。
     * <p>
     * 1. 答案取收集器的口径（FINAL_ANSWER 优先，否则拼接 DATA 片段），
     *    与实时展示保持一致，避免刷新后答案变样
     * 2. 轨迹一次批量写入，不在循环里逐条 insert
     * 3. token 用 SQL 自增累加，不做「查出来加一下再写回」
     *
     * @param turn           对话句柄
     * @param traceRecorder  本轮轨迹收集器
     * @param modelUsed      使用的模型版本
     * @param inputTokens    本轮输入 token
     * @param outputTokens   本轮输出 token
     * @param responseTime   响应耗时，单位秒
     */
    @Transactional
    public void complete(ChatTurn turn, ChatTraceRecorder traceRecorder, String modelUsed,
                         long inputTokens, long outputTokens, BigDecimal responseTime) {
        if (turn == null || !turn.persistable()) {
            log.warn("对话句柄不可用，跳过保存");
            return;
        }
        String answer = traceRecorder == null ? "" : traceRecorder.resolveAnswer();
        chatRecordService.completeChatMessage(turn.conversationId(), turn.messageId(),
                turn.question(), answer, modelUsed, inputTokens, outputTokens, responseTime);
        saveTraces(turn, traceRecorder);
        log.info("保存对话完成，会话ID={}, 消息ID={}, 答案长度={}, 轨迹数={}",
                turn.conversationId(), turn.messageId(), answer.length(),
                traceRecorder == null ? 0 : traceRecorder.snapshot().size());
    }

    /**
     * 批量落轨迹。
     * 轨迹是观测数据，写失败只告警，不让它影响已经回填成功的答案。
     */
    private void saveTraces(ChatTurn turn, ChatTraceRecorder traceRecorder) {
        if (traceRecorder == null || traceRecorder.isEmpty()) {
            return;
        }
        List<ChatTraceEntry> entries = traceRecorder.snapshot();
        List<EaChatMessageTraceDO> records = new ArrayList<>(entries.size());
        int seq = 1;
        for (ChatTraceEntry entry : entries) {
            records.add(toRecord(turn, entry, seq++));
        }
        try {
            traceDAO.batchInsert(records);
        } catch (Exception ex) {
            log.warn("保存对话轨迹失败，会话ID={}, 消息ID={}, 轨迹数={}",
                    turn.conversationId(), turn.messageId(), records.size(), ex);
        }
    }

    private EaChatMessageTraceDO toRecord(ChatTurn turn, ChatTraceEntry entry, int seq) {
        return new EaChatMessageTraceDO()
                .setConversationId(turn.conversationId())
                .setMessageId(turn.messageId())
                .setSeq(seq)
                .setTraceType(entry.getType().getCode())
                .setContent(entry.contentText())
                .setToolName(entry.getToolName())
                .setToolInput(entry.getToolInput())
                .setToolOutput(entry.getToolOutput())
                .setAgentId(entry.getSource().agentId())
                .setAgentName(entry.getSource().agentName())
                .setFlowNodeId(entry.getSource().flowNodeId())
                .setDurationMs(entry.getDurationMs())
                .setCreatedAt(entry.getCreatedAt() == null ? new Date() : entry.getCreatedAt());
    }
}
