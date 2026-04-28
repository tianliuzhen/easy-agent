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
    private String overflowStrategy;

    /**
     * 上下文窗口策略是否启用
     */
    private Boolean windowStrategyEnabled;

    /**
     * 工具结果修剪保留轮数
     * 保留最近N轮的工具执行结果，范围：1-10，默认2
     */
    private Integer keepRounds;

    /**
     * 工具结果修剪是否启用
     */
    private Boolean toolTrimEnabled;

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
}
