package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class FlowEventPublisher {
    private final FlowRunContext context;

    public void publish(FlowEventType eventType, FlowNodeDefinition node, Long agentId, Object data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", eventType.clientType());
        event.put("eventType", eventType.eventName());
        event.put("runId", context.getRunId());
        event.put("flowId", context.getFlowId());
        event.put("revisionId", context.getRevisionId());
        if (node != null) {
            event.put("nodeId", node.getId());
            event.put("nodeType", node.getType());
            event.put("nodeName", node.getName());
        }
        if (agentId != null) {
            event.put("agentId", agentId);
        }
        event.put("data", data);
        event.put("content", data instanceof String ? data : JacksonUtil.beanToStr(data));
        event.put("timestamp", Instant.now().toString());
        SseHelper.sendJson(context.getSseEmitter(), JacksonUtil.beanToStr(event), eventType.eventName());
    }

    public SseEmitter emitter() {
        return context.getSseEmitter();
    }
}
