package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.ReActAgentXmlExecutor;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.agent.service.AgentChatService;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.service.ToolMangerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentChatServiceImpl.java  2026/1/25 11:29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentManagerService agentManagerService;
    private final ToolMangerService toolMangerService;


    /**
     * 流式对话
     *
     * @param sessionId  会话 ID
     * @param question   问题
     * @param agentId    Agent ID
     * @param sseEmitter SSE 发射器
     */
    @Override
    public void streamChatWith(String sessionId, String question, String agentId, SseEmitter sseEmitter) {
        EaAgentResult agent = agentManagerService.getAgent(Long.valueOf(agentId));
        if (agent == null) {
            sseEmitter.complete();
            return;
        }


        // agent 基础信息
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(agent.getId());
        agentContext.setAgentName(agent.getAgentName());
        agentContext.setPrompt(agent.getPrompt());

        // Agent来源
        agentContext.setModelType(ModelTypeEnum.getByModel(agent.getModelPlatform()));

        // 执行参数
        agentContext.setAgentModelConfig(agent.getAgentModelConfig());

        // 工具信息
        List<EaToolConfigResult> eaToolConfigResults = toolMangerService.listBoundToolsByAgentId(agent.getId());
        List<ToolDefinition> toolDefinitions = eaToolConfigResults.stream().map(ToolDefinition::buildToolDefinition).toList();
        agentContext.setToolDefinitions(toolDefinitions);


        // 工具决策-tool
        agentContext.setToolRunMode(ToolRunMode.Tool);

        // sse
        agentContext.setSseEmitter(sseEmitter);
        agentContext.setSessionId(sessionId);

        // 开始新的聊天会话并保存到数据库
        ChatRecordSaver.startNewConversation(agentContext, question);

        // 执行Agent
        String result = new ReActAgentXmlExecutor(agentContext).exec(question);

        // 保存聊天记录（注意：这里需要从Agent执行过程中获取思考过程和工具调用信息）
        // 实际的保存逻辑在ChatRecordSaverService中通过ThreadLocal收集
        log.info("Agent执行完成，结果长度: {}", result != null ? result.length() : 0);
    }
}
