package com.aaa.easyagent.biz.flow.engine;

import com.aaa.easyagent.biz.flow.compile.CompiledFlowPlan;
import com.aaa.easyagent.biz.flow.event.FlowEventPublisher;
import com.aaa.easyagent.biz.flow.event.FlowEventType;
import com.aaa.easyagent.biz.flow.node.FlowNodeTypes;
import com.aaa.easyagent.biz.flow.node.NodeExecutionContext;
import com.aaa.easyagent.biz.flow.node.NodeExecutionResult;
import com.aaa.easyagent.biz.flow.runtime.FlowExecutionResult;
import com.aaa.easyagent.biz.flow.runtime.FlowRunContext;
import com.aaa.easyagent.biz.flow.runtime.FlowRunRecorder;
import com.aaa.easyagent.biz.flow.runtime.FlowRunStatus;
import com.aaa.easyagent.biz.flow.runtime.FlowState;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.flow.EdgeKind;
import com.aaa.easyagent.core.domain.flow.FlowEdgeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.PortDefinition;
import com.aaa.easyagent.core.domain.flow.PortKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 基于有向无环图的动态调度器。
 * <p>
 * 控制边使用「待定 / 激活 / 跳过」三态传播：节点的所有控制入边都有结论且至少一条激活才执行，
 * 全部跳过则节点跳过并把跳过继续往下传。FORK 之后同时就绪的节点并发执行。
 * <p>
 * 失败、取消和超时都会进入收敛阶段：先停止调度新节点，再打断在飞节点并把它们的运行记录收口，
 * 不让 ea_agent_flow_node_run 永久停在 RUNNING，也不让落后的节点往已关闭的 SSE 写事件。
 */
@Slf4j
@Component
public class DefaultFlowEngine implements FlowEngine {

    private final FlowRunRecorder recorder;
    private final FlowOrchestrationProperties properties;
    private final Executor nodeExecutor;

    public DefaultFlowEngine(FlowRunRecorder recorder, FlowOrchestrationProperties properties,
                             @Qualifier(FlowExecutorConfig.FLOW_NODE_EXECUTOR) Executor nodeExecutor) {
        this.recorder = recorder;
        this.properties = properties;
        this.nodeExecutor = nodeExecutor;
    }

    /**
     * 执行一张编译后的图。
     * <p>
     * 1. 初始化节点与控制边状态，写运行审计，发出 FLOW_STARTED
     * 2. 循环调度：传播跳过、提交就绪节点、等待任一节点完成
     * 3. 节点成功则落数据、激活出边；失败或超时则进入收敛阶段并返回终态
     * 4. 没有节点可跑时判定是否到达 END，给出整体终态
     *
     * @param plan    编译产物，含节点、插件与按类型索引的边
     * @param context 本次运行上下文，承载取消标志与 SSE 出口
     * @return 整体执行结果，status 取值见 {@link FlowRunStatus}
     */
    @Override
    public FlowExecutionResult execute(CompiledFlowPlan plan, FlowRunContext context) {
        ExecutionSession session = initSession(plan, context);
        try {
            while (true) {
                // 1. 先看是否已被取消或整体超时，避免再往下调度新节点继续烧 token
                FlowExecutionResult interrupted = checkInterrupted(session);
                if (interrupted != null) {
                    return interrupted;
                }

                // 2. 传播跳过并提交所有就绪节点
                scheduleReadyNodes(session);
                if (session.running.isEmpty()) {
                    break;
                }

                // 3. 等待任一节点完成；等待间隔内没有结果则回到第 1 步重新检查取消与超时
                NodeTaskResult task = awaitNextCompletion(session);
                if (task == null) {
                    FlowExecutionResult timedOut = checkNodeTimeout(session);
                    if (timedOut != null) {
                        return timedOut;
                    }
                    continue;
                }

                // 4. 节点失败立即收敛整条 Flow；成功则落数据并激活出边
                if (task.error() != null) {
                    return failByNode(session, task);
                }
                applyNodeSuccess(session, task);
            }
            return finishByGraph(session);
        } catch (InterruptedException interruptedException) {
            // 调度线程被中断：恢复中断标志，不再做可中断的等待，直接放弃在飞节点
            Thread.currentThread().interrupt();
            String reason = "执行线程被中断";
            session.context.cancel(reason);
            abandonRunningNodes(session, FlowRunStatus.CANCELLED, reason);
            return finish(session, FlowRunStatus.CANCELLED, null, reason, FlowEventType.FLOW_FAILED);
        } catch (RuntimeException runtimeException) {
            log.error("Flow 执行异常，runId={}", session.context.getRunId(), runtimeException);
            session.context.cancel("调度异常");
            abandonRunningNodes(session, FlowRunStatus.CANCELLED, "上游调度异常，节点被取消");
            return finish(session, FlowRunStatus.FAILED, null, runtimeException.getMessage(), FlowEventType.FLOW_FAILED);
        }
    }

