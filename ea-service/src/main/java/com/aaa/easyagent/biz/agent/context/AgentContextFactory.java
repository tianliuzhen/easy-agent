package com.aaa.easyagent.biz.agent.context;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.service.ChatTurnRecorder;
import com.aaa.easyagent.biz.agent.trace.ChatTraceRecorder;
import com.aaa.easyagent.biz.agent.trace.ChatTraceSource;
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
    private final ChatTurnRecorder chatTurnRecorder;

    /**
     * 构建运行上下文。
     * <p>
     * 轨迹收集器与对话持久化入口都挂在上下文上随链路传递：BaseAgent 不是 Spring Bean，
     * 无法注入，原先靠 static 持有 service，既不可测也在 reactor 线程上失效。
     *
     * @param traceRecorder 本轮轨迹收集器。编排场景传入节点视图，使各节点轨迹汇总到同一轮对话；
     *                      传 null 则新建一个独立的收集器
     */
    public AgentContext build(EaAgentResult agent, SseEmitter sse, String sessionId,
                              String imageBase64, boolean syncMode,
                              ChatTraceRecorder traceRecorder) {
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
        context.setChatTurnRecorder(chatTurnRecorder);
        context.setTraceRecorder(traceRecorder != null ? traceRecorder
                : new ChatTraceRecorder(ChatTraceSource.ofAgent(agent.getId(), agent.getAgentName())));
        return context;
    }

    /** 不需要自定义轨迹收集器时的重载，内部按本 Agent 新建一个 */
    public AgentContext build(EaAgentResult agent, SseEmitter sse, String sessionId,
                              String imageBase64, boolean syncMode) {
        return build(agent, sse, sessionId, imageBase64, syncMode, null);
    }
}
