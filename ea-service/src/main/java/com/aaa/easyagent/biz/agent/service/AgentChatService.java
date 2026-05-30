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
     * @param sessionId   会话 ID
     * @param question    问题
     * @param imageBase64 图片数据（Base64 Data URL 格式，可为 null）
     */
    void streamChatWith(String sessionId, String question, String agentId, SseEmitter sseEmitter, String imageBase64);
}
