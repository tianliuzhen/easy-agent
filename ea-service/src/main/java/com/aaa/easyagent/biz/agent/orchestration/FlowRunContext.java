package com.aaa.easyagent.biz.agent.orchestration;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Builder
public class FlowRunContext {
    private final String runId;
    private final Long flowId;
    private final Long revisionId;
    private final String sessionId;
    private final String imageBase64;
    private final boolean syncMode;
    private final boolean debug;
    private final SseEmitter sseEmitter;
    private final Executor executor;
    @Builder.Default
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    @Builder.Default
    private final Map<String, Object> input = Map.of();

    public String nodeSessionId(String nodeId) {
        return sessionId == null ? null : sessionId + ":" + nodeId;
    }
}
