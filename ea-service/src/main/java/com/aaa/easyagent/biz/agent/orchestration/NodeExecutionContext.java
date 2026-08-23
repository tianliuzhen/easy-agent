package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Getter
@Builder
public class NodeExecutionContext {
    private final FlowNodeDefinition node;
    private final Map<String, Object> inputs;
    private final FlowState state;
    private final FlowRunContext runContext;
    private final FlowEventPublisher events;
    private final List<FlowEdgeDefinition> outgoingEdges;
    private final BiFunction<String, Map<String, Object>, NodeExecutionResult> workerInvoker;
}
