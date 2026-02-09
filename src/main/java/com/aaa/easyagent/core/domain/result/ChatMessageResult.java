 package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天消息结果对象
 *
 * @author EasyAgent系统
 * @version 1.0 ChatMessageResult.java  2026/2/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessageResult extends EaChatMessageDO {
    
    // 扩展字段：发送者名称
    private String senderName;
    
    // 扩展字段：发送者头像
    private String senderAvatar;
    
    // 扩展字段：发送者类型（user/ai/system）
    private String senderType;
    
    // 扩展字段：格式化后的时间
    private String formattedTime;
    
    // 扩展字段：是否显示思考过程
    private Boolean showThinkingLog = false;
    
    // 扩展字段：思考过程摘要（前100字符）
    private String thinkingLogSummary;
    
    // 扩展字段：工具调用数量
    private Integer toolCallCount;
    
    /**
     * 获取发送者类型
     * 
     * @return 发送者类型
     */
    public String getSenderType() {
        if (senderType != null) {
            return senderType;
        }
        
        // 根据消息类型推断发送者类型
        String messageType = this.getMessageType();
        if (messageType != null) {
            if (messageType.contains("user")) {
                return "user";
            } else if (messageType.contains("ai")) {
                return "ai";
            } else if (messageType.contains("system")) {
                return "system";
            }
        }
        return "unknown";
    }
    
    /**
     * 获取发送者名称
     * 
     * @return 发送者名称
     */
    public String getSenderName() {
        if (senderName != null) {
            return senderName;
        }
        
        // 根据发送者类型返回默认名称
        String type = getSenderType();
        switch (type) {
            case "user":
                return "用户";
            case "ai":
                return "AI助手";
            case "system":
                return "系统";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取发送者头像
     * 
     * @return 发送者头像
     */
    public String getSenderAvatar() {
        if (senderAvatar != null) {
            return senderAvatar;
        }
        
        // 根据发送者类型返回默认头像
        String type = getSenderType();
        switch (type) {
            case "user":
                return "/avatars/user.png";
            case "ai":
                return "/avatars/ai.png";
            case "system":
                return "/avatars/system.png";
            default:
                return "/avatars/default.png";
        }
    }
    
    /**
     * 是否是用户消息
     * 
     * @return 是否是用户消息
     */
    public boolean isUserMessage() {
        return "user".equals(getSenderType());
    }
    
    /**
     * 是否是AI消息
     * 
     * @return 是否是AI消息
     */
    public boolean isAiMessage() {
        return "ai".equals(getSenderType());
    }
    
    /**
     * 是否是系统消息
     * 
     * @return 是否是系统消息
     */
    public boolean isSystemMessage() {
        return "system".equals(getSenderType());
    }
    
    /**
     * 获取格式化后的时间
     * 
     * @return 格式化后的时间
     */
    public String getFormattedTime() {
        if (formattedTime != null) {
            return formattedTime;
        }
        
        if (this.getCreatedAt() != null) {
            // 这里可以添加时间格式化逻辑
            // 例如：HH:mm 或 yyyy-MM-dd HH:mm:ss
            return this.getCreatedAt().toString();
        }
        return "";
    }
    
    /**
     * 获取思考过程摘要
     * 
     * @return 思考过程摘要
     */
    public String getThinkingLogSummary() {
        if (thinkingLogSummary != null) {
            return thinkingLogSummary;
        }
        
        String log = this.getThinkingLog();
        if (log != null && !log.isEmpty()) {
            if (log.length() > 100) {
                return log.substring(0, 97) + "...";
            }
            return log;
        }
        return "";
    }
    
    /**
     * 获取工具调用数量
     * 
     * @return 工具调用数量
     */
    public Integer getToolCallCount() {
        if (toolCallCount != null) {
            return toolCallCount;
        }
        
        String toolCalls = this.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty() && !toolCalls.equals("[]")) {
            // 简单的JSON数组计数
            try {
                // 计算JSON数组中对象的数量
                int count = 0;
                int depth = 0;
                boolean inObject = false;
                
                for (char c : toolCalls.toCharArray()) {
                    if (c == '{' && depth == 0) {
                        inObject = true;
                        depth++;
                    } else if (c == '}' && depth == 1) {
                        inObject = false;
                        depth--;
                        count++;
                    } else if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                    }
                }
                return count;
            } catch (Exception e) {
                // 解析失败，返回0
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 获取消息内容预览
     * 
     * @param maxLength 最大长度
     * @return 消息内容预览
     */
    public String getContentPreview(int maxLength) {
        String content = this.getContent();
        if (content == null) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 获取消息类型显示文本
     * 
     * @return 消息类型显示文本
     */
    public String getMessageTypeText() {
        String type = this.getMessageType();
        if (type == null) {
            return "未知";
        }
        
        switch (type) {
            case "user_question":
                return "用户提问";
            case "ai_answer":
                return "AI回答";
            case "system_thinking":
                return "系统思考";
            case "system_tool_call":
                return "工具调用";
            default:
                return type;
        }
    }
    
    /**
     * 是否有思考过程
     * 
     * @return 是否有思考过程
     */
    public boolean hasThinkingLog() {
        String log = this.getThinkingLog();
        return log != null && !log.isEmpty();
    }
    
    /**
     * 是否有工具调用
     * 
     * @return 是否有工具调用
     */
    public boolean hasToolCalls() {
        String toolCalls = this.getToolCalls();
        return toolCalls != null && !toolCalls.isEmpty() && !toolCalls.equals("[]");
    }
    
    /**
     * 获取性能指标文本
     * 
     * @return 性能指标文本
     */
    public String getPerformanceMetrics() {
        StringBuilder sb = new StringBuilder();
        
        if (this.getTokensUsed() != null && this.getTokensUsed() > 0) {
            sb.append("Tokens: ").append(this.getTokensUsed());
        }
        
        if (this.getResponseTime() != null && this.getResponseTime() > 0) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("响应: ").append(this.getResponseTime()).append("ms");
        }
        
        if (this.getModelUsed() != null && !this.getModelUsed().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("模型: ").append(this.getModelUsed());
        }
        
        return sb.toString();
    }
}
