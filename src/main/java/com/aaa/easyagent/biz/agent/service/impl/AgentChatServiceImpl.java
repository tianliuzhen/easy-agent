package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.ReActAgentXmlExecutor;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.agent.service.AgentChatService;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.service.ToolMangerService;
import com.aaa.easyagent.web.example.ReactAgentController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentChatServiceImpl.java  2026/1/25 11:29
 */
@Service
@RequiredArgsConstructor
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentManagerService agentManagerService;
    private final ToolMangerService toolMangerService;


    /**
     * @param conversationId
     * @param question
     * @param agentId
     * @param sseEmitter
     * @see ReactAgentController#testXml()
     */
    @Override
    public void streamChatWith(String conversationId, String question, String agentId, SseEmitter sseEmitter) {
        EaAgentResult agent = agentManagerService.getAgent(Long.valueOf(agentId));
        if (agent == null){
            sseEmitter.complete();
            return;
        }


        // agent 基础信息
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(agent.getId());
        agentContext.setAgentName(agent.getAgentName());

        // Agent来源
        agentContext.setModelType(ModelTypeEnum.getByModel(agent.getModelPlatform()));

        // 执行参数
        agentContext.setAgentModelConfig(agent.getAgentModelConfig());

        // 工具信息
        List<EaToolConfigResult> eaToolConfigResults = toolMangerService.getToolConfigByAgentId(agent.getId());
        List<ToolDefinition> toolDefinitions = eaToolConfigResults.stream().map(ToolDefinition::buildToolDefinition).toList();
        agentContext.setToolDefinitions(toolDefinitions);


        // 工具决策-tool
        agentContext.setToolRunMode(ToolRunMode.Tool);

        // sse
        agentContext.setSseEmitter(sseEmitter);

        new ReActAgentXmlExecutor(agentContext).exec(question);
    }
}
