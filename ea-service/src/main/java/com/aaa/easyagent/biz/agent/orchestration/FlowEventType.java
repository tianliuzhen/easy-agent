package com.aaa.easyagent.biz.agent.orchestration;

/** Flow 运行时事件协议。事件名及其对应的客户端 SSE 类型统一在此维护。 */
public enum FlowEventType {
    /** Flow 开始执行。 */
    FLOW_STARTED("flow_started", "flow"),
    /** Flow 全部执行成功。 */
    FLOW_SUCCEEDED("flow_succeeded", "finalAnswer"),
    /** Flow 执行失败。 */
    FLOW_FAILED("flow_failed", "error"),
    /** 节点已满足执行条件，等待调度。 */
    NODE_READY("node_ready", "flow"),
    /** 节点开始执行。 */
    NODE_STARTED("node_started", "flow"),
    /** 节点执行成功。 */
    NODE_SUCCEEDED("node_succeeded", "flow"),
    /** 节点执行失败。 */
    NODE_FAILED("node_failed", "flow"),
    /** 节点因上游分支未激活而跳过。 */
    NODE_SKIPPED("node_skipped", "flow"),
    /** 节点产生流式回答数据。 */
    NODE_DATA("node_data", "data"),
    /** 节点产生思考过程。 */
    NODE_THINK("node_think", "think"),
    /** 节点调用工具。 */
    NODE_TOOL("node_tool", "tool"),
    /** 节点产生普通运行日志。 */
    NODE_LOG("node_log", "flow");

    /** SSE 事件名称，对外协议使用小写下划线格式。 */
    private final String eventName;
    /** 前端通用消息类型，用于选择对应的展示组件。 */
    private final String clientType;

    FlowEventType(String eventName, String clientType) {
        this.eventName = eventName;
        this.clientType = clientType;
    }

    public String eventName() {
        return eventName;
    }

    public String clientType() {
        return clientType;
    }
}
