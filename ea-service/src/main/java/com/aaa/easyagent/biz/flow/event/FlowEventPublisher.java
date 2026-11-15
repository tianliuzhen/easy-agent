package com.aaa.easyagent.biz.flow.event;

import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.flow.runtime.FlowRunContext;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Flow 运行事件出口。
 * <p>
 * 发出 FLOW_SUCCEEDED / FLOW_FAILED 等终态事件后关闭出口：失败收敛期间仍在后台跑完的
 * 节点不再往已经 complete 的 SseEmitter 写数据，避免 IllegalStateException 刷日志。
 */
@Slf4j
@RequiredArgsConstructor
public class FlowEventPublisher {
    private final FlowRunContext context;
    /** 终态事件已发出，后续节点事件一律丢弃 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public void publish(FlowEventType eventType, FlowNodeDefinition node, Long agentId, Object data) {
        if (closed.get()) {
            log.debug("Flow 事件出口已关闭，丢弃事件，runId={}, eventType={}", context.getRunId(), eventType.eventName());
            return;
        }
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

    /** 发出 Flow 终态事件，并关闭事件出口。 */
    public void publishTerminal(FlowEventType eventType, Object data) {
        publish(eventType, null, null, data);
        closed.set(true);
    }

    public SseEmitter emitter() {
        return context.getSseEmitter();
    }
}
