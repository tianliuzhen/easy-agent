package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 聊天会话结果对象
 *
 * @author EasyAgent系统
 * @version 1.0 ChatConversationResult.java  2026/2/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatConversationResult extends EaChatConversationDO {
    
    // 扩展字段：关联的Agent名称
    private String agentName;
    
    // 扩展字段：关联的Agent头像
    private String agentAvatar;
    
    // 扩展字段：最后一条消息内容（预览）
    private String lastMessagePreview;
    
    // 扩展字段：消息列表（可选，根据需求加载）
    private List<ChatMessageResult> messages;
    
    // 扩展字段：未读消息数量
    private Integer unreadCount;
    
    // 扩展字段：会话持续时间（分钟）
    private Long durationMinutes;
    
    /**
     * 获取会话持续时间（分钟）
     * 
     * @return 持续时间（分钟）
     */
    public Long getDurationMinutes() {
        if (this.getCreatedAt() != null && this.getLastMessageTime() != null) {
            long durationMs = this.getLastMessageTime().getTime() - this.getCreatedAt().getTime();
            return durationMs / (1000 * 60);
        }
        return 0L;
    }
    
    /**
     * 获取最后消息预览
     * 
     * @return 最后消息预览
     */
    public String getLastMessagePreview() {
        if (lastMessagePreview != null) {
            return lastMessagePreview;
        }
        
        // 如果没有设置预览，可以返回一个默认值
        if (this.getMessageCount() != null && this.getMessageCount() > 0) {
            return "包含 " + this.getMessageCount() + " 条消息";
        }
        return "新会话";
    }
    
    /**
     * 判断会话是否活跃
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return "active".equals(this.getStatus());
    }
    
    /**
     * 判断会话是否已归档
     * 
     * @return 是否已归档
     */
    public boolean isArchived() {
        return "archived".equals(this.getStatus());
    }
    
    /**
     * 判断会话是否已删除
     * 
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return "deleted".equals(this.getStatus());
    }
    
    /**
     * 获取状态显示文本
     * 
     * @return 状态显示文本
     */
    public String getStatusText() {
        if (isActive()) {
            return "活跃";
        } else if (isArchived()) {
            return "已归档";
        } else if (isDeleted()) {
            return "已删除";
        }
        return "未知";
    }
    
    /**
     * 获取格式化后的创建时间
     * 
     * @return 格式化后的创建时间
     */
    public String getFormattedCreatedAt() {
        if (this.getCreatedAt() != null) {
            // 这里可以添加时间格式化逻辑
            return this.getCreatedAt().toString();
        }
        return "";
    }
    
    /**
     * 获取格式化后的最后消息时间
     * 
     * @return 格式化后的最后消息时间
     */
    public String getFormattedLastMessageTime() {
        if (this.getLastMessageTime() != null) {
            // 这里可以添加时间格式化逻辑
            return this.getLastMessageTime().toString();
        }
        return "";
    }
}
