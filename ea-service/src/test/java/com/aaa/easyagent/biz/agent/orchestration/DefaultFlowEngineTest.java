package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.biz.agent.orchestration.plugin.EndNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.ForkNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.JoinNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.StartNodePlugin;
import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import com.aaa.easyagent.core.domain.flow.PortDefinition;
import com.aaa.easyagent.core.domain.flow.PortKind;
import com.aaa.easyagent.core.domain.flow.PortRef;
import com.aaa.easyagent.core.domain.flow.ValueType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFlowEngineTest {

    @Test
    void executesForkBranchesConcurrentlyAndJoinsThem() {
        DelayPlugin delay = new DelayPlugin();
        FlowCompiler compiler = new FlowCompiler(new FlowNodePluginRegistry(List.of(
                new StartNodePlugin(), new ForkNodePlugin(), delay, new JoinNodePlugin(), new EndNodePlugin()
        )), new PortTypeSystem());
        FlowDefinition flow = new FlowDefinition().setFlowId(1L).setRevisionId(2L).setNodes(List.of(
                node("start", "START"), node("fork", "FORK"), node("left", "DELAY"),
                node("right", "DELAY"), node("join", "JOIN"), node("end", "END")
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "fork", "control"),
                edge("c2", EdgeKind.CONTROL, "fork", "control", "left", "control"),
                edge("c3", EdgeKind.CONTROL, "fork", "control", "right", "control"),
                edge("c4", EdgeKind.CONTROL, "left", "control", "join", "control"),
                edge("c5", EdgeKind.CONTROL, "right", "control", "join", "control"),
                edge("d1", EdgeKind.DATA, "left", "result", "join", "items"),
                edge("d2", EdgeKind.DATA, "right", "result", "join", "items"),
                edge("c6", EdgeKind.CONTROL, "join", "control", "end", "control"),
                edge("d3", EdgeKind.DATA, "join", "result", "end", "result")
        ));
        DefaultFlowEngine engine = new DefaultFlowEngine(new FlowRunRecorder(null, null));

        FlowExecutionResult result = engine.execute(compiler.compile(flow), FlowRunContext.builder()
                .runId(UUID.randomUUID().toString()).flowId(1L).revisionId(2L)
                .input(Map.of("input", "hello")).syncMode(true).build());

        assertEquals("SUCCEEDED", result.getStatus());
        assertTrue(delay.overlap.get(), "FORK 的两个分支应重叠执行");
        assertTrue(String.valueOf(result.getResult()).contains("left"));
        assertTrue(String.valueOf(result.getResult()).contains("right"));
    }

    @Test
    void skipsUnselectedRouterBranchAndStillReachesJoin() {
        FlowCompiler compiler = new FlowCompiler(new FlowNodePluginRegistry(List.of(
                new StartNodePlugin(), new FixedRouterPlugin(), new ValuePlugin(),
                new JoinNodePlugin(), new EndNodePlugin()
        )), new PortTypeSystem());
        FlowDefinition flow = new FlowDefinition().setFlowId(1L).setRevisionId(2L).setNodes(List.of(
                node("start", "START"), node("router", "ROUTER"), node("left", "VALUE"),
                node("right", "VALUE"), node("join", "JOIN"), node("end", "END")
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "router", "control"),
                edge("c2", EdgeKind.CONTROL, "router", "left", "left", "control"),
                edge("c3", EdgeKind.CONTROL, "router", "right", "right", "control"),
                edge("c4", EdgeKind.CONTROL, "left", "control", "join", "control"),
                edge("c5", EdgeKind.CONTROL, "right", "control", "join", "control"),
                edge("d1", EdgeKind.DATA, "left", "result", "join", "items"),
                edge("d2", EdgeKind.DATA, "right", "result", "join", "items"),
                edge("c6", EdgeKind.CONTROL, "join", "control", "end", "control"),
                edge("d3", EdgeKind.DATA, "join", "result", "end", "result")
        ));

        FlowExecutionResult result = new DefaultFlowEngine(new FlowRunRecorder(null, null))
                .execute(compiler.compile(flow), FlowRunContext.builder().runId(UUID.randomUUID().toString())
                        .flowId(1L).revisionId(2L).input(Map.of()).syncMode(true).build());

        assertEquals("SUCCEEDED", result.getStatus());
        assertTrue(String.valueOf(result.getResult()).contains("left"));
        assertTrue(!String.valueOf(result.getResult()).contains("right"));
    }

    private FlowNodeDefinition node(String id, String type) {
        return new FlowNodeDefinition().setId(id).setType(type).setName(id).setConfig(new LinkedHashMap<>());
    }

    private FlowEdgeDefinition edge(String id, EdgeKind kind, String sourceNode, String sourcePort,
                                    String targetNode, String targetPort) {
        return new FlowEdgeDefinition().setId(id).setKind(kind)
                .setSource(new PortRef().setNodeId(sourceNode).setPortKey(sourcePort))
                .setTarget(new PortRef().setNodeId(targetNode).setPortKey(targetPort));
    }

    private static final class DelayPlugin implements FlowNodePlugin {
        private final AtomicInteger running = new AtomicInteger();
        private final AtomicBoolean overlap = new AtomicBoolean();

        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("DELAY").setDisplayName("Delay")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("control", PortKind.CONTROL, false, true),
                            port("result", PortKind.DATA, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            if (running.incrementAndGet() > 1) overlap.set(true);
            try {
                Thread.sleep(100);
                return NodeExecutionResult.of("result", context.getNode().getId());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            } finally {
                running.decrementAndGet();
            }
        }

        private PortDefinition port(String key, PortKind kind, boolean required, boolean multiple) {
            return new PortDefinition().setKey(key).setName(key).setKind(kind).setValueType(ValueType.ANY)
                    .setRequired(required).setMultiple(multiple);
        }
    }

    private static final class FixedRouterPlugin implements FlowNodePlugin {
        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("ROUTER").setDisplayName("Router")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("left", PortKind.CONTROL, false, true),
                            port("right", PortKind.CONTROL, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return new NodeExecutionResult().setRouteKey("left");
        }
    }

    private static final class ValuePlugin implements FlowNodePlugin {
        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("VALUE").setDisplayName("Value")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("control", PortKind.CONTROL, false, true),
                            port("result", PortKind.DATA, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.of("result", context.getNode().getId());
        }
    }

    private static PortDefinition port(String key, PortKind kind, boolean required, boolean multiple) {
        return new PortDefinition().setKey(key).setName(key).setKind(kind).setValueType(ValueType.ANY)
                .setRequired(required).setMultiple(multiple);
    }
}
