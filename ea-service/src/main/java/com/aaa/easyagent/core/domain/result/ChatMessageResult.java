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

    /** 扩展字段：本条消息的执行轨迹，按 seq 升序；思考、工具调用、回答片段 */
    private java.util.List<ChatTraceResult> traces = new java.util.ArrayList<>();




    /**
     * 获取发送者类型
     *
     * @return 发送者类型
     */
    public String getSenderType() {
        if (senderType != null) {
           return senderType;
        }

        // 根据问题和回答判断发送者类型
        String question = this.getQuestion();
        String answer = this.getAnswer();

        // 如果有问题且有回答，说明是 AI 响应消息
        if (question != null && !question.isEmpty() && answer != null && !answer.isEmpty()) {
           return "ai";
        }
        // 如果只有问题没有回答，说明是用户消息（历史数据兼容）
        else if (question != null && !question.isEmpty()) {
           return "user";
        }
        // 既无提问也无回答：只有执行轨迹的系统记录
        else if (!traces.isEmpty()) {
           return "system";
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
     * 获取性能指标文本
     *
     * @return 性能指标文本
     */
    public String getPerformanceMetrics() {
        StringBuilder sb = new StringBuilder();

        long inputTokens = this.getInputTokensUsed() == null ? 0L : this.getInputTokensUsed();
        long outputTokens = this.getOutputTokensUsed() == null ? 0L : this.getOutputTokensUsed();
        if (inputTokens + outputTokens > 0) {
            sb.append("Tokens: ").append(inputTokens + outputTokens)
                    .append("（入 ").append(inputTokens).append(" / 出 ").append(outputTokens).append("）");
        }

        if (this.getResponseTime() != null) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("响应: ").append(this.getResponseTime()).append("s");
        }

        if (this.getModelUsed() != null && !this.getModelUsed().isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("模型: ").append(this.getModelUsed());
        }

        return sb.toString();
    }
}
