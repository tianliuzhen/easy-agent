package com.aaa.easyagent.biz.agent.trace;

import java.util.Objects;

/**
 * 轨迹来源：标明这条思考或工具调用是哪个 Agent、哪个 Flow 节点产生的。
 * <p>
 * 单 Agent 对话时 flowNodeId 为 null；多 Agent 编排时每个节点有自己的来源，
 * 使得一轮对话里多个 Agent 的轨迹既能汇总保存，又能在回放时区分开。
 *
 * @param agentId   Agent ID，可能为 null（纯模板节点等没有 Agent 的场景）
 * @param agentName Agent 名称，用于前端展示
 * @param flowNodeId Flow 节点 ID，非编排场景为 null
 */
public record ChatTraceSource(Long agentId, String agentName, String flowNodeId) {

    /** 无来源信息的占位，用于兜底 */
    public static final ChatTraceSource UNKNOWN = new ChatTraceSource(null, null, null);

    public static ChatTraceSource ofAgent(Long agentId, String agentName) {
        return new ChatTraceSource(agentId, agentName, null);
    }

    public static ChatTraceSource ofFlowNode(Long agentId, String agentName, String flowNodeId) {
        return new ChatTraceSource(agentId, agentName, flowNodeId);
    }

    /** 判断是否同一来源，用于决定连续片段能否合并成一条轨迹 */
    public boolean sameAs(ChatTraceSource other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(agentId, other.agentId)
                && Objects.equals(flowNodeId, other.flowNodeId);
    }
}
