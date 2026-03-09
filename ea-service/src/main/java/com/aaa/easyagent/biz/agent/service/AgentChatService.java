package com.aaa.easyagent.biz.agent.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentChatService.java  2026/1/25 11:29
 */
public interface AgentChatService {


    /**
     * 智能体聊天
     *
     * @param sessionId 会话 ID
     * @param question 问题
     */
    void streamChatWith(String sessionId, String question, String agentId, SseEmitter sseEmitter);
}
