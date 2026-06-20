package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_agent_quick_prompt
 * 表注释：Agent浮选提示词配置表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_agent_quick_prompt")
public class EaAgentQuickPromptDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 关联的Agent ID
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * 浮选按钮名称，如 售后/物流/投诉/咨询
     */
    @Column(name = "label")
    private String label;

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
     * 推荐问题列表（JSON数组）
     */
    @Column(name = "questions")
    private String questions;
}