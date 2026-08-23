package com.aaa.easyagent.biz.agent.context;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 单 Agent 与编排 Agent 共用的运行上下文工厂。 */
@Component
@RequiredArgsConstructor
public class AgentContextFactory {
    private final ToolMangerService toolMangerService;
    private final McpToolIntegrationService mcpToolIntegrationService;

    public AgentContext build(EaAgentResult agent, SseEmitter sse, String sessionId,
                              String imageBase64, boolean syncMode) {
        AgentContext context = new AgentContext();
        context.setAgentId(agent.getId());
        context.setAgentName(agent.getAgentName());
        context.setPrompt(agent.getPrompt());
        context.setAgentMemoryConfig(agent.getAgentMemoryConfig());
        context.setImageBase64(imageBase64);
        context.setWithStream(!syncMode);
        context.setModelType(ModelTypeEnum.getByModel(agent.getModelPlatform()));
        context.setAgentModelConfig(agent.getAgentModelConfig());

        List<EaToolConfigResult> tools = toolMangerService.listBoundToolsByAgentId(agent.getId());
        List<ToolDefinition> definitions = tools.stream()
                .map(ToolDefinition::buildToolDefinition).collect(Collectors.toCollection(ArrayList::new));
        List<ToolDefinition<?>> mcpDefinitions = mcpToolIntegrationService.getMcpToolsForAgent(agent.getId());
        definitions.addAll(mcpDefinitions);
        context.setToolDefinitions(definitions);

        context.setToolRunMode(ToolRunMode.getByMode(agent.getToolRunMode()));
        context.setSseEmitter(sse);
        context.setSessionId(sessionId);
        return context;
    }
}
