package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CompiledFlowPlan {
    private final FlowDefinition definition;
    private final String checksum;
    private final Map<String, FlowNodeDefinition> nodes;
    private final Map<String, FlowNodePlugin> plugins;
    private final Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> incoming;
    private final Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> outgoing;

    public CompiledFlowPlan(FlowDefinition definition, String checksum,
                            Map<String, FlowNodeDefinition> nodes,
                            Map<String, FlowNodePlugin> plugins,
                            Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> incoming,
                            Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> outgoing) {
        this.definition = definition;
        this.checksum = checksum;
        this.nodes = nodes;
        this.plugins = plugins;
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public List<FlowEdgeDefinition> incoming(EdgeKind kind, String nodeId) {
        return incoming.getOrDefault(kind, Map.of()).getOrDefault(nodeId, List.of());
    }

    public List<FlowEdgeDefinition> outgoing(EdgeKind kind, String nodeId) {
        return outgoing.getOrDefault(kind, Map.of()).getOrDefault(nodeId, List.of());
    }
}
