package com.aaa.easyagent.biz.agent.data;

import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import jakarta.persistence.Column;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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
}
