package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_model_platform
 * 表注释：模型平台配置表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_model_platform")
public class EaModelPlatformDO {
    /**
     * 主键 ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 模型平台标识 (如 deepseek/siliconflow/openai/ollama)
     */
    @Column(name = "model_platform")
    private String modelPlatform;

    /**
     * 模型平台描述
     */
    @Column(name = "model_desc")
    private String modelDesc;

    /**
     * 模型平台图标 URL
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 官网链接
     */
    @Column(name = "official_website")
    private String officialWebsite;

    /**
     * 基础 API URL
     */
    @Column(name = "base_url")
    private String baseUrl;

    /**
     * 是否启用 (1=启用，0=禁用)
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

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
     * 模型版本数组 (JSON 格式存储)
     */
    @Column(name = "model_versions")
    private String modelVersions;
}