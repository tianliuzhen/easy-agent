package com.aaa.springai.domain.model;

import com.aaa.springai.domain.enums.ModelTypeEnum;
import com.aaa.springai.domain.enums.ToolRunMode;
import lombok.Data;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentModel.java  2025/5/25 18:20
 */
@Data
public class AgentModel {
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
    private String question;


    /**
     * agent 关联工具
     */
    private List<ToolModel> toolModels;

    /**
     * 大模型
     */
    private ModelTypeEnum modelType;

    private ToolRunMode toolRunMode = ToolRunMode.reAct;
}
