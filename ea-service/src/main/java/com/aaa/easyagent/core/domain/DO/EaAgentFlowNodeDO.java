package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_agent_flow_node
 * 表注释：多Agent编排成员节点表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_agent_flow_node")
public class EaAgentFlowNodeDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 关联的编排 ID
     */
    @Column(name = "flow_id")
    private Long flowId;

    /**
     * 成员 Agent ID
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * 节点角色描述
     */
    @Column(name = "node_role")
    private String nodeRole;

    /**
     * 执行顺序：WORKFLOW 按此串行
     */
    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}