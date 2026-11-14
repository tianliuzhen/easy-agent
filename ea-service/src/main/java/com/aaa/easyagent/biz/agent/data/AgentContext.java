package com.aaa.easyagent.biz.agent.data;

import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import jakarta.persistence.Column;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.function.BooleanSupplier;
import com.aaa.easyagent.biz.agent.context.FunctionCallback;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentHolder.java  2025/5/25 18:20
 */
@Data
public class AgentContext {
    /**
     * agentId
     */
    private long agentId;
    /**
     * 编排ID（多 Agent 编排会话归属用；单 Agent 为 null）
     */
    private Long flowId;
    /** 编排发布版本；会话创建后固定，避免发布新版本改变历史会话语义。 */
    private Long flowRevisionId;
    /**
     * agentName
     */
    private String agentName;

    /**
     * 提示词
     */
    @Column(name = "prompt")
    private String prompt;

    /**
     * 模型配置
     */
    private String modelConfig;

    /**
     * 工具模型配置
     */
    private String toolModelConfig;

    /**
     * 用户问题
     */
    // private String question;

    /**
     * 模式参数配置
     */
    private AgentModelConfig agentModelConfig;

    /**
     * agent 关联工具
     */
    private List<ToolDefinition> toolDefinitions;

    /** 仅本次运行有效的动态工具，例如 Supervisor 的 Worker Agent。 */
    private List<FunctionCallback> runtimeCallbacks;

    /**
     * 大模型
     */
    private ModelTypeEnum modelType;

    /**
     * 工具允运行模式
     */
    private ToolRunMode toolRunMode = ToolRunMode.ReAct;


    /**
     * agent 记忆配置
     */
    private AgentMemoryConfig agentMemoryConfig;


    /**
     * sseEmitter
     */
    private SseEmitter sseEmitter;


    /**
     * 流式返回
     */
    private boolean withStream = true;


    /**
     * 会话 Id
     */
    private String sessionId;

    /**
     * 图片数据（Base64 Data URL 格式，如 data:image/jpeg;base64,/9j/4AAQ...）
     */
    private String imageBase64;

    /** Flow 节点标识，仅用于运行观测。 */
    private String flowNodeId;

    /**
     * 取消信号。
     * <p>
     * 多 Agent 编排在客户端断开、整体超时或同批节点失败时置位，Agent 决策循环每轮开始前检查，
     * 不再发起下一次大模型调用。单 Agent 直跑时为默认值，恒为 false，行为不变。
     */
    private BooleanSupplier cancelledSupplier = () -> false;

    /** 本次执行是否已被外部取消 */
    public boolean isCancelled() {
        return cancelledSupplier != null && cancelledSupplier.getAsBoolean();
    }
}
