package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_chat_message
 * 表注释：聊天消息表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_chat_message")
public class EaChatMessageDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 会话ID
     */
    @Column(name = "conversation_id")
    private Long conversationId;

    /**
     * 消息类型：user_question-用户提问, ai_answer-AI回答, system_thinking-系统思考, system_tool_call-系统工具调用
     */
    @Column(name = "message_type")
    private String messageType;

    /**
     * 使用的模型
     */
    @Column(name = "model_used")
    private String modelUsed;

    /**
     * 消耗的token数
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * 响应时间（毫秒）
     */
    @Column(name = "response_time")
    private Integer responseTime;

    /**
     * 消息序号（用于排序，从1开始）
     */
    @Column(name = "sequence")
    private Integer sequence;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * 消息内容
     */
    @Column(name = "content")
    private String content;

    /**
     * 思考过程日志（单独存储）
     */
    @Column(name = "thinking_log")
    private String thinkingLog;

    /**
     * 工具调用信息（JSON格式）
     */
    @Column(name = "tool_calls")
    private String toolCalls;
}