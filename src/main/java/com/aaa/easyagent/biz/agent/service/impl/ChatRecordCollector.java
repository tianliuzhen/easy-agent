package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.common.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天记录收集器
 * 用于在Agent执行过程中收集思考过程和工具调用信息
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordCollector.java  2026/2/10
 */
@Slf4j
public class ChatRecordCollector {

    // 存储当前会话的上下文信息
    private final AgentContext agentContext;
    // 存储当前会话的用户提问
    private final String userQuestion;
    // 存储当前会话的开始时间
    private final long startTime;
    // 会话ID（由外部设置）
    private Long conversationId;

    public ChatRecordCollector(AgentContext agentContext, String userQuestion) {
        this.agentContext = agentContext;
        this.userQuestion = userQuestion;
        this.startTime = System.currentTimeMillis();
        conversationId = ChatRecordSaver.getCurrentConversationId();
    }

    /**
     * 添加思考过程日志
     *
     * @param thinkingLog 思考过程日志
     */
    public void addThinkingLog(String thinkingLog) {
        if (thinkingLog != null && !thinkingLog.trim().isEmpty()) {
            try {
                ChatRecordSaver chatRecordSaver =
                        SpringContextUtil.getBean(ChatRecordSaver.class);
                if (chatRecordSaver != null) {
                    chatRecordSaver.addThinkingLog(thinkingLog);
                    log.debug("收集思考过程日志: {}", thinkingLog);
                }
            } catch (Exception e) {
                log.warn("添加思考过程日志失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 添加工具调用信息（从FunctionUseAction）
     *
     * @param functionUseAction 工具调用动作
     */
    public void addToolCall(FunctionUseAction functionUseAction) {
        if (functionUseAction != null) {
            try {
                ChatRecordSaver chatRecordSaver =
                        SpringContextUtil.getBean(ChatRecordSaver.class);
                if (chatRecordSaver != null) {
                    chatRecordSaver.addToolCall(functionUseAction);
                    log.debug("收集工具调用信息: {} - {}",
                            functionUseAction.getAction(),
                            functionUseAction.getActionInput());
                }
            } catch (Exception e) {
                log.warn("添加工具调用信息失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 保存Agent完成结果
     *
     * @param agentFinish Agent完成结果
     * @return 是否保存成功
     */
    public boolean saveAgentFinish(AgentFinish agentFinish) {
        if (agentFinish == null) {
            log.warn("Agent完成结果为空，无法保存");
            return false;
        }

        String aiAnswer = agentFinish.getResult();
        if (aiAnswer == null || aiAnswer.trim().isEmpty()) {
            aiAnswer = agentFinish.getLlmResponse();
        }

        return saveChatRecord(aiAnswer);
    }

    /**
     * 保存聊天记录
     *
     * @param aiAnswer AI回答
     * @return 是否保存成功
     */
    public boolean saveChatRecord(String aiAnswer) {
        try {
            if (conversationId == null) {
                log.warn("会话ID为空，无法保存聊天记录");
                return false;
            }

            // 计算响应时间
            long responseTime = System.currentTimeMillis() - startTime;

            // 获取模型信息
            String modelUsed = agentContext.getModelType() != null ?
                    agentContext.getModelType().name() : "unknown";

            // 估算token使用量（简单的估算：每4个字符大约1个token）
            int tokensUsed = estimateTokensUsed(aiAnswer);

            // 通过SpringContextUtil获取ChatRecordSaverService
            try {
                ChatRecordSaver chatRecordSaver =
                        SpringContextUtil.getBean(ChatRecordSaver.class);
                if (chatRecordSaver != null) {
                    // 调用saveChatInteraction方法
                    chatRecordSaver.saveChatInteraction(
                            aiAnswer, modelUsed, tokensUsed, (int) responseTime);

                    log.info("保存聊天记录成功，会话ID: {}, 响应时间: {}ms", conversationId, responseTime);
                    return true;
                } else {
                    log.warn("未找到ChatRecordSaverService Bean");
                    return false;
                }
            } catch (Exception e) {
                log.error("调用ChatRecordSaverService失败", e);
                return false;
            }
        } catch (Exception e) {
            log.error("保存聊天记录失败", e);
            return false;
        }
    }

    /**
     * 设置会话ID
     *
     * @param conversationId 会话ID
     */
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 获取会话ID
     *
     * @return 会话ID
     */
    public Long getConversationId() {
        return conversationId;
    }

    /**
     * 估算token使用量
     *
     * @param aiAnswer AI回答
     * @return 估算的token使用量
     */
    private int estimateTokensUsed(String aiAnswer) {
        if (aiAnswer == null) {
            return 0;
        }

        // 简单的估算：每4个字符大约1个token
        return aiAnswer.length() > 0 ? aiAnswer.length() / 4 : 0;
    }
}
