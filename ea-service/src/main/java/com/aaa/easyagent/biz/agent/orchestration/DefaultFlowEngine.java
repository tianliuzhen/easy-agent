package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.PortDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** 基于有向无环图的动态调度器。控制边使用三态传播，FORK 后的就绪节点并发执行。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultFlowEngine implements FlowEngine {
    private final FlowRunRecorder recorder;

    @Override
    public FlowExecutionResult execute(CompiledFlowPlan plan, FlowRunContext context) {
        FlowState state = new FlowState();
        FlowEventPublisher events = new FlowEventPublisher(context);
        Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();
        Map<String, EdgeState> edgeStates = new ConcurrentHashMap<>();
        plan.getNodes().keySet().forEach(id -> nodeStates.put(id, NodeState.PENDING));
        plan.getDefinition().getEdges().stream().filter(edge -> edge.getKind() == EdgeKind.CONTROL)
                .forEach(edge -> edgeStates.put(edge.getId(), EdgeState.PENDING));

        AtomicLong inputTokens = new AtomicLong();
        AtomicLong outputTokens = new AtomicLong();
        recorder.startFlow(context);
        events.publish("flow_started", null, null, context.getInput());
        int parallelism = Math.max(2, Math.min(8, plan.getNodes().size()));
        ExecutorService executor = Executors.newFixedThreadPool(parallelism, threadFactory(context.getRunId()));
        CompletionService<NodeTaskResult> completion = new ExecutorCompletionService<>(executor);
        int running = 0;
        Object finalResult = null;
        boolean endReached = false;

        try {
            while (true) {
                if (context.getCancelled().get()) {
                    recorder.finishFlow(context, "CANCELLED", null, inputTokens.get(), outputTokens.get(), null);
                    return result(context, "CANCELLED", null, state, inputTokens.get(), outputTokens.get());
                }

                boolean progressed;
                do {
                    progressed = false;
                    for (FlowNodeDefinition node : plan.getNodes().values()) {
                        if (nodeStates.get(node.getId()) != NodeState.PENDING) continue;
                        Readiness readiness = readiness(plan, node, nodeStates, edgeStates);
                        if (readiness == Readiness.SKIP) {
                            nodeStates.put(node.getId(), NodeState.SKIPPED);
                            plan.outgoing(EdgeKind.CONTROL, node.getId())
                                    .forEach(edge -> edgeStates.replace(edge.getId(), EdgeState.PENDING, EdgeState.SKIPPED));
                            events.publish("node_skipped", node, agentId(node), "上游分支未激活");
                            progressed = true;
                        } else if (readiness == Readiness.READY) {
                            nodeStates.put(node.getId(), NodeState.RUNNING);
                            events.publish("node_ready", node, agentId(node), null);
                            completion.submit(() -> executeNode(plan, context, state, events, node));
                            running++;
                            progressed = true;
                        }
                    }
                } while (progressed);

                if (running == 0) break;

                NodeTaskResult task = completion.take().get();
                running--;
                FlowNodeDefinition node = task.node();
                if (task.error() != null) {
                    nodeStates.put(node.getId(), NodeState.FAILED);
                    context.getCancelled().set(true);
                    String message = task.error().getMessage() == null
                            ? task.error().getClass().getSimpleName() : task.error().getMessage();
                    log.error("Flow node failed, runId={}, nodeId={}", context.getRunId(), node.getId(), task.error());
                    recorder.finishNode(task.nodeRunId(), "FAILED", null, 0, 0, message);
                    events.publish("node_failed", node, agentId(node), message);
                    events.publish("flow_failed", null, null, message);
                    recorder.finishFlow(context, "FAILED", null, inputTokens.get(), outputTokens.get(), message);
                    return result(context, "FAILED", null, state, inputTokens.get(), outputTokens.get());
                }

                NodeExecutionResult nodeResult = task.result();
                nodeResult.getOutputs().forEach((port, value) -> state.put(node.getId(), port, value));
                inputTokens.addAndGet(nodeResult.getInputTokens());
                outputTokens.addAndGet(nodeResult.getOutputTokens());
                nodeStates.put(node.getId(), NodeState.SUCCEEDED);
                recorder.finishNode(task.nodeRunId(), "SUCCEEDED", nodeResult.getOutputs(),
                        nodeResult.getInputTokens(), nodeResult.getOutputTokens(), null);
                events.publish("node_succeeded", node, agentId(node), nodeResult.getOutputs());
                activateOutgoing(plan, node, nodeResult, edgeStates);

                if ("END".equals(node.getType())) {
                    endReached = true;
                    if (finalResult == null) finalResult = nodeResult.primaryOutput();
                }
            }

            if (!endReached) {
                String message = "Flow 执行结束但没有到达 END 节点";
                events.publish("flow_failed", null, null, message);
                recorder.finishFlow(context, "FAILED", null, inputTokens.get(), outputTokens.get(), message);
                return result(context, "FAILED", null, state, inputTokens.get(), outputTokens.get());
            }
            events.publish("flow_succeeded", null, null, finalResult);
            recorder.finishFlow(context, "SUCCEEDED", finalResult, inputTokens.get(), outputTokens.get(), null);
            return result(context, "SUCCEEDED", finalResult, state, inputTokens.get(), outputTokens.get());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            context.getCancelled().set(true);
            recorder.finishFlow(context, "CANCELLED", null, inputTokens.get(), outputTokens.get(), "执行线程被中断");
            return result(context, "CANCELLED", null, state, inputTokens.get(), outputTokens.get());
        } catch (Exception ex) {
            log.error("Flow execution failed, runId={}", context.getRunId(), ex);
            events.publish("flow_failed", null, null, ex.getMessage());
            recorder.finishFlow(context, "FAILED", null, inputTokens.get(), outputTokens.get(), ex.getMessage());
            return result(context, "FAILED", null, state, inputTokens.get(), outputTokens.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private NodeTaskResult executeNode(CompiledFlowPlan plan, FlowRunContext context, FlowState state,
                                       FlowEventPublisher events, FlowNodeDefinition node) {
        Map<String, Object> inputs = collectInputs(plan, node, state);
        Long nodeRunId = recorder.startNode(context, node, agentId(node), inputs);
        events.publish("node_started", node, agentId(node), inputs);
        try {
            NodeExecutionContext nodeContext = NodeExecutionContext.builder()
                    .node(node).inputs(inputs).state(state).runContext(context).events(events)
                    .outgoingEdges(allOutgoing(plan, node.getId()))
                    .workerInvoker((workerId, workerInput) -> executeWorker(
                            plan, context, state, events, workerId, workerInput))
                    .build();
            return new NodeTaskResult(node, nodeRunId, plan.getPlugins().get(node.getId()).execute(nodeContext), null);
        } catch (Throwable error) {
            return new NodeTaskResult(node, nodeRunId, null, error);
        }
    }

    private NodeExecutionResult executeWorker(CompiledFlowPlan plan, FlowRunContext context, FlowState state,
                                              FlowEventPublisher events, String workerId,
                                              Map<String, Object> workerInput) {
        FlowNodeDefinition worker = plan.getNodes().get(workerId);
        if (worker == null) throw new IllegalArgumentException("Worker 节点不存在：" + workerId);
        if (!"AGENT".equals(worker.getType())) {
            throw new IllegalArgumentException("Worker 端口只能连接 AGENT 节点：" + workerId);
        }
        Long nodeRunId = recorder.startNode(context, worker, agentId(worker), workerInput);
        events.publish("node_started", worker, agentId(worker), workerInput);
        try {
            NodeExecutionContext workerContext = NodeExecutionContext.builder().node(worker).inputs(workerInput)
                    .state(state).runContext(context).events(events).outgoingEdges(allOutgoing(plan, workerId))
                    .workerInvoker(null).build();
            NodeExecutionResult result = plan.getPlugins().get(workerId).execute(workerContext);
            result.getOutputs().forEach((key, value) -> state.put(workerId, key, value));
            recorder.finishNode(nodeRunId, "SUCCEEDED", result.getOutputs(), result.getInputTokens(),
                    result.getOutputTokens(), null);
            events.publish("node_succeeded", worker, agentId(worker), result.getOutputs());
            return result;
        } catch (RuntimeException ex) {
            recorder.finishNode(nodeRunId, "FAILED", null, 0, 0, ex.getMessage());
            events.publish("node_failed", worker, agentId(worker), ex.getMessage());
            throw ex;
        }
    }

    private Readiness readiness(CompiledFlowPlan plan, FlowNodeDefinition node,
                                Map<String, NodeState> nodeStates, Map<String, EdgeState> edgeStates) {
        List<FlowEdgeDefinition> incoming = plan.incoming(EdgeKind.CONTROL, node.getId());
        if (incoming.isEmpty()) {
            return "START".equals(node.getType()) ? Readiness.READY : Readiness.WAIT;
        }
        boolean active = false;
        for (FlowEdgeDefinition edge : incoming) {
            EdgeState state = edgeStates.getOrDefault(edge.getId(), EdgeState.PENDING);
            if (state == EdgeState.PENDING) return Readiness.WAIT;
            if (state == EdgeState.ACTIVE) active = true;
        }
        if (!active) return Readiness.SKIP;
        for (FlowEdgeDefinition edge : plan.incoming(EdgeKind.DATA, node.getId())) {
            NodeState source = nodeStates.get(edge.getSource().getNodeId());
            if (source == NodeState.PENDING || source == NodeState.RUNNING) return Readiness.WAIT;
        }
        return Readiness.READY;
    }

    private void activateOutgoing(CompiledFlowPlan plan, FlowNodeDefinition node, NodeExecutionResult result,
                                  Map<String, EdgeState> edgeStates) {
        for (FlowEdgeDefinition edge : plan.outgoing(EdgeKind.CONTROL, node.getId())) {
            boolean active = result.getRouteKey() == null
                    || Objects.equals(result.getRouteKey(), edge.getSource().getPortKey());
            edgeStates.put(edge.getId(), active ? EdgeState.ACTIVE : EdgeState.SKIPPED);
        }
    }

    private Map<String, Object> collectInputs(CompiledFlowPlan plan, FlowNodeDefinition node, FlowState state) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        Map<String, PortDefinition> ports = new LinkedHashMap<>();
        node.getInputPorts().forEach(port -> ports.put(port.getKey(), port));
        for (FlowEdgeDefinition edge : plan.incoming(EdgeKind.DATA, node.getId())) {
            Object value = state.get(edge.getSource().getNodeId(), edge.getSource().getPortKey());
            PortDefinition port = ports.get(edge.getTarget().getPortKey());
            if (port != null && port.isMultiple()) {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) inputs.computeIfAbsent(port.getKey(), key -> new ArrayList<>());
                if (value != null) values.add(value);
            } else {
                if (value != null) inputs.put(edge.getTarget().getPortKey(), value);
            }
        }
        node.getInputPorts().stream().filter(port -> port.getDefaultValue() != null)
                .forEach(port -> inputs.putIfAbsent(port.getKey(), port.getDefaultValue()));
        return inputs;
    }

    private List<FlowEdgeDefinition> allOutgoing(CompiledFlowPlan plan, String nodeId) {
        List<FlowEdgeDefinition> edges = new ArrayList<>();
        for (EdgeKind kind : EdgeKind.values()) edges.addAll(plan.outgoing(kind, nodeId));
        return edges;
    }

    private Long agentId(FlowNodeDefinition node) {
        if (node == null || node.getConfig() == null || node.getConfig().get("agentId") == null) return null;
        try {
            return Long.valueOf(String.valueOf(node.getConfig().get("agentId")));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private FlowExecutionResult result(FlowRunContext context, String status, Object value, FlowState state,
                                       long inputTokens, long outputTokens) {
        return new FlowExecutionResult().setRunId(context.getRunId()).setStatus(status).setResult(value)
                .setState(state.snapshot()).setInputTokens(inputTokens).setOutputTokens(outputTokens);
    }

    private ThreadFactory threadFactory(String runId) {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "flow-" + runId + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private enum EdgeState {PENDING, ACTIVE, SKIPPED}
    private enum NodeState {PENDING, RUNNING, SUCCEEDED, SKIPPED, FAILED}
    private enum Readiness {WAIT, READY, SKIP}
    private record NodeTaskResult(FlowNodeDefinition node, Long nodeRunId,
                                  NodeExecutionResult result, Throwable error) {
    }
}