    /** 初始化本次运行的节点状态、控制边状态与审计记录。 */
    private ExecutionSession initSession(CompiledFlowPlan plan, FlowRunContext context) {
        ExecutionSession session = new ExecutionSession(plan, context, new FlowEventPublisher(context),
                new ExecutorCompletionService<>(nodeExecutor),
                System.currentTimeMillis() + properties.getFlowTimeoutSeconds() * 1000L);
        plan.getNodes().keySet().forEach(nodeId -> session.nodeStates.put(nodeId, NodeState.PENDING));
        plan.getDefinition().getEdges().stream()
                .filter(edge -> edge.getKind() == EdgeKind.CONTROL)
                .forEach(edge -> session.edgeStates.put(edge.getId(), EdgeState.PENDING));
        recorder.startFlow(context);
        session.events.publish(FlowEventType.FLOW_STARTED, null, null, context.getInput());
        return session;
    }

    /** 检查取消标志与 Flow 整体超时，命中则收敛并返回终态，否则返回 null 继续执行。 */
    private FlowExecutionResult checkInterrupted(ExecutionSession session) throws InterruptedException {
        if (session.context.isCancelled()) {
            return cancelSession(session, session.context.cancelReasonOrDefault());
        }
        if (System.currentTimeMillis() > session.flowDeadlineMillis) {
            String message = "Flow 执行超过整体超时 " + properties.getFlowTimeoutSeconds() + " 秒";
            session.context.cancel(message);
            return cancelSession(session, message);
        }
        return null;
    }

    /**
     * 传播跳过状态并提交就绪节点。
     * <p>
     * 跳过不占线程，始终传播到底；就绪节点受单次运行并发上限约束，达到上限后留到下一轮提交。
     */
    private void scheduleReadyNodes(ExecutionSession session) {
        boolean progressed;
        do {
            progressed = false;
            for (FlowNodeDefinition node : session.plan.getNodes().values()) {
                if (session.nodeStates.get(node.getId()) != NodeState.PENDING) {
                    continue;
                }
                Readiness readiness = readiness(session, node);
                if (readiness == Readiness.SKIP) {
                    skipNode(session, node);
                    progressed = true;
                } else if (readiness == Readiness.READY
                        && session.running.size() < properties.getMaxParallelismPerRun()) {
                    submitNode(session, node);
                    progressed = true;
                }
            }
        } while (progressed);
    }

    /** 节点所有控制入边都被跳过：标记跳过，并把跳过继续传给它的控制出边。 */
    private void skipNode(ExecutionSession session, FlowNodeDefinition node) {
        session.nodeStates.put(node.getId(), NodeState.SKIPPED);
        session.plan.outgoing(EdgeKind.CONTROL, node.getId()).forEach(edge ->
                session.edgeStates.replace(edge.getId(), EdgeState.PENDING, EdgeState.SKIPPED));
        session.events.publish(FlowEventType.NODE_SKIPPED, node, agentId(node), "上游分支未激活");
    }

    /**
     * 提交一个就绪节点。
     * <p>
     * 入参收集、运行记录创建和 NODE_STARTED 事件都在调度线程完成，这样失败收敛时能拿到
     * nodeRunId 把在飞节点收口；节点线程只负责执行插件。
     */
    private void submitNode(ExecutionSession session, FlowNodeDefinition node) {
        Map<String, Object> inputs = collectInputs(session, node);
        session.nodeStates.put(node.getId(), NodeState.RUNNING);
        Long nodeRunId = recorder.startNode(session.context, node, agentId(node), inputs);
        session.events.publish(FlowEventType.NODE_READY, node, agentId(node), null);
        session.events.publish(FlowEventType.NODE_STARTED, node, agentId(node), inputs);
        Future<NodeTaskResult> future = session.completion.submit(
                () -> runNodePlugin(session, node, nodeRunId, inputs));
        long deadline = System.currentTimeMillis() + properties.getNodeTimeoutSeconds() * 1000L;
        session.running.put(node.getId(), new RunningNode(node, nodeRunId, future, deadline));
    }

