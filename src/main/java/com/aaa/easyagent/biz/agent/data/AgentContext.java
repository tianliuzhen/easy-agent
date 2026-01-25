package com.aaa.easyagent.biz.agent.data;

import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
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
     * agentName
     */
    private String agentName;

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
     *sseEmitter
     */
    private SseEmitter sseEmitter;
}
