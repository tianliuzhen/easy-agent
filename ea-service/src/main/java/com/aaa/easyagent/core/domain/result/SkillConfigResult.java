package com.aaa.easyagent.core.domain.result;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Skill 配置结果
 *
 * @author liuzhen.tian
 * @version 1.0 SkillConfigResult.java  2026/4/6
 */
@Data
public class SkillConfigResult {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID（0表示官方技能）
     */
    private Long userId;

    /**
     * 技能名称（唯一标识）
     */
    private String skillName;

    /**
     * 技能显示名称
     */
    private String skillDisplayName;

    /**
     * 技能描述
     */
    private String skillDescription;

    /**
     * 技能类型：INTERNAL(内部)/EXTERNAL(外部)/PLUGIN(插件)
     */
    private String skillType;

    /**
     * 技能分类：general/development/data/media/etc
     */
    private String skillCategory;

    /**
     * 技能图标URL或emoji
     */
    private String skillIcon;

    /**
     * 技能版本号
     */
    private String skillVersion;

    /**
     * 技能提供者
     */
    private String skillProvider;

    /**
     * 技能能力列表
     */
    private List<String> skillCapabilities;

    /**
     * 输入参数Schema（JSON格式）
     */
    private String inputSchema;

    /**
     * 输出参数Schema（JSON格式）
     */
    private String outputSchema;

    /**
     * 技能元数据（JSON格式）
     */
    private String skillMetadata;

    /**
     * 技能配置（JSON格式）
     */
    private String skillConfig;

    /**
     * 执行模式：sync(同步)/async(异步)
     */
    private String executionMode;

    /**
     * 执行超时时间（秒）
     */
    private Integer timeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 状态：active/inactive/error/deprecated
     */
    private String status;

    /**
     * 最后执行时间
     */
    private Date lastExecutedAt;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}