package com.aaa.easyagent.biz.flow.engine;

import com.aaa.easyagent.biz.flow.compile.FlowCompiler;
import com.aaa.easyagent.biz.flow.compile.PortTypeSystem;
import com.aaa.easyagent.biz.flow.node.FlowNodePlugin;
import com.aaa.easyagent.biz.flow.node.FlowNodePluginRegistry;
import com.aaa.easyagent.biz.flow.node.NodeExecutionContext;
import com.aaa.easyagent.biz.flow.node.NodeExecutionResult;
import com.aaa.easyagent.biz.flow.plugin.EndNodePlugin;
import com.aaa.easyagent.biz.flow.plugin.ForkNodePlugin;
import com.aaa.easyagent.biz.flow.plugin.JoinNodePlugin;
import com.aaa.easyagent.biz.flow.plugin.StartNodePlugin;
import com.aaa.easyagent.biz.flow.runtime.FlowExecutionResult;
import com.aaa.easyagent.biz.flow.runtime.FlowRunContext;
import com.aaa.easyagent.biz.flow.runtime.FlowRunRecorder;
import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import com.aaa.easyagent.core.domain.flow.PortDefinition;
import com.aaa.easyagent.core.domain.flow.PortKind;
import com.aaa.easyagent.core.domain.flow.PortRef;
import com.aaa.easyagent.core.domain.flow.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFlowEngineTest {

    private final ExecutorService nodeExecutor = Executors.newFixedThreadPool(4);

    @AfterEach
    void shutdownExecutor() {
        nodeExecutor.shutdownNow();
    }

    /** 默认配置的引擎 */
    private DefaultFlowEngine engine() {
        return engine(new FlowOrchestrationProperties());
    }

    private DefaultFlowEngine engine(FlowOrchestrationProperties properties) {
        return new DefaultFlowEngine(new FlowRunRecorder(null, null), properties, nodeExecutor);
    }

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
        FlowExecutionResult result = engine().execute(compiler.compile(flow), FlowRunContext.builder()
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

        FlowExecutionResult result = engine()
                .execute(compiler.compile(flow), FlowRunContext.builder().runId(UUID.randomUUID().toString())
                        .flowId(1L).revisionId(2L).input(Map.of()).syncMode(true).build());

        assertEquals("SUCCEEDED", result.getStatus());
        assertTrue(String.valueOf(result.getResult()).contains("left"));
        assertTrue(!String.valueOf(result.getResult()).contains("right"));
    }

    /** 取消后不应再调度下游节点，整体状态为 CANCELLED。 */
    @Test
    void stopsSchedulingDownstreamNodesAfterCancel() {
        CancelPlugin cancelPlugin = new CancelPlugin();
        ValuePlugin valuePlugin = new ValuePlugin();
        FlowCompiler compiler = new FlowCompiler(new FlowNodePluginRegistry(List.of(
                new StartNodePlugin(), cancelPlugin, valuePlugin, new EndNodePlugin()
        )), new PortTypeSystem());
        FlowDefinition flow = new FlowDefinition().setFlowId(1L).setRevisionId(2L).setNodes(List.of(
                node("start", "START"), node("cancel", "CANCEL"), node("downstream", "VALUE"), node("end", "END")
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "cancel", "control"),
                edge("c2", EdgeKind.CONTROL, "cancel", "control", "downstream", "control"),
                edge("c3", EdgeKind.CONTROL, "downstream", "control", "end", "control"),
                edge("d1", EdgeKind.DATA, "downstream", "result", "end", "result")
        ));

        FlowExecutionResult result = engine().execute(compiler.compile(flow), FlowRunContext.builder()
                .runId(UUID.randomUUID().toString()).flowId(1L).revisionId(2L)
                .input(Map.of()).syncMode(true).build());

        assertEquals("CANCELLED", result.getStatus());
        assertFalse(valuePlugin.executed.get(), "取消后不应再执行下游节点");
    }

    /** 节点超过单节点超时应判定为失败，而不是让调度线程一直阻塞等待。 */
    @Test
    void failsFlowWhenNodeExceedsTimeout() {
        FlowOrchestrationProperties properties = new FlowOrchestrationProperties();
        properties.setNodeTimeoutSeconds(1L);
        FlowCompiler compiler = new FlowCompiler(new FlowNodePluginRegistry(List.of(
                new StartNodePlugin(), new SleepPlugin(3000L), new EndNodePlugin()
        )), new PortTypeSystem());
        FlowDefinition flow = new FlowDefinition().setFlowId(1L).setRevisionId(2L).setNodes(List.of(
                node("start", "START"), node("slow", "SLEEP"), node("end", "END")
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "slow", "control"),
                edge("c2", EdgeKind.CONTROL, "slow", "control", "end", "control"),
                edge("d1", EdgeKind.DATA, "slow", "result", "end", "result")
        ));

        FlowExecutionResult result = engine(properties).execute(compiler.compile(flow), FlowRunContext.builder()
                .runId(UUID.randomUUID().toString()).flowId(1L).revisionId(2L)
                .input(Map.of()).syncMode(true).build());

        assertEquals("FAILED", result.getStatus());
    }

    /** 必填数据入参在运行期缺失时判定节点失败，不把 null 当成空字符串继续往下传。 */
    @Test
    void failsFlowWhenRequiredDataInputMissingAtRuntime() {
        FlowCompiler compiler = new FlowCompiler(new FlowNodePluginRegistry(List.of(
                new StartNodePlugin(), new FixedRouterPlugin(), new ValuePlugin(), new EndNodePlugin()
        )), new PortTypeSystem());
        // router 只激活 left 分支，END 的必填 result 却接在未激活的 right 分支上
        FlowDefinition flow = new FlowDefinition().setFlowId(1L).setRevisionId(2L).setNodes(List.of(
                node("start", "START"), node("router", "ROUTER"), node("left", "VALUE"),
                node("right", "VALUE"), node("end", "END")
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "router", "control"),
                edge("c2", EdgeKind.CONTROL, "router", "left", "left", "control"),
                edge("c3", EdgeKind.CONTROL, "router", "right", "right", "control"),
                edge("c4", EdgeKind.CONTROL, "left", "control", "end", "control"),
                edge("d1", EdgeKind.DATA, "right", "result", "end", "result")
        ));

        FlowExecutionResult result = engine().execute(compiler.compile(flow), FlowRunContext.builder()
                .runId(UUID.randomUUID().toString()).flowId(1L).revisionId(2L)
                .input(Map.of()).syncMode(true).build());

        assertEquals("FAILED", result.getStatus());
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
        private final AtomicBoolean executed = new AtomicBoolean();

        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("VALUE").setDisplayName("Value")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("control", PortKind.CONTROL, false, true),
                            port("result", PortKind.DATA, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            executed.set(true);
            return NodeExecutionResult.of("result", context.getNode().getId());
        }
    }

    /** 模拟客户端断开：节点执行过程中置位取消标志 */
    private static final class CancelPlugin implements FlowNodePlugin {
        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("CANCEL").setDisplayName("Cancel")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("control", PortKind.CONTROL, false, true),
                            port("result", PortKind.DATA, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            context.getRunContext().cancel("测试：客户端断开");
            return NodeExecutionResult.of("result", context.getNode().getId());
        }
    }

    /** 模拟慢节点：用于验证单节点超时 */
    private record SleepPlugin(long sleepMillis) implements FlowNodePlugin {
        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("SLEEP").setDisplayName("Sleep")
                    .setInputPorts(List.of(port("control", PortKind.CONTROL, true, false)))
                    .setOutputPorts(List.of(port("control", PortKind.CONTROL, false, true),
                            port("result", PortKind.DATA, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            try {
                Thread.sleep(sleepMillis);
                return NodeExecutionResult.of("result", context.getNode().getId());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("节点被中断", ex);
            }
        }
    }

    private static PortDefinition port(String key, PortKind kind, boolean required, boolean multiple) {
        return new PortDefinition().setKey(key).setName(key).setKind(kind).setValueType(ValueType.ANY)
                .setRequired(required).setMultiple(multiple);
    }
}
