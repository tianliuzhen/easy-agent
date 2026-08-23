package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.BaseAgent;
import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.context.AgentContextFactory;
import com.aaa.easyagent.biz.agent.orchestration.FlowNodeSseEmitter;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AgentNodePlugin implements FlowNodePlugin {
    private final AgentManagerService agentManagerService;
    private final AgentContextFactory contextFactory;

    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata("AGENT", "Agent", "Agent", "调用已有 Agent", "RobotOutlined",
                List.of(
                        NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, false, false),
                        NodeMetadataSupport.port("worker", "Worker 调用", PortKind.WORKER, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("input", "输入", PortKind.DATA, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("image", "图片", PortKind.DATA, ValueType.IMAGE, false, false)),
                List.of(
                        NodeMetadataSupport.port("control", "完成", PortKind.CONTROL, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("result", "输出", PortKind.DATA, ValueType.STRING, false, true)),
                Map.of("agentId", Map.of("type", "integer", "title", "Agent ID", "minimum", 1),
                        "inputTemplate", NodeMetadataSupport.stringField("输入模板", "可引用 {{ input }} 等变量")),
                List.of("agentId"));
    }

    @Override
    public void validate(FlowNodeDefinition node, FlowValidationResult result) {
        Object agentId = node.getConfig().get("agentId");
        if (!(agentId instanceof Number) && (agentId == null || String.valueOf(agentId).isBlank())) {
            result.addError("NODE", node.getId(), "AGENT_ID_EMPTY", "Agent 节点必须配置 agentId");
        }
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long agentId = Long.parseLong(String.valueOf(context.getNode().getConfig().get("agentId")));
        EaAgentResult agent = agentManagerService.getAgent(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在：" + agentId);
        }
        Object rawInput = context.getInputs().getOrDefault("input", context.getInputs());
        String question = rawInput == null ? "" : String.valueOf(rawInput);
        AgentContext agentContext = contextFactory.build(agent,
                new FlowNodeSseEmitter(context.getEvents(), context.getNode(), agentId),
                context.getRunContext().nodeSessionId(context.getNode().getId()),
                context.getInputs().get("image") == null ? context.getRunContext().getImageBase64()
                        : String.valueOf(context.getInputs().get("image")),
                context.getRunContext().isSyncMode());
        agentContext.setFlowNodeId(context.getNode().getId());
        BaseAgent executor = agentContext.getToolRunMode() == com.aaa.easyagent.core.domain.enums.ToolRunMode.ReAct
                ? new ReActAgentExecutor(agentContext) : new ToolAgentExecutor(agentContext);
        executor.setOwnSse(false);
        executor.setSaveRecord(false);
        String output = executor.exec(question);
        return new NodeExecutionResult().setOutputs(Map.of("result", output == null ? "" : output))
                .setInputTokens(executor.getAccumulateInputTokenCount())
                .setOutputTokens(executor.getAccumulateOutputTokenCount());
    }
}
