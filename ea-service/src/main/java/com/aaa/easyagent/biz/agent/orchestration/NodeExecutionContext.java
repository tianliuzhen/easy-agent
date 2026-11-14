package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** 单个节点执行时拿到的上下文：入参、运行状态、事件出口以及 Worker 派发能力。 */
@Getter
@Builder
public class NodeExecutionContext {
    private final FlowNodeDefinition node;
    private final Map<String, Object> inputs;
    private final FlowState state;
    private final FlowRunContext runContext;
    private final FlowEventPublisher events;
    private final List<FlowEdgeDefinition> outgoingEdges;
    /** 按 nodeId 取图中的节点定义，SUPERVISOR 用它读取 Worker 的配置 */
    private final Function<String, FlowNodeDefinition> nodeResolver;
    private final BiFunction<String, Map<String, Object>, NodeExecutionResult> workerInvoker;
}
