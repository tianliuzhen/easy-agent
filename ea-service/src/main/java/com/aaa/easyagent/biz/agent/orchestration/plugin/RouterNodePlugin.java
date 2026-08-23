package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.common.llm.LLmModelSelector;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.core.domain.flow.*;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RouterNodePlugin implements FlowNodePlugin {
    private final AgentManagerService agentManagerService;

    @Override
    public NodeMetadata metadata() {
        Map<String, Object> branchItems = Map.of(
                "type", "object",
                "properties", Map.of(
                        "key", Map.of("type", "string", "title", "分支 Key"),
                        "label", Map.of("type", "string", "title", "分支名称"),
                        "description", Map.of("type", "string", "title", "匹配说明")),
                "required", List.of("key", "label"));
        return NodeMetadataSupport.metadata("ROUTER", "控制", "路由", "使用 Agent 选择一个出口", "BranchesOutlined",
                List.of(NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, true, false),
                        NodeMetadataSupport.port("input", "问题", PortKind.DATA, ValueType.ANY, true, false)),
                List.of(NodeMetadataSupport.port("route", "分支 Key", PortKind.DATA, ValueType.STRING, false, true)),
                Map.of("agentId", Map.of("type", "integer", "title", "路由 Agent ID", "minimum", 1),
                        "instruction", NodeMetadataSupport.stringField("路由指令", "告诉模型如何选择分支"),
                        "branches", Map.of("type", "array", "title", "路由分支", "items", branchItems,
                                "default", List.of(
                                        Map.of("key", "branch_a", "label", "分支 A", "description", "第一类任务"),
                                        Map.of("key", "branch_b", "label", "分支 B", "description", "第二类任务"))),
                        "fallback", NodeMetadataSupport.stringField("fallback 分支 Key", "无法识别时使用的分支")),
                List.of("agentId", "branches"));
    }

    @Override
    public void prepare(FlowNodeDefinition node) {
        List<PortDefinition> outputs = new ArrayList<>();
        outputs.add(NodeMetadataSupport.port("route", "分支 Key", PortKind.DATA, ValueType.STRING, false, true));
        for (Branch branch : branches(node)) {
            outputs.add(NodeMetadataSupport.port(branch.key(), branch.label(), PortKind.CONTROL,
                    ValueType.ANY, false, true));
        }
        node.setOutputPorts(outputs);
    }

    @Override
    public void validate(FlowNodeDefinition node, FlowValidationResult result) {
        if (node.getConfig().get("agentId") == null) {
            result.addError("NODE", node.getId(), "ROUTER_AGENT_EMPTY", "路由节点必须配置 agentId");
        }
        List<Branch> branches = branches(node);
        if (branches.size() < 2) {
            result.addError("NODE", node.getId(), "ROUTER_BRANCHES", "路由节点至少需要两个有效分支");
        }
        Set<String> keys = new HashSet<>();
        for (Branch branch : branches) {
            if ("route".equals(branch.key())) {
                result.addError("NODE", node.getId(), "ROUTER_BRANCH_RESERVED", "分支 Key 不能使用保留字 route");
            } else if (!keys.add(branch.key())) {
                result.addError("NODE", node.getId(), "ROUTER_BRANCH_DUPLICATE", "路由分支 Key 重复：" + branch.key());
            }
        }
        String fallback = String.valueOf(node.getConfig().getOrDefault("fallback", ""));
        if (!fallback.isBlank() && branches.stream().noneMatch(branch -> branch.key().equals(fallback))) {
            result.addError("NODE", node.getId(), "ROUTER_FALLBACK", "fallback 必须对应一个路由分支");
        }
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long agentId = Long.parseLong(String.valueOf(context.getNode().getConfig().get("agentId")));
        EaAgentResult agent = agentManagerService.getAgent(agentId);
        if (agent == null) throw new IllegalArgumentException("路由 Agent 不存在：" + agentId);
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(agent.getId());
        agentContext.setAgentName(agent.getAgentName());
        agentContext.setPrompt(agent.getPrompt());
        agentContext.setModelType(com.aaa.easyagent.core.domain.enums.ModelTypeEnum.getByModel(agent.getModelPlatform()));
        agentContext.setAgentModelConfig(agent.getAgentModelConfig());
        agentContext.setWithStream(false);
        ChatModel model = LLmModelSelector.buildChatModel(agentContext);
        String instruction = String.valueOf(context.getNode().getConfig().getOrDefault("instruction", "从可用分支中选择一个最合适的 key"));
        List<Branch> branches = branches(context.getNode());
        List<String> keys = branches.stream().map(Branch::key).toList();
        String branchPrompt = branches.stream().map(branch -> branch.key() + "：" + branch.description())
                .reduce((left, right) -> left + "\n" + right).orElse("");
        String system = instruction + "\n可选分支：\n" + branchPrompt + "\n只输出分支 key。";
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(system));
        messages.add(new UserMessage(String.valueOf(context.getInputs().get("input"))));
        String choice = ChatResponseUtil.getResStr(model.call(new Prompt(messages))).trim();
        String fallback = String.valueOf(context.getNode().getConfig().getOrDefault("fallback", keys.isEmpty() ? "" : keys.get(0)));
        String route = keys.stream().filter(choice::contains).findFirst().orElse(fallback);
        return new NodeExecutionResult().setRouteKey(route).setOutputs(Map.of("route", route));
    }

    private List<Branch> branches(FlowNodeDefinition node) {
        Object value = node.getConfig().get("branches");
        if (!(value instanceof Collection<?> collection)) return List.of();
        List<Branch> result = new ArrayList<>();
        for (Object item : collection) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String key = String.valueOf(map.containsKey("key") ? map.get("key") : "").trim();
            if (key.isBlank()) continue;
            String label = String.valueOf(map.containsKey("label") ? map.get("label") : key).trim();
            String description = String.valueOf(map.containsKey("description") ? map.get("description") : label).trim();
            result.add(new Branch(key, label.isBlank() ? key : label,
                    description.isBlank() ? label : description));
        }
        return result;
    }

    private record Branch(String key, String label, String description) {
    }
}
