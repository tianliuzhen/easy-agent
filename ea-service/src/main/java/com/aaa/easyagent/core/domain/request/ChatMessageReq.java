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
        if (this.getModelUsed() == null) {
            this.setModelUsed("");
        }
        if (this.getInputTokensUsed() == null) {
            this.setInputTokensUsed(0L);
        }
        if (this.getOutputTokensUsed() == null) {
            this.setOutputTokensUsed(0L);
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
}
