package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
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
     * 大模型平台
     */
    @Column(name = "model_platform")
    private String modelPlatform;

    /**
     * 决策大模型
     */
    @Column(name = "analysis_model")
    private String analysisModel;

    /**
     * 工具大模型
     */
    @Column(name = "tool_model")
    private String toolModel;

    /**
     * agent备注
     */
    @Column(name = "agent_desc")
    private String agentDesc;
}