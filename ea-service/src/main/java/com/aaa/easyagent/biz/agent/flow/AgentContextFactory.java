package com.aaa.easyagent.biz.agent.flow;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.McpToolIntegrationService;
import com.aaa.easyagent.core.service.ToolMangerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 由 {@link EaAgentResult} 构建运行期 {@link AgentContext}（含工具 + MCP 工具加载）。
 * <p>
 * 单 Agent 路径（AgentChatServiceImpl）与多 Agent 编排节点共用同一套构建逻辑，避免分叉。
 *
 * @author liuzhen.tian
 */
@Component
@RequiredArgsConstructor
public class AgentContextFactory {

    private final ToolMangerService toolMangerService;
    private final McpToolIntegrationService mcpToolIntegrationService;

    /**
     * @param agent       Agent 信息
     * @param sse         SSE 发射器，可为 null（同步模式）
     * @param sessionId   会话 ID
     * @param imageBase64 图片（多模态），仅首节点传入，可为 null
     * @param syncMode    是否同步模式（无流式订阅）
     */
    public AgentContext build(EaAgentResult agent, SseEmitter sse, String sessionId,
                              String imageBase64, boolean syncMode) {
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(agent.getId());
        agentContext.setAgentName(agent.getAgentName());
        agentContext.setPrompt(agent.getPrompt());
        agentContext.setAgentMemoryConfig(agent.getAgentMemoryConfig());
        agentContext.setImageBase64(imageBase64);
        if (syncMode) {
            agentContext.setWithStream(false);
        }

        agentContext.setModelType(ModelTypeEnum.getByModel(agent.getModelPlatform()));
        agentContext.setAgentModelConfig(agent.getAgentModelConfig());

        // 工具
        List<EaToolConfigResult> eaToolConfigResults = toolMangerService.listBoundToolsByAgentId(agent.getId());
        List<ToolDefinition> toolDefinitions = eaToolConfigResults.stream()
                .map(ToolDefinition::buildToolDefinition).collect(Collectors.toList());
        agentContext.setToolDefinitions(toolDefinitions);

        // MCP 工具
        List<ToolDefinition<?>> mcpToolDefinitions = mcpToolIntegrationService.getMcpToolsForAgent(agent.getId());
        agentContext.getToolDefinitions().addAll(mcpToolDefinitions);

        agentContext.setToolRunMode(ToolRunMode.getByMode(agent.getToolRunMode()));
        agentContext.setSseEmitter(sse);
        agentContext.setSessionId(sessionId);
        return agentContext;
    }
}
