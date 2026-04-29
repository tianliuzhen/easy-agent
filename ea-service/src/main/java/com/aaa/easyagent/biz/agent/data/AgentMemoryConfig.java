package com.aaa.easyagent.biz.agent.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent记忆配置模型类
 * 用于封装Agent的记忆相关配置，包括上下文窗口控制、工具结果修剪等参数
 *
 * @author liuzhen.tian
 * @version 1.0 AgentMemoryConfig.java  2026/4/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMemoryConfig {

    /**
     * 上下文轮数限制
     * 控制多轮对话中保留的上下文轮数，范围：1-50
     */
    private Integer roundLimit;

    /**
     * 上下文轮数限制是否启用
     */
    private boolean roundLimitEnabled;

    /**
     * 上下文窗口策略触发阈值
     * 当上下文接近窗口上限时的触发比例，范围：0-1，默认0.82
     */
    private Double triggerThreshold;

    /**
     * 溢出处理策略
     * sliding: 滑动窗口策略，删除最早的对话
     * compression: 压缩策略，对历史对话进行摘要压缩
     */
    private OverflowStrategyEnum overflowStrategy;

    /**
     * 上下文窗口策略是否启用
     */
    private boolean windowStrategyEnabled;

    /**
     * 工具结果修剪保留轮数
     * 保留最近N轮的工具执行结果，范围：1-10，默认2
     */
    private Integer keepRounds;

    /**
     * 工具结果修剪是否启用
     */
    private boolean toolTrimEnabled;

    /**
     * 安全区域消息数（仅压缩策略生效）
     * 不纳入压缩的安全区域消息数量，范围：1-20，默认5
     */
    private Integer safeMessageCount;

    /**
     * 自定义压缩指令（仅压缩策略生效）
     * 将添加到系统压缩Prompt后面，最大长度500字符
     */
    private String customCompressPrompt;

    /**
     * 模型平台配置的最大 Token 数
     * 格式如："32K"、"1M"、"128K"，由 ea_model_platform.max_token 字段传入
     */
    private String maxToken;

    /**
     * 获取窗口限制 token 数（解析 maxToken 字符串）
     * 规则：K = 1024, M = 1048576
     * 示例：32K → 32768, 1M → 1048576, 128K → 131072
     *
     * @return token 数，解析失败或为空时返回 0（表示不限制）
     */
    public long getWindowLimit() {
        if (maxToken == null || maxToken.isBlank()) {
            return 0;
        }
        String trimmed = maxToken.trim().toUpperCase();
        try {
            if (trimmed.endsWith("M")) {
                long value = Long.parseLong(trimmed.replace("M", "").trim());
                return value * 1048576;
            } else if (trimmed.endsWith("K")) {
                long value = Long.parseLong(trimmed.replace("K", "").trim());
                return value * 1024;
            } else {
                return Long.parseLong(trimmed);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
