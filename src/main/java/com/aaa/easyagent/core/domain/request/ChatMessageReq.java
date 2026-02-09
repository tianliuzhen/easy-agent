package com.aaa.easyagent.core.domain.request;

import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;

/**
 * 聊天消息请求对象
 *
 * @author EasyAgent系统
 * @version 1.0 ChatMessageReq.java  2026/2/10
 */
public class ChatMessageReq extends EaChatMessageDO {
    
    // 消息类型常量
    public static final String TYPE_USER_QUESTION = "user_question";
    public static final String TYPE_AI_ANSWER = "ai_answer";
    public static final String TYPE_SYSTEM_THINKING = "system_thinking";
    public static final String TYPE_SYSTEM_TOOL_CALL = "system_tool_call";
    
    /**
     * 验证请求参数是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return this.getConversationId() != null && this.getConversationId() > 0
                && this.getMessageType() != null && !this.getMessageType().isEmpty()
                && this.getContent() != null;
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
            this.setResponseTime(0);
        }
    }
    
    /**
     * 是否是用户提问
     * 
     * @return 是否是用户提问
     */
    public boolean isUserQuestion() {
        return TYPE_USER_QUESTION.equals(this.getMessageType());
    }
    
    /**
     * 是否是AI回答
     * 
     * @return 是否是AI回答
     */
    public boolean isAiAnswer() {
        return TYPE_AI_ANSWER.equals(this.getMessageType());
    }
    
    /**
     * 是否是系统思考过程
     * 
     * @return 是否是系统思考过程
     */
    public boolean isSystemThinking() {
        return TYPE_SYSTEM_THINKING.equals(this.getMessageType());
    }
    
    /**
     * 是否是系统工具调用
     * 
     * @return 是否是系统工具调用
     */
    public boolean isSystemToolCall() {
        return TYPE_SYSTEM_TOOL_CALL.equals(this.getMessageType());
    }
    
    /**
     * 创建用户提问消息
     * 
     * @param conversationId 会话ID
     * @param content 消息内容
     * @param sequence 消息序号
     * @return 用户提问消息请求对象
     */
    public static ChatMessageReq createUserQuestion(Long conversationId, String content, Integer sequence) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setMessageType(TYPE_USER_QUESTION);
        req.setContent(content);
        req.setSequence(sequence);
        req.setDefaults();
        return req;
    }
    
    /**
     * 创建AI回答消息
     * 
     * @param conversationId 会话ID
     * @param content 消息内容
     * @param sequence 消息序号
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的token数
     * @param responseTime 响应时间
     * @return AI回答消息请求对象
     */
    public static ChatMessageReq createAiAnswer(Long conversationId, String content, Integer sequence,
                                                String modelUsed, Integer tokensUsed, Integer responseTime) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setMessageType(TYPE_AI_ANSWER);
        req.setContent(content);
        req.setSequence(sequence);
        req.setModelUsed(modelUsed);
        req.setTokensUsed(tokensUsed);
        req.setResponseTime(responseTime);
        req.setDefaults();
        return req;
    }
    
    /**
     * 创建系统思考过程消息
     * 
     * @param conversationId 会话ID
     * @param thinkingLog 思考过程日志
     * @param sequence 消息序号
     * @return 系统思考过程消息请求对象
     */
    public static ChatMessageReq createSystemThinking(Long conversationId, String thinkingLog, Integer sequence) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setMessageType(TYPE_SYSTEM_THINKING);
        req.setThinkingLog(thinkingLog);
        req.setSequence(sequence);
        req.setDefaults();
        return req;
    }
    
    /**
     * 创建系统工具调用消息
     * 
     * @param conversationId 会话ID
     * @param toolCalls 工具调用信息（JSON格式）
     * @param sequence 消息序号
     * @return 系统工具调用消息请求对象
     */
    public static ChatMessageReq createSystemToolCall(Long conversationId, String toolCalls, Integer sequence) {
        ChatMessageReq req = new ChatMessageReq();
        req.setConversationId(conversationId);
        req.setMessageType(TYPE_SYSTEM_TOOL_CALL);
        req.setToolCalls(toolCalls);
        req.setSequence(sequence);
        req.setDefaults();
        return req;
    }
}
