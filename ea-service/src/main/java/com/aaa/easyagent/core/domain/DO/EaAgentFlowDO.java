package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_agent_flow
 * 表注释：多Agent编排表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_agent_flow")
public class EaAgentFlowDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 多Agent编排名称
     */
    @Column(name = "flow_name")
    private String flowName;

    /**
     * 编排Key
     */
    @Column(name = "flow_key")
    private String flowKey;

    /**
     * 编排策略：SUPERVISOR/ROUTER/WORKFLOW
     */
    @Column(name = "strategy")
    private String strategy;

    /**
     * 主管/路由Agent ID
     */
    @Column(name = "supervisor_agent_id")
    private Long supervisorAgentId;

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
     * 编排备注
     */
    @Column(name = "flow_desc")
    private String flowDesc;

    /**
     * 编排级提示词（主管/路由指令）
     */
    @Column(name = "prompt")
    private String prompt;

    /**
     * 欢迎语
     */
    @Column(name = "welcome_message")
    private String welcomeMessage;
}