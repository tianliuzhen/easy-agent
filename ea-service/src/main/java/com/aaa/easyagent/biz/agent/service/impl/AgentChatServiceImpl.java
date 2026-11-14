package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ToolAgentExecutor;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentModelConfig;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.agent.context.AgentContextFactory;
import com.aaa.easyagent.biz.agent.orchestration.CompiledFlowPlan;
import com.aaa.easyagent.biz.agent.orchestration.FlowEngine;
import com.aaa.easyagent.biz.agent.orchestration.FlowExecutionResult;
import com.aaa.easyagent.biz.agent.orchestration.FlowRunContext;
import com.aaa.easyagent.biz.agent.orchestration.FlowRunStatus;
import com.aaa.easyagent.biz.agent.orchestration.FlowCompiler;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
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
import com.aaa.easyagent.core.service.FlowStudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private final FlowStudioService flowStudioService;
    private final FlowCompiler flowCompiler;
    private final FlowEngine flowEngine;
    private final AgentContextFactory agentContextFactory;

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

        // 多 Agent 编排路径（向后兼容：flowId 为空时走原单 Agent 路径）
        if (request.getFlowId() != null) {
            return execFlow(request, sseEmitter);
        }

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
        AgentContext agentContext = agentContextFactory.build(agent, effectiveEmitter, sessionId, imageBase64, syncMode);
        ToolRunMode runMode = agentContext.getToolRunMode();

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

    /** 多 Agent 图执行。会话首次运行时固定发布版本，最终答案按本轮 token 增量保存一次。 */
    private String execFlow(StreamChatPostRequest request, SseEmitter sseEmitter) {
        long startedAt = System.currentTimeMillis();
        String sessionId = request.getSessionId();
        String question = request.getMsg();
        String imageBase64 = request.getImageBase64();
        boolean syncMode = sseEmitter == null;

        EaChatConversationDO existing = chatRecordService.getBySessionId(sessionId);
        if (existing != null && existing.getFlowId() != null && !request.getFlowId().equals(existing.getFlowId())) {
            throw new IllegalArgumentException("当前会话属于其他 Flow，不能复用 sessionId：" + sessionId);
        }
        Long pinnedRevisionId = existing != null && request.getFlowId().equals(existing.getFlowId())
                ? existing.getFlowRevisionId() : null;
        FlowDefinition definition = pinnedRevisionId == null
                ? flowStudioService.published(request.getFlowId())
                : flowStudioService.revision(request.getFlowId(), pinnedRevisionId);
        CompiledFlowPlan plan = flowCompiler.compile(definition);
        AgentContext conversationContext = new AgentContext();
        Long firstAgentId = firstConfiguredAgentId(definition);
        if (firstAgentId == null) {
            // 整张图都没有 Agent 节点（例如纯模板图），会话不归属任何 Agent；
            // 旧实现兜底成 agentId=1，会把会话错记到某个真实 Agent 上。
            log.warn("Flow 未包含配置了 agentId 的节点，会话不归属 Agent，flowId={}", request.getFlowId());
        } else {
            conversationContext.setAgentId(firstAgentId);
        }
        conversationContext.setFlowId(request.getFlowId());
        conversationContext.setFlowRevisionId(definition.getRevisionId());
        conversationContext.setSessionId(sessionId);
        ChatRecordSaver.startNewConversation(conversationContext, question);
        FlowRunContext context = FlowRunContext.builder().runId(UUID.randomUUID().toString())
                .flowId(request.getFlowId()).revisionId(definition.getRevisionId()).sessionId(sessionId)
                .imageBase64(imageBase64).syncMode(syncMode).sseEmitter(sseEmitter)
                .input(Map.of("input", question, "question", question)).build();
        // 客户端断开后不再继续调度剩余节点，避免用户已经离开还在消耗模型额度
        if (sseEmitter != null) {
            sseEmitter.onCompletion(() -> context.cancel("会话连接已关闭"));
            sseEmitter.onTimeout(() -> context.cancel("会话连接超时"));
            sseEmitter.onError(error -> context.cancel("会话连接异常：" + error.getMessage()));
        }
        FlowExecutionResult result = flowEngine.execute(plan, context);
        if (sseEmitter != null) sseEmitter.complete();
        // 执行未成功不再静默返回 null。
        // 失败一定要让调用方知道；取消通常是客户端自己断开，SSE 侧已经没人接收，只记日志不再抛异常，
        // 但同步调用（无 SSE，取消只可能来自整体超时）仍需要抛出，否则调用方拿到 null 无从判断。
        if (!FlowRunStatus.SUCCEEDED.code().equals(result.getStatus())) {
            ChatRecordSaver.clearThreadLocal();
            log.warn("Flow 执行未成功，flowId={}, runId={}, status={}",
                    request.getFlowId(), result.getRunId(), result.getStatus());
            boolean cancelled = FlowRunStatus.CANCELLED.code().equals(result.getStatus());
            if (cancelled && !syncMode) {
                return null;
            }
            throw new AgentException("多 Agent 编排执行" + (cancelled ? "已取消" : "失败")
                    + "，flowId=" + request.getFlowId() + "，runId=" + result.getRunId());
        }
        String answer = result.getResult() == null ? "" : String.valueOf(result.getResult());
        ChatRecordSaver.addFinalAnswer(answer);
        AgentFinish finish = new AgentFinish();
        finish.setResult(answer);
        ChatRecordSaver.saveAgentFinish(finish,
                "flow:" + request.getFlowId() + "@" + definition.getRevisionId(),
                result.getInputTokens(), result.getOutputTokens(),
                BigDecimal.valueOf(System.currentTimeMillis() - startedAt));
        return answer;
    }

    /**
     * 取图里第一个配置了 agentId 的节点，用于会话记录归属。
     * 一张图可以完全由 TEMPLATE 等非 Agent 节点组成，此时没有可归属的 agentId。
     */
    private Long firstConfiguredAgentId(FlowDefinition definition) {
        return definition.getNodes().stream()
                .map(node -> node.getConfig().get("agentId"))
                .filter(Objects::nonNull)
                .map(value -> Long.valueOf(String.valueOf(value)))
                .findFirst()
                .orElse(null);
    }
}
