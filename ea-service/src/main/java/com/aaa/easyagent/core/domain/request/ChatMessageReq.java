package com.aaa.easyagent.core.domain.request;

import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;

import java.math.BigDecimal;

/**
 * 聊天消息请求对象
 *
 * @author EasyAgent系统
 * @version 1.0 ChatMessageReq.java  2026/2/10
 */
public class ChatMessageReq extends EaChatMessageDO {

    /**
     * 验证请求参数是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return this.getConversationId() != null && this.getConversationId() > 0
                && this.getQuestion() != null;
    }

    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (this.getSequence() == null) {
            this.setSequence(1);
        }
        if (this.getThinkingLog() == null) {
            this.setThinkingLog("");
        }
        if (this.getToolCalls() == null) {
            this.setToolCalls("");
        }
        if (this.getModelUsed() == null) {
            this.setModelUsed("");
        }
        if (this.getTokensUsed() == null) {
            this.setTokensUsed(0);
        }
        if (this.getResponseTime() == null) {
            this.setResponseTime(BigDecimal.ZERO);
        }
    }




    /**
     * 创建用户提问消息（仅问题，无回答）
     *
     * @param conversationId 会话 ID
     * @param question 用户问题
     * @param sequence 消息序号
     * @return 用户提问消息请求对象
     */
    public static ChatMessageReq createUserQuestion(Long conversationId, String question, Integer sequence) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setQuestion(question);
        req.setAnswer("");
        req.setSequence(sequence);
        req.setDefaults();
        return req;
    }

    /**
     * 创建 AI 回答消息（包含问题和回答）
     *
     * @param conversationId 会话 ID
     * @param question 用户问题
     * @param answer AI 回答
     * @param sequence 消息序号
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的 token 数
     * @param responseTime 响应时间
     * @return AI 回答消息请求对象
     */
    public static ChatMessageReq createAiAnswer(Long conversationId, String question, String answer, Integer sequence,
                                                String modelUsed, Integer tokensUsed, Integer responseTime) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setQuestion(question);
        req.setAnswer(answer);
        req.setSequence(sequence);
        req.setModelUsed(modelUsed);
        req.setTokensUsed(tokensUsed);
        req.setResponseTime(responseTime != null ? BigDecimal.valueOf(responseTime) : null);
        req.setDefaults();
        return req;
    }

    /**
     * 创建 AI 回答消息（包含思考过程）
     *
     * @param conversationId 会话 ID
     * @param question 用户问题
     * @param answer AI 回答
     * @param thinkingLog 思考过程日志
     * @param sequence 消息序号
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的 token 数
     * @param responseTime 响应时间
     * @return AI 回答消息请求对象
     */
    public static ChatMessageReq createAiAnswerWithThinking(Long conversationId, String question, String answer,
                                                            String thinkingLog, Integer sequence,
                                                            String modelUsed, Integer tokensUsed, Integer responseTime) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setQuestion(question);
        req.setAnswer(answer);
        req.setThinkingLog(thinkingLog);
        req.setSequence(sequence);
        req.setModelUsed(modelUsed);
        req.setTokensUsed(tokensUsed);
        req.setResponseTime(responseTime != null ? BigDecimal.valueOf(responseTime) : null);
        req.setDefaults();
        return req;
    }

    /**
     * 创建 AI 回答消息（包含工具调用）
     *
     * @param conversationId 会话 ID
     * @param question 用户问题
     * @param answer AI 回答
     * @param toolCalls 工具调用信息（JSON 格式）
     * @param sequence 消息序号
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的 token 数
     * @param responseTime 响应时间
     * @return AI 回答消息请求对象
     */
    public static ChatMessageReq createAiAnswerWithToolCalls(Long conversationId, String question, String answer,
                                                             String toolCalls, Integer sequence,
                                                             String modelUsed, Integer tokensUsed, Integer responseTime) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setQuestion(question);
        req.setAnswer(answer);
        req.setToolCalls(toolCalls);
        req.setSequence(sequence);
        req.setModelUsed(modelUsed);
        req.setTokensUsed(tokensUsed);
        req.setResponseTime(responseTime != null ? BigDecimal.valueOf(responseTime) : null);
        req.setDefaults();
        return req;
    }

    /**
     * 创建 AI 回答消息（完整信息：思考 + 工具 + 回答）
     *
     * @param conversationId 会话 ID
     * @param question 用户问题
     * @param answer AI 回答
     * @param answer AI 回答
     * @param messageContext 聊天消息上下文
     * @param sequence 消息序号
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的 token 数
     * @param responseTime 响应时间
     * @return AI 回答消息请求对象
     */
    public static ChatMessageReq createFullAiAnswer(Long conversationId, Long messageId, String question, String answer,
                                                    String messageContext, Integer sequence,
                                                    String modelUsed, Integer tokensUsed, BigDecimal responseTime) {
        ChatMessageReq req = new ChatMessageReq();
        req.setId(messageId);
        req.setConversationId(conversationId);
        req.setQuestion(question);
        req.setAnswer(answer);
        req.setMessageContext(messageContext);
        req.setSequence(sequence);
        req.setModelUsed(modelUsed);
        req.setTokensUsed(tokensUsed);
        req.setResponseTime(responseTime);
        req.setDefaults();
        return req;
    }
}
