package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentModelConfig;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.agent.service.AgentChatService;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.request.StreamChatPostRequest;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.service.ChatRecordService;
import com.aaa.easyagent.core.service.McpToolIntegrationService;
import com.aaa.easyagent.core.service.ToolMangerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final McpToolIntegrationService mcpToolIntegrationService;
    private final ChatRecordService chatRecordService;

    /**
     * 非流式 SSE 发射器：缓冲所有 send 调用，在 complete 时一次性发送
     */
    private static class NonStreamingSseEmitter extends SseEmitter {

        private final SseEmitter delegate;
        private final List<SseEventBuilder> bufferedEvents = new ArrayList<>();
        private boolean completed = false;

        public NonStreamingSseEmitter(SseEmitter delegate) {
            super(delegate.getTimeout());
            this.delegate = delegate;
            // 设置完成回调，将委托的完成转发给原始发射器
            delegate.onCompletion(() -> {
                if (!completed) {
                    complete();
                }
            });
            delegate.onError(throwable -> {
                if (!completed) {
                    completeWithError(throwable);
                }
            });
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (!completed) {
                bufferedEvents.add(builder);
            }
        }

        @Override
        public void complete() {
            if (!completed) {
                completed = true;
                // 先发送一个"开始"标记（可选）
                // 再一次性发送所有缓冲的事件
                for (SseEventBuilder event : bufferedEvents) {
                    try {
                        delegate.send(event);
                    } catch (IOException e) {
                        log.error("非流式模式发送缓冲事件失败", e);
                        break;
                    }
                }
                bufferedEvents.clear();
                delegate.complete();
            }
        }

        @Override
        public void completeWithError(Throwable ex) {
            if (!completed) {
                completed = true;
                delegate.completeWithError(ex);
            }
        }
    }


    /**
     * 智能体聊天。流式与同步共用：sseEmitter 不为 null 走流式输出；传 null 即同步模式。
     *
     * @param request    聊天请求参数（sessionId / msg / agentId / imageBase64）
     * @param sseEmitter SSE 发射器，传 null 表示同步模式
     * @return Agent 最终答案
     */
    @Override
    public String streamChatWith(StreamChatPostRequest request, SseEmitter sseEmitter) {
        String sessionId = request.getSessionId();
        String question = request.getMsg();
        String agentId = request.getAgentId();
        String imageBase64 = request.getImageBase64();

        EaAgentResult agent = agentManagerService.getAgent(Long.valueOf(agentId));
        if (agent == null) {
            if (sseEmitter != null) {
                sseEmitter.complete();
            }
            return null;
        }

        // 同步模式（无 SSE）：关闭流式订阅，走 chatModel.call
        boolean syncMode = sseEmitter == null;

        // 根据 modelConfig 中的 streamEnabled 决定是否使用非流式模式
        AgentModelConfig modelConfig = agent.getAgentModelConfig();
        boolean streamEnabled = modelConfig == null || modelConfig.isStreamEnabled();

        // 如果关闭了流式输出，使用缓冲型 SseEmitter；同步模式下不创建 emitter
        SseEmitter effectiveEmitter = syncMode ? null : (streamEnabled ? sseEmitter : new NonStreamingSseEmitter(sseEmitter));


        // agent 基础信息
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(agent.getId());
        agentContext.setAgentName(agent.getAgentName());
        agentContext.setPrompt(agent.getPrompt());
        agentContext.setAgentMemoryConfig(agent.getAgentMemoryConfig());
        agentContext.setImageBase64(imageBase64);
        if (syncMode) {
            agentContext.setWithStream(false);
        }

        // Agent来源
        agentContext.setModelType(ModelTypeEnum.getByModel(agent.getModelPlatform()));

        // 执行参数
        agentContext.setAgentModelConfig(agent.getAgentModelConfig());

        // 工具信息
        List<EaToolConfigResult> eaToolConfigResults = toolMangerService.listBoundToolsByAgentId(agent.getId());
        List<ToolDefinition> toolDefinitions = eaToolConfigResults.stream().map(ToolDefinition::buildToolDefinition).collect(Collectors.toList());
        agentContext.setToolDefinitions(toolDefinitions);

        // 集成mcp
        List<ToolDefinition<?>> mcpToolDefinitions = mcpToolIntegrationService.getMcpToolsForAgent(agent.getId());
        agentContext.getToolDefinitions().addAll(mcpToolDefinitions);

        // 工具决策-tool
        ToolRunMode runMode = ToolRunMode.getByMode(agent.getToolRunMode());
        agentContext.setToolRunMode(runMode);

        // sse
        agentContext.setSseEmitter(effectiveEmitter);
        agentContext.setSessionId(sessionId);

        // 开始新的聊天会话并保存到数据库
        ChatRecordSaver.startNewConversation(agentContext, question);

        // 获取历史累计 Token 数
        EaChatConversationDO conversation = chatRecordService.getBySessionId(sessionId);
        long initInputTokens = conversation != null && conversation.getAccumulatedInputTokens() != null ? conversation.getAccumulatedInputTokens() : 0L;
        long initOutputTokens = conversation != null && conversation.getAccumulatedOutputTokens() != null ? conversation.getAccumulatedOutputTokens() : 0L;

        String result = null;
        // 执行Agent
        if (runMode == ToolRunMode.ReAct) {
            // 传统的ReAct模式
            result = new ReActAgentExecutor(agentContext).exec(question);
        }else {
            // 大模型的tool模式
            ToolAgentExecutor executor = new ToolAgentExecutor(agentContext, initInputTokens, initOutputTokens);
            result = executor.exec(question);
        }

        // 保存聊天记录（注意：这里需要从Agent执行过程中获取思考过程和工具调用信息）
        // 实际的保存逻辑在ChatRecordSaverService中通过ThreadLocal收集
        log.info("Agent执行完成，结果长度: {}", result != null ? result.length() : 0);
        return result;
    }
}
