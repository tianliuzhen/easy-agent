package com.aaa.easyagent.biz.agent.orchestration;

import lombok.Builder;
import lombok.Getter;
import com.aaa.easyagent.biz.agent.trace.ChatTraceRecorder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 单次 Flow 运行的上下文。取消标志由客户端断开、整体超时或节点失败收敛置位。 */
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
    /**
     * 整条编排共用的轨迹收集器。
     * Agent 与 Supervisor 节点取 scopedTo 视图写入，于是各节点的思考与工具调用
     * 既汇总到同一轮对话，又各自带着节点与 Agent 标识。
     */
    private final ChatTraceRecorder traceRecorder;
    @Builder.Default
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    /** 取消原因，用于审计与前端提示；首个置位的原因胜出 */
    @Builder.Default
    private final AtomicReference<String> cancelReason = new AtomicReference<>();
    @Builder.Default
    private final Map<String, Object> input = Map.of();

    /**
     * 置位取消标志。
     * 幂等：重复调用只保留第一个取消原因，便于区分「客户端断开」和「整体超时」。
     *
     * @param reason 取消原因
     * @return true 表示本次调用真正触发了取消
     */
    public boolean cancel(String reason) {
        boolean first = cancelled.compareAndSet(false, true);
        if (first) {
            cancelReason.set(reason);
        }
        return first;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public String cancelReasonOrDefault() {
        String reason = cancelReason.get();
        return reason == null ? "执行已取消" : reason;
    }

    public String nodeSessionId(String nodeId) {
        return sessionId == null ? null : sessionId + ":" + nodeId;
    }
}
