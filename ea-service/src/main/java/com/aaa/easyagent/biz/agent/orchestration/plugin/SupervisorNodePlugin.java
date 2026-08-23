package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.BaseAgent;
import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.context.AgentContextFactory;
import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.orchestration.FlowNodeSseEmitter;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SupervisorNodePlugin implements FlowNodePlugin {
    private final AgentManagerService agentManagerService;
    private final AgentContextFactory contextFactory;

    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata("SUPERVISOR", "控制", "主管", "动态调度 Worker Agent", "ApartmentOutlined",
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

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long agentId = Long.parseLong(String.valueOf(context.getNode().getConfig().get("agentId")));
        EaAgentResult supervisor = agentManagerService.getAgent(agentId);
        if (supervisor == null) throw new IllegalArgumentException("主管 Agent 不存在：" + agentId);
        List<FunctionCallback> callbacks = new ArrayList<>();
        java.util.concurrent.atomic.AtomicLong workerInputTokens = new java.util.concurrent.atomic.AtomicLong();
        java.util.concurrent.atomic.AtomicLong workerOutputTokens = new java.util.concurrent.atomic.AtomicLong();
        context.getOutgoingEdges().stream().filter(e -> e.getKind() == EdgeKind.WORKER).forEach(edge -> {
            String workerNodeId = edge.getTarget().getNodeId();
            callbacks.add(new FunctionCallback() {
                public String getName() { return workerNodeId; }
                public String getDescription() { return "调用 Worker Agent：" + workerNodeId; }
                public String getInputTypeSchema() { return "{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"string\"}}}"; }
                public String call(String actionInput) {
                    NodeExecutionResult result = context.getWorkerInvoker().apply(workerNodeId, Map.of("input", actionInput));
                    workerInputTokens.addAndGet(result.getInputTokens());
                    workerOutputTokens.addAndGet(result.getOutputTokens());
                    return String.valueOf(result.primaryOutput());
                }
            });
        });
        AgentContext agentContext = contextFactory.build(supervisor,
                new FlowNodeSseEmitter(context.getEvents(), context.getNode(), agentId),
                context.getRunContext().nodeSessionId(context.getNode().getId()), null,
                context.getRunContext().isSyncMode());
        agentContext.setRuntimeCallbacks(callbacks);
        BaseAgent executor = agentContext.getToolRunMode() == com.aaa.easyagent.core.domain.enums.ToolRunMode.ReAct
                ? new ReActAgentExecutor(agentContext) : new ToolAgentExecutor(agentContext);
        executor.setOwnSse(false);
        executor.setSaveRecord(false);
        String output = executor.exec(String.valueOf(context.getInputs().get("input")));
        return new NodeExecutionResult().setOutputs(Map.of("result", output == null ? "" : output))
                .setInputTokens(executor.getAccumulateInputTokenCount() + workerInputTokens.get())
                .setOutputTokens(executor.getAccumulateOutputTokenCount() + workerOutputTokens.get());
    }
}
