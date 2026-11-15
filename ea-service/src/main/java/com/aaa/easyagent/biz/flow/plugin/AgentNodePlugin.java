package com.aaa.easyagent.biz.flow.plugin;

import com.aaa.easyagent.biz.agent.BaseAgent;
import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.context.AgentContextFactory;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.trace.ChatTraceRecorder;
import com.aaa.easyagent.biz.agent.trace.ChatTraceSource;
import com.aaa.easyagent.biz.flow.event.FlowNodeSseEmitter;
import com.aaa.easyagent.biz.flow.node.FlowNodePlugin;
import com.aaa.easyagent.biz.flow.node.FlowNodeTypes;
import com.aaa.easyagent.biz.flow.node.NodeExecutionContext;
import com.aaa.easyagent.biz.flow.node.NodeExecutionResult;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.flow.*;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Agent 节点：按配置的 agentId 调用一个已有 Agent，节点输入渲染成这个 Agent 的提问。 */
@Component
@RequiredArgsConstructor
public class AgentNodePlugin implements FlowNodePlugin {
    private final AgentManagerService agentManagerService;
    private final AgentContextFactory contextFactory;
    private final FlowTemplateRenderer templateRenderer;

    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata(FlowNodeTypes.AGENT, "Agent", "Agent", "调用已有 Agent", "RobotOutlined",
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

    /**
     * 执行节点。
     * <p>
     * 1. 取出并校验配置的 Agent
     * 2. 把节点输入渲染成提问：配了输入模板走模板，否则取 input 端口的值
     * 3. 构建 Agent 上下文，挂上节点级 SSE 出口与编排的取消信号
     * 4. 按 Agent 自身的运行模式执行，空结果视为节点失败
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        EaAgentResult agent = requireAgent(context.getNode());
        String question = resolveQuestion(context);
        AgentContext agentContext = buildAgentContext(context, agent);
        BaseAgent executor = agentContext.getToolRunMode() == ToolRunMode.ReAct
                ? new ReActAgentExecutor(agentContext) : new ToolAgentExecutor(agentContext);
        executor.setOwnSse(false);
        executor.setSaveRecord(false);
        String output = executor.exec(question);
        // BaseAgent 在模型异常、卡住或决策轮数耗尽时返回 null。
        // 旧实现把 null 兜成 ""，节点会被记成执行成功并把空串传给下游，这里按失败处理。
        if (output == null) {
            throw new AgentException("Agent 节点未产出结果：" + agent.getAgentName()
                    + "（模型异常、重复调用卡住或决策轮数耗尽）");
        }
        return new NodeExecutionResult().setOutputs(Map.of("result", output))
                .setInputTokens(executor.getAccumulateInputTokenCount())
                .setOutputTokens(executor.getAccumulateOutputTokenCount());
    }

    private EaAgentResult requireAgent(FlowNodeDefinition node) {
        long agentId = Long.parseLong(String.valueOf(node.getConfig().get("agentId")));
        EaAgentResult agent = agentManagerService.getAgent(agentId);
        if (agent == null) {
            throw new AgentException("Agent 不存在：" + agentId);
        }
        return agent;
    }

    /**
     * 组装发给 Agent 的提问。
     * <p>
     * 配了 inputTemplate 就按模板渲染全部入参（此前这个配置项声明了却从未使用）；
     * 没配模板时只取 input 端口，不再把整个入参 Map 字符串化当提问。
     */
    private String resolveQuestion(NodeExecutionContext context) {
        String inputTemplate = String.valueOf(context.getNode().getConfig().getOrDefault("inputTemplate", ""));
        if (!inputTemplate.isBlank()) {
            return templateRenderer.render(inputTemplate, context.getInputs());
        }
        Object input = context.getInputs().get("input");
        if (input == null) {
            throw new AgentException("Agent 节点缺少输入：" + context.getNode().getId()
                    + "（请连接 input 端口或配置输入模板）");
        }
        return stringifyInput(input);
    }

    /**
     * 把 input 端口的值转成提问文本。
     * <p>
     * input 是 multiple 端口，连一条线时拿到的是单元素列表，必须解包，否则发给模型的是
     * ["问题"] 这种带括号的字面量；连多条线时按行拼接，保留每个上游的完整输出。
     */
    private String stringifyInput(Object input) {
        if (input instanceof String text) {
            return text;
        }
        if (input instanceof List<?> values) {
            if (values.size() == 1) {
                return stringifyInput(values.get(0));
            }
            return values.stream().map(this::stringifyInput).collect(Collectors.joining("\n\n"));
        }
        return templateRenderer.render(null, Map.of("input", input));
    }

    /**
     * 取本节点的轨迹视图。
     * 编排级收集器为空（例如调试试运行不落库）时新建一个独立收集器，避免空指针。
     */
    private ChatTraceRecorder scopedRecorder(NodeExecutionContext context, EaAgentResult agent) {
        ChatTraceSource source = ChatTraceSource.ofFlowNode(
                agent.getId(), agent.getAgentName(), context.getNode().getId());
        ChatTraceRecorder runRecorder = context.getRunContext().getTraceRecorder();
        return runRecorder == null ? new ChatTraceRecorder(source) : runRecorder.scopedTo(source);
    }

    private AgentContext buildAgentContext(NodeExecutionContext context, EaAgentResult agent) {
        Object nodeImage = context.getInputs().get("image");
        String imageBase64 = nodeImage == null
                ? context.getRunContext().getImageBase64() : String.valueOf(nodeImage);
        // 取编排级收集器的节点视图：轨迹汇总到整轮对话，同时带上本节点标识
        ChatTraceRecorder nodeRecorder = scopedRecorder(context, agent);
        AgentContext agentContext = contextFactory.build(agent,
                new FlowNodeSseEmitter(context.getEvents(), context.getNode(), agent.getId()),
                context.getRunContext().nodeSessionId(context.getNode().getId()),
                imageBase64, context.getRunContext().isSyncMode(), nodeRecorder);
        agentContext.setFlowNodeId(context.getNode().getId());
        agentContext.setCancelledSupplier(context.getRunContext()::isCancelled);
        return agentContext;
    }
}
