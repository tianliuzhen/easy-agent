package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_skill_config
 * 表注释：Skill技能配置表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_skill_config")
public class EaSkillConfigDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 用户ID（0表示官方技能）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 技能名称（唯一标识）
     */
    @Column(name = "skill_name")
    private String skillName;

    /**
     * 技能显示名称
     */
    @Column(name = "skill_display_name")
    private String skillDisplayName;

    /**
     * 技能描述
     */
    @Column(name = "skill_description")
    private String skillDescription;

    /**
     * 技能类型：INTERNAL(内部)/EXTERNAL(外部)/PLUGIN(插件)
     */
    @Column(name = "skill_type")
    private String skillType;

    /**
     * 技能分类：general/development/data/media/etc
     */
    @Column(name = "skill_category")
    private String skillCategory;

    /**
     * 技能图标URL或emoji
     */
    @Column(name = "skill_icon")
    private String skillIcon;

    /**
     * 技能版本号
     */
    @Column(name = "skill_version")
    private String skillVersion;

    /**
     * 技能提供者
     */
    @Column(name = "skill_provider")
    private String skillProvider;

    /**
     * 技能能力列表（JSON数组）
     */
    @Column(name = "skill_capabilities")
    private String skillCapabilities;

    /**
     * 输入参数Schema（JSON格式）
     */
    @Column(name = "input_schema")
    private String inputSchema;

    /**
     * 输出参数Schema（JSON格式）
     */
    @Column(name = "output_schema")
    private String outputSchema;

    /**
     * 技能元数据（JSON格式）
     */
    @Column(name = "skill_metadata")
    private String skillMetadata;

    /**
     * 技能配置（JSON格式，存储执行所需配置）
     */
    @Column(name = "skill_config")
    private String skillConfig;

    /**
     * 执行模式：sync(同步)/async(异步)
     */
    @Column(name = "execution_mode")
    private String executionMode;

    /**
     * 执行超时时间（秒）
     */
    @Column(name = "timeout")
    private Integer timeout;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries")
    private Integer maxRetries;

    /**
     * 状态：active/inactive/error/deprecated
     */
    @Column(name = "status")
    private String status;

    /**
     * 最后执行时间
     */
    @Column(name = "last_executed_at")
    private Date lastExecutedAt;

    /**
     * 最后错误信息
     */
    @Column(name = "last_error")
    private String lastError;

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
}