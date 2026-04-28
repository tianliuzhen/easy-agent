package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_agent
 * 表注释：agent管理表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_agent")
public class EaAgentDO {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * agent名称
     */
    @Column(name = "agent_name")
    private String agentName;

    /**
     * agentKey
     */
    @Column(name = "agent_key")
    private String agentKey;

    /**
     * 大模型平台: ollama/deepseek/硅基流动
     */
    @Column(name = "model_platform")
    private String modelPlatform;

    /**
     * 决策大模型：todo
     */
    @Column(name = "analysis_model")
    private String analysisModel;

    /**
     * 工具大模型：todo
     */
    @Column(name = "tool_model")
    private String toolModel;

    /**
     * 工具运行模式：reAct/tool
     */
    @Column(name = "tool_run_mode")
    private String toolRunMode;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 头像
     */
    @Column(name = "avatar")
    private String avatar;

    /**
     * agent备注
     */
    @Column(name = "agent_desc")
    private String agentDesc;

    /**
     * 提示词
     */
    @Column(name = "prompt")
    private String prompt;

    /**
     * 模型配置:{}
     */
    @Column(name = "model_config")
    private String modelConfig;

    /**
     * 记忆配置:{}
     */
    @Column(name = "memory_config")
    private String memoryConfig;
}