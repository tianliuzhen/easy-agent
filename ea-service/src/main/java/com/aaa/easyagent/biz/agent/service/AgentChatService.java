package com.aaa.easyagent.biz.agent.service;

import com.aaa.easyagent.core.domain.request.StreamChatPostRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentChatService.java  2026/1/25 11:29
 */
public interface AgentChatService {


    /**
     * 智能体聊天。流式与同步共用：sseEmitter 不为 null 走流式输出；传 null 即同步模式。
     *
     * @param request    聊天请求参数（sessionId / msg / agentId / imageBase64）
     * @param sseEmitter SSE 发射器，传 null 表示同步模式
     * @return Agent 最终答案
     */
    String streamChatWith(StreamChatPostRequest request, SseEmitter sseEmitter);
}
