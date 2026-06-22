package com.aaa.easyagent.biz.agent.flow;

/**
 * 编排加载服务：按 flowId 加载 {@link FlowContext}（编排主表 + 有序成员 Agent）。
 *
 * @author liuzhen.tian
 */
public interface FlowManagerService {

    /**
     * 加载编排上下文骨架（flow + nodes + members + strategy）。
     * 运行期字段（sse/sessionId/imageBase64 等）由调用方填充。
     *
     * @param flowId 编排 ID
     * @return 已填充 flow/nodes/members/strategy 的 {@link FlowContext}
     */
    FlowContext loadFlow(Long flowId);
}
