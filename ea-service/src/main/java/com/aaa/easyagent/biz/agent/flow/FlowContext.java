package com.aaa.easyagent.biz.agent.flow;

import com.aaa.easyagent.core.domain.DO.EaAgentFlowDO;
import com.aaa.easyagent.core.domain.DO.EaAgentFlowNodeDO;
import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 多 Agent 编排运行期上下文。
 * <p>
 * 由 {@link FlowManagerService} 加载（flow + 有序成员 Agent + nodes），
 * 入口处再填充 sse / sessionId / imageBase64 等运行期信息，最终交由 {@link FlowExecutorManager} 分发执行。
 *
 * @author liuzhen.tian
 */
@Data
@Accessors(chain = true)
public class FlowContext {

    /**
     * 编排主表信息
     */
    private EaAgentFlowDO flow;

    /**
     * 成员节点（按 order_index 升序）
     */
    private List<EaAgentFlowNodeDO> nodes;

    /**
     * 有序成员 Agent，与 {@link #nodes} 一一对应
     */
    private List<EaAgentResult> members;

    /**
     * 编排策略
     */
    private FlowStrategyEnum strategy;

    /**
     * 共享 SSE 发射器，可为 null（同步模式）
     */
    private SseEmitter sse;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 图片（多模态），仅首节点使用，可为 null
     */
    private String imageBase64;

    /**
     * 是否同步模式（无流式订阅）
     */
    private boolean syncMode;

    /**
     * 历史累计输入 Token（用于跨轮汇总），首轮为 0
     */
    private long initInputTokens;

    /**
     * 历史累计输出 Token
     */
    private long initOutputTokens;
}
