package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.BaseAgent;
import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.context.AgentContextFactory;
import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.orchestration.FlowNodeSseEmitter;
import com.aaa.easyagent.biz.agent.trace.ChatTraceRecorder;
import com.aaa.easyagent.biz.agent.trace.ChatTraceSource;
import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.flow.*;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 主管节点：把 Worker 边连接的 Agent 包装成工具交给主管 Agent，由模型自行决定调用哪个、调几次。
 * <p>
 * 工具名与说明取自 Worker 自己的 Agent 配置（Key / 名称 / 描述）。主管选谁完全依赖这两个字段，
 * 旧实现用 nodeId 当工具名（形如 agent_3f2a1b），等于让模型盲猜。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisorNodePlugin implements FlowNodePlugin {
    /** Worker 工具的入参 schema，只有一个 input 字段 */
    private static final String WORKER_INPUT_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"string\",\"description\":\"交给该 Agent 处理的完整任务描述\"}},\"required\":[\"input\"]}";

    private final AgentManagerService agentManagerService;
    private final AgentContextFactory contextFactory;

    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata(FlowNodeTypes.SUPERVISOR, "控制", "主管", "动态调度 Worker Agent", "ApartmentOutlined",
                List.of(NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, true, false),
                        NodeMetadataSupport.port("input", "问题", PortKind.DATA, ValueType.ANY, true, false)),
                List.of(NodeMetadataSupport.port("control", "完成", PortKind.CONTROL, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("worker", "Worker Agent", PortKind.WORKER, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("result", "最终回答", PortKind.DATA, ValueType.STRING, false, true)),
                Map.of("agentId", Map.of("type", "integer", "title", "主管 Agent ID", "minimum", 1),
                        "instruction", NodeMetadataSupport.stringField("主管指令", "说明何时调用不同 Worker")), List.of("agentId"));
    }

    @Override
    public void validate(FlowNodeDefinition node, FlowValidationResult result) {
        if (node.getConfig().get("agentId") == null) {
            result.addError("NODE", node.getId(), "SUPERVISOR_AGENT_EMPTY", "主管节点必须配置 agentId");
        }
    }

    /**
     * 执行节点。
     * <p>
     * 1. 取出主管 Agent
     * 2. 把每条 Worker 边上的 Agent 包装成一个工具回调，累计 Worker 消耗的 token
     * 3. 构建主管上下文，挂上节点级 SSE 出口与编排的取消信号
     * 4. 执行主管，token 合计 = 主管自身 + 全部 Worker，空结果视为节点失败
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        EaAgentResult supervisor = requireAgent(context.getNode().getConfig().get("agentId"), "主管 Agent");
        AtomicLong workerInputTokens = new AtomicLong();
        AtomicLong workerOutputTokens = new AtomicLong();
        List<FunctionCallback> callbacks = buildWorkerCallbacks(context, workerInputTokens, workerOutputTokens);
        if (callbacks.isEmpty()) {
            throw new AgentException("主管节点未连接任何 Worker Agent：" + context.getNode().getId());
        }

        // 取编排级收集器的节点视图，主管自身的思考与 Worker 派发都记在本节点名下
        ChatTraceSource source = ChatTraceSource.ofFlowNode(
                supervisor.getId(), supervisor.getAgentName(), context.getNode().getId());
        ChatTraceRecorder runRecorder = context.getRunContext().getTraceRecorder();
        ChatTraceRecorder nodeRecorder = runRecorder == null
                ? new ChatTraceRecorder(source) : runRecorder.scopedTo(source);
        AgentContext agentContext = contextFactory.build(supervisor,
                new FlowNodeSseEmitter(context.getEvents(), context.getNode(), supervisor.getId()),
                context.getRunContext().nodeSessionId(context.getNode().getId()), null,
                context.getRunContext().isSyncMode(), nodeRecorder);
        agentContext.setRuntimeCallbacks(callbacks);
        agentContext.setFlowNodeId(context.getNode().getId());
        agentContext.setCancelledSupplier(context.getRunContext()::isCancelled);

        BaseAgent executor = agentContext.getToolRunMode() == ToolRunMode.ReAct
                ? new ReActAgentExecutor(agentContext) : new ToolAgentExecutor(agentContext);
        executor.setOwnSse(false);
        executor.setSaveRecord(false);
        String output = executor.exec(String.valueOf(context.getInputs().get("input")));
        if (output == null) {
            throw new AgentException("主管节点未产出结果：" + supervisor.getAgentName()
                    + "（模型异常、重复调用卡住或决策轮数耗尽）");
        }
        return new NodeExecutionResult().setOutputs(Map.of("result", output))
                .setInputTokens(executor.getAccumulateInputTokenCount() + workerInputTokens.get())
                .setOutputTokens(executor.getAccumulateOutputTokenCount() + workerOutputTokens.get());
    }

    /** 把 Worker 边上的每个 Agent 包装成一个工具回调。 */
    private List<FunctionCallback> buildWorkerCallbacks(NodeExecutionContext context,
                                                        AtomicLong workerInputTokens,
                                                        AtomicLong workerOutputTokens) {
        List<FunctionCallback> callbacks = new ArrayList<>();
        Set<String> usedNames = new LinkedHashSet<>();
        for (FlowEdgeDefinition edge : context.getOutgoingEdges()) {
            if (edge.getKind() != EdgeKind.WORKER || edge.getTarget() == null) {
                continue;
            }
            String workerNodeId = edge.getTarget().getNodeId();
            FlowNodeDefinition workerNode = context.getNodeResolver() == null
                    ? null : context.getNodeResolver().apply(workerNodeId);
            EaAgentResult workerAgent = workerNode == null
                    ? null : requireAgent(workerNode.getConfig().get("agentId"), "Worker Agent");
            callbacks.add(new WorkerFunctionCallback(
                    uniqueToolName(workerAgent, workerNodeId, usedNames),
                    describeWorker(workerAgent, workerNode, workerNodeId),
                    workerNodeId, context, workerInputTokens, workerOutputTokens));
        }
        return callbacks;
    }

    /**
     * 生成工具名。
     * <p>
     * 优先用 Agent Key（本身就是标识符风格），否则退回节点 ID；同名时追加序号，
     * 保证模型看到的工具名唯一且可读。
     */
    private String uniqueToolName(EaAgentResult workerAgent, String workerNodeId, Set<String> usedNames) {
        String base = workerAgent == null ? null : workerAgent.getAgentKey();
        if (StringUtils.isBlank(base)) {
            base = workerNodeId;
        }
        String sanitized = base.replaceAll("[^a-zA-Z0-9_-]", "_");
        String name = sanitized;
        int sequence = 2;
        while (!usedNames.add(name)) {
            name = sanitized + "_" + sequence++;
        }
        return name;
    }

    /** 工具说明：Agent 名称 + 自身描述，模型靠这段文字判断该不该派活。 */
    private String describeWorker(EaAgentResult workerAgent, FlowNodeDefinition workerNode, String workerNodeId) {
        if (workerAgent == null) {
            return "调用 Worker Agent：" + workerNodeId;
        }
        String nodeName = workerNode == null || StringUtils.isBlank(workerNode.getName())
                ? workerAgent.getAgentName() : workerNode.getName();
        String description = StringUtils.isBlank(workerAgent.getAgentDesc())
                ? "无额外描述" : workerAgent.getAgentDesc();
        return "把任务交给【" + nodeName + "】处理。该 Agent 的职责：" + description;
    }

    private EaAgentResult requireAgent(Object agentIdValue, String role) {
        if (agentIdValue == null || String.valueOf(agentIdValue).isBlank()) {
            throw new AgentException(role + "未配置 agentId");
        }
        long agentId = Long.parseLong(String.valueOf(agentIdValue));
        EaAgentResult agent = agentManagerService.getAgent(agentId);
        if (agent == null) {
            throw new AgentException(role + "不存在：" + agentId);
        }
        return agent;
    }

    /** Worker 工具回调：解析模型给的入参，同步执行 Worker 节点，并把 token 累加回主管。 */
    @RequiredArgsConstructor
    private static class WorkerFunctionCallback implements FunctionCallback {
        private final String name;
        private final String description;
        private final String workerNodeId;
        private final NodeExecutionContext context;
        private final AtomicLong workerInputTokens;
        private final AtomicLong workerOutputTokens;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getInputTypeSchema() {
            return WORKER_INPUT_SCHEMA;
        }

        @Override
        public String call(String actionInput) {
            NodeExecutionResult result = context.getWorkerInvoker()
                    .apply(workerNodeId, Map.of("input", extractInput(actionInput)));
            workerInputTokens.addAndGet(result.getInputTokens());
            workerOutputTokens.addAndGet(result.getOutputTokens());
            return String.valueOf(result.primaryOutput());
        }

        /**
         * 取出模型填的 input 字段。
         * <p>
         * schema 声明的是 {"input": "..."}，旧实现把整段 JSON 原样当成 Worker 的提问，
         * Worker 收到的是 {"input":"帮我查天气"} 这串文本而不是真正的问题。
         * 入参不是 JSON 对象时按纯文本处理，兼容模型直接给一句话的情况。
         */
        private String extractInput(String actionInput) {
            if (StringUtils.isBlank(actionInput)) {
                return "";
            }
            String trimmed = actionInput.trim();
            if (!trimmed.startsWith("{")) {
                return trimmed;
            }
            try {
                Map<String, Object> arguments = JacksonUtil.jsonToMap(trimmed);
                Object input = arguments == null ? null : arguments.get("input");
                return input == null ? trimmed : String.valueOf(input);
            } catch (RuntimeException ex) {
                log.warn("Worker 工具入参解析失败，按纯文本处理，workerNodeId={}, input={}", workerNodeId, trimmed, ex);
                return trimmed;
            }
        }
    }
}