    /** 在节点线程里执行插件，任何异常都收进结果对象，由调度线程统一处理。 */
    private NodeTaskResult runNodePlugin(ExecutionSession session, FlowNodeDefinition node,
                                         Long nodeRunId, Map<String, Object> inputs) {
        try {
            requireDataInputs(node, inputs);
            NodeExecutionContext nodeContext = NodeExecutionContext.builder()
                    .node(node).inputs(inputs).state(session.state).runContext(session.context)
                    .events(session.events).outgoingEdges(allOutgoing(session.plan, node.getId()))
                    .nodeResolver(session.plan.getNodes()::get)
                    .workerInvoker((workerId, workerInput) -> executeWorker(session, workerId, workerInput))
                    .build();
            return new NodeTaskResult(node, nodeRunId, session.plan.getPlugins().get(node.getId()).execute(nodeContext), null);
        } catch (Throwable error) {
            return new NodeTaskResult(node, nodeRunId, null, error);
        }
    }

    /** 按配置的间隔等待任一节点完成；返回 null 表示本轮没有节点完成。 */
    private NodeTaskResult awaitNextCompletion(ExecutionSession session) throws InterruptedException {
        Future<NodeTaskResult> future = session.completion.poll(properties.getPollIntervalMillis(), TimeUnit.MILLISECONDS);
        return resolve(future);
    }

    /** 节点成功：落数据、累加 token、收口审计、激活控制出边。 */
    private void applyNodeSuccess(ExecutionSession session, NodeTaskResult task) {
        FlowNodeDefinition node = task.node();
        session.running.remove(node.getId());
        NodeExecutionResult nodeResult = task.result();
        nodeResult.getOutputs().forEach((portKey, value) -> session.state.put(node.getId(), portKey, value));
        session.inputTokens += nodeResult.getInputTokens();
        session.outputTokens += nodeResult.getOutputTokens();
        session.nodeStates.put(node.getId(), NodeState.SUCCEEDED);
        recorder.finishNode(task.nodeRunId(), FlowRunStatus.SUCCEEDED, nodeResult.getOutputs(),
                nodeResult.getInputTokens(), nodeResult.getOutputTokens(), null);
        session.events.publish(FlowEventType.NODE_SUCCEEDED, node, agentId(node), nodeResult.getOutputs());
        activateOutgoing(session, node, nodeResult);
        // 多个 END 节点时以第一个到达的为最终答案
        if (FlowNodeTypes.END.equals(node.getType())) {
            session.endReached = true;
            if (session.finalResult == null) {
                session.finalResult = nodeResult.primaryOutput();
            }
        }
    }

    /** 节点失败：收口该节点，打断其余在飞节点，整条 Flow 判定失败。 */
    private FlowExecutionResult failByNode(ExecutionSession session, NodeTaskResult task) throws InterruptedException {
        FlowNodeDefinition node = task.node();
        session.running.remove(node.getId());
        session.nodeStates.put(node.getId(), NodeState.FAILED);
        String message = errorMessage(task.error());
        log.error("Flow 节点执行失败，runId={}, nodeId={}", session.context.getRunId(), node.getId(), task.error());
        recorder.finishNode(task.nodeRunId(), FlowRunStatus.FAILED, null, 0, 0, message);
        session.events.publish(FlowEventType.NODE_FAILED, node, agentId(node), message);
        session.context.cancel("上游节点失败：" + node.getId());
        drainRunningNodes(session, FlowRunStatus.CANCELLED, "同批次节点失败，本节点被取消");
        return finish(session, FlowRunStatus.FAILED, null, message, FlowEventType.FLOW_FAILED);
    }

    /** 检查在飞节点是否超过单节点超时，命中则按失败收敛。 */
    private FlowExecutionResult checkNodeTimeout(ExecutionSession session) throws InterruptedException {
        long now = System.currentTimeMillis();
        RunningNode expired = session.running.values().stream()
                .filter(running -> now > running.deadlineMillis())
                .findFirst().orElse(null);
        if (expired == null) {
            return null;
        }
        String message = "节点执行超过单节点超时 " + properties.getNodeTimeoutSeconds() + " 秒";
        log.warn("Flow 节点超时，runId={}, nodeId={}", session.context.getRunId(), expired.node().getId());
        session.running.remove(expired.node().getId());
        session.nodeStates.put(expired.node().getId(), NodeState.FAILED);
        expired.future().cancel(true);
        recorder.finishNode(expired.nodeRunId(), FlowRunStatus.FAILED, null, 0, 0, message);
        session.events.publish(FlowEventType.NODE_FAILED, expired.node(), agentId(expired.node()), message);
        session.context.cancel("节点超时：" + expired.node().getId());
        drainRunningNodes(session, FlowRunStatus.CANCELLED, "同批次节点超时，本节点被取消");
        return finish(session, FlowRunStatus.FAILED, null, message, FlowEventType.FLOW_FAILED);
    }

