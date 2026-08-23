package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.biz.agent.context.ScopedEventEmitter;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 把旧 Agent 的 think/data/tool 事件补充为带 nodeId 的 Flow 事件。 */
public class FlowNodeSseEmitter extends SseEmitter implements ScopedEventEmitter {
    private final FlowEventPublisher publisher;
    private final FlowNodeDefinition node;
    private final Long agentId;

    public FlowNodeSseEmitter(FlowEventPublisher publisher, FlowNodeDefinition node, Long agentId) {
        super(0L);
        this.publisher = publisher;
        this.node = node;
        this.agentId = agentId;
    }

    @Override
    public void sendScoped(String eventType, String content) {
        FlowEventType mapped = switch (eventType) {
            case "data", "finalAnswer" -> FlowEventType.NODE_DATA;
            case "think" -> FlowEventType.NODE_THINK;
            case "tool" -> FlowEventType.NODE_TOOL;
            default -> FlowEventType.NODE_LOG;
        };
        publisher.publish(mapped, node, agentId, content);
    }

    @Override
    public void complete() {
        // Node 不拥有 Flow SSE 生命周期。
    }

    @Override
    public void completeWithError(Throwable ex) {
        publisher.publish(FlowEventType.NODE_FAILED, node, agentId, ex.getMessage());
    }
}
