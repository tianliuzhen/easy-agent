package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_iam_user_permission
 * 表注释：用户权限表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_iam_user_permission")
public class EaIamUserPermissionDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 权限编码（如ADMIN/USER或agent:read等细粒度权限）
     */
    @Column(name = "permission_code")
    private String permissionCode;

    /**
     * 权限名称（如管理员、普通用户、查看Agent等）
     */
    @Column(name = "permission_name")
    private String permissionName;

    /**
     * 权限描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;
}