    /** 没有节点可以继续执行：到达 END 则成功，否则说明图被分支截断。 */
    private FlowExecutionResult finishByGraph(ExecutionSession session) {
        if (!session.endReached) {
            String message = "Flow 执行结束但没有到达 END 节点";
            return finish(session, FlowRunStatus.FAILED, null, message, FlowEventType.FLOW_FAILED);
        }
        return finish(session, FlowRunStatus.SUCCEEDED, session.finalResult, null, FlowEventType.FLOW_SUCCEEDED);
    }

    /** 取消收敛：打断在飞节点并按取消收口。 */
    private FlowExecutionResult cancelSession(ExecutionSession session, String reason) throws InterruptedException {
        drainRunningNodes(session, FlowRunStatus.CANCELLED, reason);
        return finish(session, FlowRunStatus.CANCELLED, null, reason, FlowEventType.FLOW_FAILED);
    }

    /**
     * 收敛在飞节点。
     * <p>
     * 1. 打断所有在飞节点线程
     * 2. 有界等待它们退出，期间按真实结果收口审计
     * 3. 等待超时仍未退出的，按取消收口，并在错误信息里标明可能仍在后台运行
     * <p>
     * 说明：大模型 HTTP 调用不一定响应线程中断，这里保证的是审计不残留、不再调度新节点、
     * 不再往 SSE 写事件，而不是瞬间掐断已经发出的那一次模型请求。
     */
    private void drainRunningNodes(ExecutionSession session, FlowRunStatus status, String message)
            throws InterruptedException {
        if (session.running.isEmpty()) {
            return;
        }
        session.running.values().forEach(running -> running.future().cancel(true));
        long deadline = System.currentTimeMillis() + properties.getCancelDrainSeconds() * 1000L;
        while (!session.running.isEmpty()) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            NodeTaskResult task = resolve(session.completion.poll(remaining, TimeUnit.MILLISECONDS));
            if (task == null) {
                continue;
            }
            session.running.remove(task.node().getId());
            if (task.error() == null) {
                recorder.finishNode(task.nodeRunId(), FlowRunStatus.SUCCEEDED, task.result().getOutputs(),
                        task.result().getInputTokens(), task.result().getOutputTokens(), null);
            } else {
                recorder.finishNode(task.nodeRunId(), status, null, 0, 0, message);
            }
        }
        abandonRunningNodes(session, status, message + "（等待退出超时，节点可能仍在后台运行）");
    }

    /**
     * 不等待地放弃在飞节点：打断线程并按给定状态收口审计。
     * 调度线程已被中断或已经发生异常时走这条路，不能再做可中断的等待。
     */
    private void abandonRunningNodes(ExecutionSession session, FlowRunStatus status, String message) {
        session.running.values().forEach(running -> {
            log.warn("Flow 节点被放弃等待，runId={}, nodeId={}",
                    session.context.getRunId(), running.node().getId());
            running.future().cancel(true);
            recorder.finishNode(running.nodeRunId(), status, null, 0, 0, message);
        });
        session.running.clear();
    }

    /** 写 Flow 终态审计、发出终态事件并关闭事件出口。 */
    private FlowExecutionResult finish(ExecutionSession session, FlowRunStatus status, Object value,
                                       String message, FlowEventType eventType) {
        recorder.finishFlow(session.context, status, value, session.inputTokens, session.outputTokens, message);
        session.events.publishTerminal(eventType,
                eventType == FlowEventType.FLOW_SUCCEEDED ? value : message);
        return new FlowExecutionResult().setRunId(session.context.getRunId()).setStatus(status.code())
                .setResult(value).setState(session.state.snapshot())
                .setInputTokens(session.inputTokens).setOutputTokens(session.outputTokens);
    }

    /**
     * 以 Worker 身份执行一个 AGENT 节点，由 SUPERVISOR 在工具回调里同步调用。
     * 嵌套 SUPERVISOR 也能继续派发 Worker，不再因为 workerInvoker 为空而 NPE。
     */
    private NodeExecutionResult executeWorker(ExecutionSession session, String workerId,
                                              Map<String, Object> workerInput) {
        FlowNodeDefinition worker = session.plan.getNodes().get(workerId);
        if (worker == null) {
            throw new AgentException("Worker 节点不存在：" + workerId);
        }
        if (!FlowNodeTypes.AGENT.equals(worker.getType())) {
            throw new AgentException("Worker 端口只能连接 AGENT 节点：" + workerId);
        }
        Long nodeRunId = recorder.startNode(session.context, worker, agentId(worker), workerInput);
        session.events.publish(FlowEventType.NODE_STARTED, worker, agentId(worker), workerInput);
        try {
            NodeExecutionContext workerContext = NodeExecutionContext.builder().node(worker).inputs(workerInput)
                    .state(session.state).runContext(session.context).events(session.events)
                    .outgoingEdges(allOutgoing(session.plan, workerId))
                    .nodeResolver(session.plan.getNodes()::get)
                    .workerInvoker((nestedId, nestedInput) -> executeWorker(session, nestedId, nestedInput))
                    .build();
            NodeExecutionResult result = session.plan.getPlugins().get(workerId).execute(workerContext);
            result.getOutputs().forEach((portKey, value) -> session.state.put(workerId, portKey, value));
            recorder.finishNode(nodeRunId, FlowRunStatus.SUCCEEDED, result.getOutputs(),
                    result.getInputTokens(), result.getOutputTokens(), null);
            session.events.publish(FlowEventType.NODE_SUCCEEDED, worker, agentId(worker), result.getOutputs());
            return result;
        } catch (RuntimeException ex) {
            recorder.finishNode(nodeRunId, FlowRunStatus.FAILED, null, 0, 0, ex.getMessage());
            session.events.publish(FlowEventType.NODE_FAILED, worker, agentId(worker), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 判断节点是否可以执行。
     * <p>
     * 控制入边全部有结论且至少一条激活才能跑；全部跳过则跳过；还有待定的则继续等待。
     * 数据入边的来源节点必须已经结束（成功或跳过），否则等待其产出。
     */
    private Readiness readiness(ExecutionSession session, FlowNodeDefinition node) {
        List<FlowEdgeDefinition> incoming = session.plan.incoming(EdgeKind.CONTROL, node.getId());
        if (incoming.isEmpty()) {
            return FlowNodeTypes.START.equals(node.getType()) ? Readiness.READY : Readiness.WAIT;
        }
        boolean active = false;
        for (FlowEdgeDefinition edge : incoming) {
            EdgeState edgeState = session.edgeStates.getOrDefault(edge.getId(), EdgeState.PENDING);
            if (edgeState == EdgeState.PENDING) {
                return Readiness.WAIT;
            }
            if (edgeState == EdgeState.ACTIVE) {
                active = true;
            }
        }
        if (!active) {
            return Readiness.SKIP;
        }
        for (FlowEdgeDefinition edge : session.plan.incoming(EdgeKind.DATA, node.getId())) {
            NodeState sourceState = session.nodeStates.get(edge.getSource().getNodeId());
            if (sourceState == NodeState.PENDING || sourceState == NodeState.RUNNING) {
                return Readiness.WAIT;
            }
        }
        return Readiness.READY;
    }

    /** 按节点选择的分支激活控制出边：ROUTER 只激活命中分支，其余节点激活全部出边。 */
    private void activateOutgoing(ExecutionSession session, FlowNodeDefinition node, NodeExecutionResult result) {
        for (FlowEdgeDefinition edge : session.plan.outgoing(EdgeKind.CONTROL, node.getId())) {
            boolean active = result.getRouteKey() == null
                    || Objects.equals(result.getRouteKey(), edge.getSource().getPortKey());
            session.edgeStates.put(edge.getId(), active ? EdgeState.ACTIVE : EdgeState.SKIPPED);
        }
    }

    /** 按数据入边从运行状态里取出节点入参，multiple 端口聚合成列表，并补上端口默认值。 */
    private Map<String, Object> collectInputs(ExecutionSession session, FlowNodeDefinition node) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        Map<String, PortDefinition> ports = new LinkedHashMap<>();
        node.getInputPorts().forEach(port -> ports.put(port.getKey(), port));
        for (FlowEdgeDefinition edge : session.plan.incoming(EdgeKind.DATA, node.getId())) {
            Object value = session.state.get(edge.getSource().getNodeId(), edge.getSource().getPortKey());
            PortDefinition port = ports.get(edge.getTarget().getPortKey());
            if (port != null && port.isMultiple()) {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) inputs.computeIfAbsent(port.getKey(), key -> new ArrayList<>());
                if (value != null) {
                    values.add(value);
                }
            } else if (value != null) {
                inputs.put(edge.getTarget().getPortKey(), value);
            }
        }
        node.getInputPorts().stream().filter(port -> port.getDefaultValue() != null)
                .forEach(port -> inputs.putIfAbsent(port.getKey(), port.getDefaultValue()));
        return inputs;
    }

    /**
     * 校验必填数据入参。
     * <p>
     * 编译期只能保证端口连了线，运行期上游分支被跳过时值仍可能缺失。不校验的话
     * 插件会把 null 拼成字符串 "null" 发给大模型，拿到一个看似成功的垃圾结果。
     */
    private void requireDataInputs(FlowNodeDefinition node, Map<String, Object> inputs) {
        for (PortDefinition port : node.getInputPorts()) {
            if (port.getKind() != PortKind.DATA || !port.isRequired()) {
                continue;
            }
            Object value = inputs.get(port.getKey());
            // 只把「没有值」当缺失：空字符串是上游真实产出的结果，不在这里替用户判断
            boolean missing = value == null || (value instanceof List<?> values && values.isEmpty());
            if (missing) {
                throw new AgentException("节点必填输入缺失：" + node.getId() + "." + port.getName()
                        + "（上游分支未激活或未产出数据）");
            }
        }
    }

    private List<FlowEdgeDefinition> allOutgoing(CompiledFlowPlan plan, String nodeId) {
        List<FlowEdgeDefinition> edges = new ArrayList<>();
        for (EdgeKind kind : EdgeKind.values()) {
            edges.addAll(plan.outgoing(kind, nodeId));
        }
        return edges;
    }

    /** 取 Future 结果；节点任务自身不抛异常，取消导致的 CancellationException 视为无结果。 */
    private NodeTaskResult resolve(Future<NodeTaskResult> future) {
        if (future == null) {
            return null;
        }
        try {
            return future.get();
        } catch (CancellationException | ExecutionException ex) {
            log.warn("Flow 节点任务未返回结果", ex);
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private String errorMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private Long agentId(FlowNodeDefinition node) {
        if (node == null || node.getConfig() == null || node.getConfig().get("agentId") == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(node.getConfig().get("agentId")));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 单次 Flow 运行的可变状态集合，避免调度方法之间传一长串参数。 */
    private static final class ExecutionSession {
        private final CompiledFlowPlan plan;
        private final FlowRunContext context;
        private final FlowEventPublisher events;
        private final CompletionService<NodeTaskResult> completion;
        private final long flowDeadlineMillis;
        private final FlowState state = new FlowState();
        private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();
        private final Map<String, EdgeState> edgeStates = new ConcurrentHashMap<>();
        /** 已提交未收口的节点，key 为 nodeId；只由调度线程读写 */
        private final Map<String, RunningNode> running = new LinkedHashMap<>();
        private long inputTokens;
        private long outputTokens;
        private Object finalResult;
        private boolean endReached;

        private ExecutionSession(CompiledFlowPlan plan, FlowRunContext context, FlowEventPublisher events,
                                 CompletionService<NodeTaskResult> completion, long flowDeadlineMillis) {
            this.plan = plan;
            this.context = context;
            this.events = events;
            this.completion = completion;
            this.flowDeadlineMillis = flowDeadlineMillis;
        }
    }

    /** 控制边三态 */
    private enum EdgeState {PENDING, ACTIVE, SKIPPED}

    /** 节点调度状态 */
    private enum NodeState {PENDING, RUNNING, SUCCEEDED, SKIPPED, FAILED}

    /** 节点就绪判定结果 */
    private enum Readiness {WAIT, READY, SKIP}

    /** 已提交的节点及其超时时间 */
    private record RunningNode(FlowNodeDefinition node, Long nodeRunId,
                               Future<NodeTaskResult> future, long deadlineMillis) {
    }

    /** 节点执行结果或异常 */
    private record NodeTaskResult(FlowNodeDefinition node, Long nodeRunId,
                                  NodeExecutionResult result, Throwable error) {
    }
}
