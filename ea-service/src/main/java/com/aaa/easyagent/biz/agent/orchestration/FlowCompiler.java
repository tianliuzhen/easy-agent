package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.flow.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlowCompiler {
    private final FlowNodePluginRegistry registry;
    private final PortTypeSystem portTypeSystem;

    public FlowValidationResult validate(FlowDefinition definition) {
        FlowValidationResult result = new FlowValidationResult().setValid(true);
        if (definition == null) {
            return result.addError("FLOW", null, "FLOW_EMPTY", "Flow 定义不能为空");
        }
        List<FlowNodeDefinition> nodes = definition.getNodes() == null ? List.of() : definition.getNodes();
        List<FlowEdgeDefinition> edges = definition.getEdges() == null ? List.of() : definition.getEdges();
        Map<String, FlowNodeDefinition> nodeMap = new LinkedHashMap<>();

        for (FlowNodeDefinition node : nodes) {
            if (node == null || StringUtils.isBlank(node.getId())) {
                result.addError("NODE", null, "NODE_ID_EMPTY", "节点 ID 不能为空");
                continue;
            }
            if (nodeMap.putIfAbsent(node.getId(), node) != null) {
                result.addError("NODE", node.getId(), "NODE_ID_DUPLICATE", "节点 ID 重复");
            }
            if (StringUtils.isBlank(node.getType()) || !registry.contains(node.getType(), node.getVersion())) {
                result.addError("NODE", node.getId(), "NODE_TYPE_UNKNOWN", "节点类型未注册：" + node.getType());
                continue;
            }
            FlowNodePlugin plugin = registry.get(node.getType(), node.getVersion());
            fillPorts(node, plugin.metadata());
            plugin.prepare(node);
            plugin.validate(node, result);
        }

        long starts = nodes.stream().filter(n -> "START".equals(n.getType())).count();
        long ends = nodes.stream().filter(n -> "END".equals(n.getType())).count();
        if (starts != 1) {
            result.addError("FLOW", null, "START_COUNT", "Flow 必须且只能包含一个 START 节点");
        }
        if (ends < 1) {
            result.addError("FLOW", null, "END_MISSING", "Flow 至少需要一个 END 节点");
        }

        Set<String> edgeIds = new HashSet<>();
        Map<String, Integer> targetPortCounts = new HashMap<>();
        for (FlowEdgeDefinition edge : edges) {
            if (edge == null || StringUtils.isBlank(edge.getId()) || !edgeIds.add(edge.getId())) {
                result.addError("EDGE", edge == null ? null : edge.getId(), "EDGE_ID", "边 ID 不能为空且不能重复");
                continue;
            }
            if (edge.getSource() == null || edge.getTarget() == null) {
                result.addError("EDGE", edge.getId(), "EDGE_ENDPOINT", "边必须指定来源和目标端口");
                continue;
            }
            FlowNodeDefinition sourceNode = nodeMap.get(edge.getSource().getNodeId());
            FlowNodeDefinition targetNode = nodeMap.get(edge.getTarget().getNodeId());
            if (sourceNode == null || targetNode == null) {
                result.addError("EDGE", edge.getId(), "EDGE_NODE_MISSING", "边引用了不存在的节点");
                continue;
            }
            PortDefinition sourcePort = findPort(sourceNode.getOutputPorts(), edge.getSource().getPortKey());
            PortDefinition targetPort = findPort(targetNode.getInputPorts(), edge.getTarget().getPortKey());
            if (sourcePort == null || targetPort == null) {
                result.addError("EDGE", edge.getId(), "EDGE_PORT_MISSING", "边引用了不存在的端口");
                continue;
            }
            EdgeKind expected = EdgeKind.valueOf(sourcePort.getKind().name());
            if (edge.getKind() != expected || targetPort.getKind() != sourcePort.getKind()) {
                result.addError("EDGE", edge.getId(), "EDGE_KIND", "边类型与端口类型不一致");
            }
            if (edge.getKind() == EdgeKind.DATA && !portTypeSystem.compatible(sourcePort, targetPort)) {
                result.addError("EDGE", edge.getId(), "PORT_TYPE", "端口类型不兼容："
                        + sourcePort.getValueType() + " → " + targetPort.getValueType());
            }
            if (edge.getKind() == EdgeKind.WORKER
                    && (!("SUPERVISOR".equals(sourceNode.getType())) || !("AGENT".equals(targetNode.getType())))) {
                result.addError("EDGE", edge.getId(), "WORKER_TOPOLOGY",
                        "Worker 边只能从 SUPERVISOR 连接到 AGENT");
            }
            String targetKey = edge.getTarget().getNodeId() + ":" + edge.getTarget().getPortKey();
            int count = targetPortCounts.merge(targetKey, 1, Integer::sum);
            if (count > 1 && !targetPort.isMultiple()) {
                result.addError("EDGE", edge.getId(), "PORT_MULTIPLE", "目标端口不允许多条输入边");
            }
        }

        validateRequiredPorts(nodes, edges, result);
        validateWorkerNodes(nodes, edges, result);
        validateControlGraph(nodes, edges, result);
        return result;
    }

    public CompiledFlowPlan compile(FlowDefinition definition) {
        FlowValidationResult validation = validate(definition);
        if (!validation.isValid()) {
            throw new FlowValidationException(validation);
        }
        Map<String, FlowNodeDefinition> nodes = definition.getNodes().stream()
                .collect(Collectors.toMap(FlowNodeDefinition::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, FlowNodePlugin> plugins = nodes.values().stream()
                .collect(Collectors.toMap(FlowNodeDefinition::getId,
                        node -> registry.get(node.getType(), node.getVersion()), (a, b) -> a, LinkedHashMap::new));
        Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> incoming = indexEdges(definition.getEdges(), false);
        Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> outgoing = indexEdges(definition.getEdges(), true);
        return new CompiledFlowPlan(definition, checksum(definition), nodes, plugins, incoming, outgoing);
    }

    public List<NodeMetadata> nodeTypes() {
        return registry.metadata();
    }

    private void fillPorts(FlowNodeDefinition node, NodeMetadata metadata) {
        if (node.getInputPorts() == null || node.getInputPorts().isEmpty()) {
            node.setInputPorts(copyPorts(metadata.getInputPorts()));
        }
        if (node.getOutputPorts() == null || node.getOutputPorts().isEmpty()) {
            node.setOutputPorts(copyPorts(metadata.getOutputPorts()));
        }
        if (node.getConfig() == null) {
            node.setConfig(new LinkedHashMap<>());
        }
    }

    private List<PortDefinition> copyPorts(List<PortDefinition> ports) {
        return ports.stream().map(port -> new PortDefinition()
                .setKey(port.getKey()).setName(port.getName()).setKind(port.getKind())
                .setValueType(port.getValueType()).setRequired(port.isRequired())
                .setMultiple(port.isMultiple()).setDefaultValue(port.getDefaultValue())
                .setSchema(port.getSchema())).toList();
    }

    private void validateRequiredPorts(List<FlowNodeDefinition> nodes, List<FlowEdgeDefinition> edges,
                                       FlowValidationResult result) {
        Set<String> connected = edges.stream().filter(e -> e.getTarget() != null)
                .map(e -> e.getTarget().getNodeId() + ":" + e.getTarget().getPortKey()).collect(Collectors.toSet());
        for (FlowNodeDefinition node : nodes) {
            for (PortDefinition port : node.getInputPorts()) {
                if (port.isRequired() && port.getDefaultValue() == null
                        && !connected.contains(node.getId() + ":" + port.getKey())) {
                    result.addError("NODE", node.getId(), "REQUIRED_PORT",
                            "必填输入端口未连接：" + port.getName());
                }
            }
        }
    }

    private void validateControlGraph(List<FlowNodeDefinition> nodes, List<FlowEdgeDefinition> edges,
                                      FlowValidationResult result) {
        Map<String, List<String>> graph = new HashMap<>();
        for (FlowEdgeDefinition edge : edges) {
            if (edge.getKind() == EdgeKind.CONTROL && edge.getSource() != null && edge.getTarget() != null) {
                graph.computeIfAbsent(edge.getSource().getNodeId(), key -> new ArrayList<>())
                        .add(edge.getTarget().getNodeId());
            }
        }
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (FlowNodeDefinition node : nodes) {
            if (hasCycle(node.getId(), graph, visiting, visited)) {
                result.addError("FLOW", null, "CONTROL_CYCLE", "控制流存在环；循环必须使用专用 LOOP 节点");
                break;
            }
        }
        nodes.stream().filter(n -> "START".equals(n.getType())).findFirst().ifPresent(start -> {
            Set<String> reachable = new HashSet<>();
            visit(start.getId(), graph, reachable);
            nodes.stream().filter(n -> !"AGENT".equals(n.getType()) || hasControlPort(n))
                    .filter(n -> !reachable.contains(n.getId()))
                    .forEach(n -> result.addWarning("NODE", n.getId(), "UNREACHABLE", "节点无法从 START 到达"));
        });
    }

    private void validateWorkerNodes(List<FlowNodeDefinition> nodes, List<FlowEdgeDefinition> edges,
                                     FlowValidationResult result) {
        for (FlowNodeDefinition node : nodes) {
            if (!"AGENT".equals(node.getType())) continue;
            boolean worker = edges.stream().anyMatch(edge -> edge.getKind() == EdgeKind.WORKER
                    && edge.getTarget() != null && node.getId().equals(edge.getTarget().getNodeId()));
            boolean control = edges.stream().anyMatch(edge -> edge.getKind() == EdgeKind.CONTROL
                    && edge.getTarget() != null && node.getId().equals(edge.getTarget().getNodeId()));
            if (worker && control) {
                result.addError("NODE", node.getId(), "WORKER_CONTROL_MIXED",
                        "同一个 Agent 不能同时作为 Worker 和普通控制流节点");
            }
        }
    }

    private boolean hasControlPort(FlowNodeDefinition node) {
        return node.getInputPorts().stream().anyMatch(p -> p.getKind() == PortKind.CONTROL);
    }

    private boolean hasCycle(String node, Map<String, List<String>> graph, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(node)) return true;
        if (visited.contains(node)) return false;
        visiting.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (hasCycle(next, graph, visiting, visited)) return true;
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private void visit(String node, Map<String, List<String>> graph, Set<String> visited) {
        if (!visited.add(node)) return;
        graph.getOrDefault(node, List.of()).forEach(next -> visit(next, graph, visited));
    }

    private PortDefinition findPort(List<PortDefinition> ports, String key) {
        if (ports == null) return null;
        return ports.stream().filter(port -> Objects.equals(port.getKey(), key)).findFirst().orElse(null);
    }

    private Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> indexEdges(List<FlowEdgeDefinition> edges,
                                                                            boolean source) {
        Map<EdgeKind, Map<String, List<FlowEdgeDefinition>>> result = new EnumMap<>(EdgeKind.class);
        for (FlowEdgeDefinition edge : edges) {
            String nodeId = source ? edge.getSource().getNodeId() : edge.getTarget().getNodeId();
            result.computeIfAbsent(edge.getKind(), key -> new HashMap<>())
                    .computeIfAbsent(nodeId, key -> new ArrayList<>()).add(edge);
        }
        return result;
    }

    public String checksum(FlowDefinition definition) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(JacksonUtil.beanToStr(definition).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
