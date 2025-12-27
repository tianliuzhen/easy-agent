package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_tool_config
 * 表注释：工具通用模板配置表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_tool_config")
public class EaToolConfigDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * agentId
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * 工具类型 (SQL, HTTP, MCP, GRPC等)
     */
    @Column(name = "tool_type")
    private String toolType;

    /**
     * 工具实例ID（可选，用于区分同一类型的多个实例）
     */
    @Column(name = "tool_instance_id")
    private String toolInstanceId;

    /**
     * 工具实例名称
     */
    @Column(name = "tool_instance_name")
    private String toolInstanceName;

    /**
     * 入参模板
     */
    @Column(name = "input_template")
    private String inputTemplate;

    /**
     * 出参模板
     */
    @Column(name = "out_template")
    private String outTemplate;

    /**
     * 是否必需 (1=是, 0=否)
     */
    @Column(name = "is_required")
    private Boolean isRequired;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否启用 (1=启用, 0=禁用)
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
     * 默认值（JSON格式存储）
     */
    @Column(name = "tool_value")
    private String toolValue;

    /**
     * 额外配置信息（JSON格式，用于存储工具特定的配置）
     */
    @Column(name = "extra_config")
    private String extraConfig;
}