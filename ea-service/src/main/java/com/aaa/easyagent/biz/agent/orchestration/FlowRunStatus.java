package com.aaa.easyagent.biz.agent.orchestration;

/**
 * Flow 运行与节点运行的状态。
 * <p>
 * 落库到 ea_agent_flow_run.status 与 ea_agent_flow_node_run.status，对外协议沿用枚举 code。
 */
public enum FlowRunStatus {
    /** 正在执行 */
    RUNNING("RUNNING"),
    /** 全部执行成功 */
    SUCCEEDED("SUCCEEDED"),
    /** 执行失败（含节点超时） */
    FAILED("FAILED"),
    /** 被取消：客户端断开、整体超时或上游节点失败后的收敛 */
    CANCELLED("CANCELLED"),
    /** 因上游控制分支未激活而跳过 */
    SKIPPED("SKIPPED");

    private final String code;

    FlowRunStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
