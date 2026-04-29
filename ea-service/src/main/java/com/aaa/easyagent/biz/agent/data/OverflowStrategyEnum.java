package com.aaa.easyagent.biz.agent.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上下文溢出处理策略枚举
 *
 * @author liuzhen.tian
 */
@Getter
@AllArgsConstructor
public enum OverflowStrategyEnum {

    /**
     * 滑动窗口策略，删除最早的对话
     */
    SLIDING("sliding", "滑动窗口"),

    /**
     * 压缩策略，对历史对话进行摘要压缩
     */
    COMPRESSION("compression", "压缩策略");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     *
     * @param code 策略代码
     * @return 对应的枚举值，未找到返回 null
     */
    public static OverflowStrategyEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OverflowStrategyEnum strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return null;
    }
}
