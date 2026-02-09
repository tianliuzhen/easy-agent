package com.aaa.easyagent.biz.agent.service;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.core.service.ChatRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天记录保存服务
 * 负责在Agent执行过程中保存聊天记录到数据库
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordSaver.java  2026/2/10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecordSaver {

    private final ChatRecordService chatRecordService;

    // 存储当前会话的思考过程
    private static final ThreadLocal<List<String>> thinkingLogs = ThreadLocal.withInitial(ArrayList::new);
    // 存储当前会话的工具调用信息
    private static final ThreadLocal<List<String>> toolCalls = ThreadLocal.withInitial(ArrayList::new);
    // 存储当前会话的上下文信息
    private static final ThreadLocal<AgentContext> currentAgentContext = new ThreadLocal<>();
    // 存储当前会话的用户提问
    private static final ThreadLocal<String> currentUserQuestion = new ThreadLocal<>();
    // 存储当前会话的会话ID
    private static final ThreadLocal<Long> currentConversationId = new ThreadLocal<>();

    /**
     * 开始新的聊天会话
     *
     * @param agentContext Agent上下文
     * @param userQuestion 用户提问
     * @return 会话ID
     */
    public Long startNewConversation(AgentContext agentContext, String userQuestion) {
        try {
            // 清理线程本地变量
            clearThreadLocal();

            // 保存当前上下文
            currentAgentContext.set(agentContext);
            currentUserQuestion.set(userQuestion);

            // 生成用户ID（如果没有用户体系，使用默认值）
            String userId = "anonymous_user";

            // 开始新的会话
            Long conversationId = chatRecordService.startNewConversation(
                    agentContext.getAgentId(),
                    userId,
                    userQuestion
            );

            currentConversationId.set(conversationId);
            log.info("开始新的聊天会话，会话ID: {}, Agent ID: {}, 用户提问: {}",
                    conversationId, agentContext.getAgentId(), userQuestion);

            return conversationId;
        } catch (Exception e) {
            log.error("开始新会话失败", e);
            return null;
        }
    }

    /**
     * 添加思考过程日志
     *
     * @param thinkingLog 思考过程日志
     */
    public void addThinkingLog(String thinkingLog) {
        if (thinkingLog != null && !thinkingLog.trim().isEmpty()) {
            List<String> logs = thinkingLogs.get();
            logs.add(thinkingLog);
            log.debug("添加思考过程日志: {}", thinkingLog);
        }
    }

    /**
     * 添加工具调用信息
     *
     * @param toolCall 工具调用信息
     */
    public void addToolCall(String toolCall) {
        if (toolCall != null && !toolCall.trim().isEmpty()) {
            List<String> calls = toolCalls.get();
            calls.add(toolCall);
            log.debug("添加工具调用信息: {}", toolCall);
        }
    }

    /**
     * 添加工具调用信息（从FunctionUseAction）
     *
     * @param functionUseAction 工具调用动作
     */
    public void addToolCall(FunctionUseAction functionUseAction) {
        if (functionUseAction != null) {
            String toolCall = String.format("工具名称: %s, 输入参数: %s",
                    functionUseAction.getAction(),
                    functionUseAction.getActionInput());
            addToolCall(toolCall);
        }
    }

    /**
     * 保存完整的聊天交互
     *
     * @param aiAnswer AI回答
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的token数
     * @param responseTime 响应时间（毫秒）
     * @return 保存的消息ID列表
     */
    @Transactional
    public List<Long> saveChatInteraction(String aiAnswer, String modelUsed,
                                          Integer tokensUsed, Integer responseTime) {
        try {
            Long conversationId = currentConversationId.get();
            String userQuestion = currentUserQuestion.get();

            if (conversationId == null || userQuestion == null) {
                log.warn("无法保存聊天交互，会话ID或用户提问为空");
                return new ArrayList<>();
            }

            // 合并思考过程日志
            String thinkingLog = String.join("\n", thinkingLogs.get());

            // 合并工具调用信息
            String toolCallsJson = "[" + String.join(", ", toolCalls.get()) + "]";

            // 保存聊天交互
            List<Long> messageIds = chatRecordService.saveChatInteraction(
                    conversationId,
                    userQuestion,
                    aiAnswer,
                    thinkingLog,
                    toolCallsJson,
                    modelUsed,
                    tokensUsed,
                    responseTime
            );

            log.info("保存聊天交互成功，会话ID: {}, 消息数量: {}", conversationId, messageIds.size());

            // 清理线程本地变量（保留会话ID，因为可能还有后续交互）
            thinkingLogs.get().clear();
            toolCalls.get().clear();
            currentUserQuestion.remove();

            return messageIds;
        } catch (Exception e) {
            log.error("保存聊天交互失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存Agent完成结果
     *
     * @param agentFinish Agent完成结果
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的token数
     * @param responseTime 响应时间（毫秒）
     * @return 保存的消息ID列表
     */
    @Transactional
    public List<Long> saveAgentFinish(AgentFinish agentFinish, String modelUsed,
                                      Integer tokensUsed, Integer responseTime) {
        if (agentFinish == null) {
            log.warn("Agent完成结果为空，无法保存");
            return new ArrayList<>();
        }

        String aiAnswer = agentFinish.getResult();
        if (aiAnswer == null || aiAnswer.trim().isEmpty()) {
            aiAnswer = agentFinish.getLlmResponse();
        }

        return saveChatInteraction(aiAnswer, modelUsed, tokensUsed, responseTime);
    }

    /**
     * 获取当前会话ID
     *
     * @return 当前会话ID
     */
    public static Long getCurrentConversationId() {
        return currentConversationId.get();
    }

    /**
     * 清理线程本地变量
     */
    public void clearThreadLocal() {
        thinkingLogs.get().clear();
        toolCalls.get().clear();
        currentAgentContext.remove();
        currentUserQuestion.remove();
        currentConversationId.remove();
    }

    /**
     * 获取思考过程日志
     *
     * @return 思考过程日志
     */
    public String getThinkingLogs() {
        return String.join("\n", thinkingLogs.get());
    }

    /**
     * 获取工具调用信息
     *
     * @return 工具调用信息
     */
    public String getToolCalls() {
        return "[" + String.join(", ", toolCalls.get()) + "]";
    }
}
