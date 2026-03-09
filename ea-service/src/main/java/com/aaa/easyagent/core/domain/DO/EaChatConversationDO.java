package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_chat_conversation
 * 表注释：聊天会话表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_chat_conversation")
public class EaChatConversationDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 会话标题（取第一次问题的前50个字符）
     */
    @Column(name = "title")
    private String title;

    @Column(name = "session_id")
    private String sessionId;

    /**
     * 关联的Agent ID
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * 用户ID（预留字段，未来扩展用）
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 消息总数
     */
    @Column(name = "message_count")
    private Integer messageCount;

    /**
     * 最后消息时间
     */
    @Column(name = "last_message_time")
    private Date lastMessageTime;

    /**
     * 会话状态：active-活跃, archived-已归档, deleted-已删除
     */
    @Column(name = "status")
    private String status;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;
}