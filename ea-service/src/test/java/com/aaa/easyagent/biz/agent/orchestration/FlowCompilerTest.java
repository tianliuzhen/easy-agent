package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.biz.agent.orchestration.plugin.EndNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.RouterNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.StartNodePlugin;
import com.aaa.easyagent.biz.agent.orchestration.plugin.TemplateNodePlugin;
import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowValidationResult;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import com.aaa.easyagent.core.domain.flow.PortDefinition;
import com.aaa.easyagent.core.domain.flow.PortKind;
import com.aaa.easyagent.core.domain.flow.PortRef;
import com.aaa.easyagent.core.domain.flow.ValueType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowCompilerTest {

    @Test
    void compilesStartTemplateEndGraph() {
        FlowCompiler compiler = compiler(new StartNodePlugin(),
                new TemplateNodePlugin(new FlowTemplateRenderer()), new EndNodePlugin());
        FlowDefinition flow = new FlowDefinition().setNodes(List.of(
                node("start", "START", Map.of()),
                node("template", "TEMPLATE", Map.of("template", "回答：{{ data }}")),
                node("end", "END", Map.of())
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "template", "control"),
                edge("d1", EdgeKind.DATA, "start", "input", "template", "data"),
                edge("c2", EdgeKind.CONTROL, "template", "control", "end", "control"),
                edge("d2", EdgeKind.DATA, "template", "result", "end", "result")
        ));

        CompiledFlowPlan plan = compiler.compile(flow);

        assertNotNull(plan);
        assertTrue(plan.getChecksum().length() == 64);
    }

    @Test
    void rejectsIncompatibleDataPorts() {
        FlowCompiler compiler = compiler(new StartNodePlugin(), new StrictEndPlugin());
        FlowDefinition flow = new FlowDefinition().setNodes(List.of(
                node("start", "START", Map.of()), node("end", "END", Map.of())
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "end", "control"),
                edge("d1", EdgeKind.DATA, "start", "image", "end", "result")
        ));

        FlowValidationResult result = compiler.validate(flow);

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue -> "PORT_TYPE".equals(issue.getCode())));
    }

    @Test
    void rejectsControlCycle() {
        FlowCompiler compiler = compiler(new StartNodePlugin(),
                new TemplateNodePlugin(new FlowTemplateRenderer()), new EndNodePlugin());
        FlowDefinition flow = new FlowDefinition().setNodes(List.of(
                node("start", "START", Map.of()),
                node("a", "TEMPLATE", Map.of("template", "a")),
                node("b", "TEMPLATE", Map.of("template", "b")),
                node("end", "END", Map.of())
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "a", "control"),
                edge("c2", EdgeKind.CONTROL, "a", "control", "b", "control"),
                edge("c3", EdgeKind.CONTROL, "b", "control", "a", "control"),
                edge("c4", EdgeKind.CONTROL, "b", "control", "end", "control"),
                edge("d1", EdgeKind.DATA, "b", "result", "end", "result")
        ));

        FlowValidationResult result = compiler.validate(flow);

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue -> "CONTROL_CYCLE".equals(issue.getCode())));
    }

    @Test
    void materializesRouterBranchesAsControlPorts() {
        RouterNodePlugin router = new RouterNodePlugin(null);
        FlowCompiler compiler = compiler(new StartNodePlugin(), router, new EndNodePlugin());
        FlowNodeDefinition routerNode = node("router", "ROUTER", Map.of(
                "agentId", 1,
                "branches", List.of(
                        Map.of("key", "writing", "label", "写作", "description", "写作任务"),
                        Map.of("key", "search", "label", "搜索", "description", "搜索任务")),
                "fallback", "search"));
        FlowDefinition flow = new FlowDefinition().setNodes(List.of(
                node("start", "START", Map.of()), routerNode, node("end", "END", Map.of())
        )).setEdges(List.of(
                edge("c1", EdgeKind.CONTROL, "start", "control", "router", "control"),
                edge("d1", EdgeKind.DATA, "start", "input", "router", "input"),
                edge("c2", EdgeKind.CONTROL, "router", "writing", "end", "control"),
                edge("d2", EdgeKind.DATA, "router", "route", "end", "result")
        ));

        FlowValidationResult result = compiler.validate(flow);

        assertTrue(result.isValid());
        assertTrue(routerNode.getOutputPorts().stream().anyMatch(port -> "writing".equals(port.getKey())
                && port.getKind() == PortKind.CONTROL));
        assertTrue(routerNode.getOutputPorts().stream().anyMatch(port -> "search".equals(port.getKey())
                && port.getKind() == PortKind.CONTROL));
    }

    private FlowCompiler compiler(FlowNodePlugin... plugins) {
        return new FlowCompiler(new FlowNodePluginRegistry(List.of(plugins)), new PortTypeSystem());
    }

    private FlowNodeDefinition node(String id, String type, Map<String, Object> config) {
        return new FlowNodeDefinition().setId(id).setType(type).setName(id)
                .setConfig(new LinkedHashMap<>(config));
    }

    private FlowEdgeDefinition edge(String id, EdgeKind kind, String sourceNode, String sourcePort,
                                    String targetNode, String targetPort) {
        return new FlowEdgeDefinition().setId(id).setKind(kind)
                .setSource(new PortRef().setNodeId(sourceNode).setPortKey(sourcePort))
                .setTarget(new PortRef().setNodeId(targetNode).setPortKey(targetPort));
    }

    private static final class StrictEndPlugin implements FlowNodePlugin {
        @Override
        public NodeMetadata metadata() {
            return new NodeMetadata().setType("END").setDisplayName("End")
                    .setInputPorts(List.of(
                            port("control", PortKind.CONTROL, ValueType.ANY, true, true),
                            port("result", PortKind.DATA, ValueType.STRING, true, false)))
                    .setOutputPorts(List.of(port("result", PortKind.DATA, ValueType.STRING, false, true)));
        }

        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.of("result", context.getInputs().get("result"));
        }

        private PortDefinition port(String key, PortKind kind, ValueType type, boolean required, boolean multiple) {
            return new PortDefinition().setKey(key).setName(key).setKind(kind).setValueType(type)
                    .setRequired(required).setMultiple(multiple);
        }
    }
}
