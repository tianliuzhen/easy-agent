package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_mcp_relation
 * 表注释：MCP关系表（Agent绑定）
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_mcp_relation")
public class EaMcpRelationDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * Agent ID
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * MCP配置ID
     */
    @Column(name = "mcp_config_id")
    private Long mcpConfigId;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否启用：1=启用，0=禁用
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 绑定配置（JSON格式，可覆盖默认参数）
     */
    @Column(name = "binding_config")
    private String bindingConfig;
}