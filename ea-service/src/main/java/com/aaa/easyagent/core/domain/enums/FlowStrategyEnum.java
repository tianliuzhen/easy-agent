package com.aaa.easyagent.core.domain.enums;

/**
 * 多 Agent 编排策略。
 * 对应 ea_agent_flow.strategy 字段。
 *
 * @author liuzhen.tian
 */
public enum FlowStrategyEnum {
    /**
     * 主管模式：主管 Agent 把成员 Agent 当工具按需调用
     */
    SUPERVISOR,
    /**
     * 路由模式：路由 Agent 判断意图后把对话转交给某个成员 Agent
     */
    ROUTER,
    /**
     * 流水线模式：成员 Agent 按 order_index 串行，前者输出作为后者输入
     */
    WORKFLOW;

    public static FlowStrategyEnum getByName(String name) {
        for (FlowStrategyEnum value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
