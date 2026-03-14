package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_iam_role_permission
 * 表注释：角色权限关联表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_iam_role_permission")
public class EaIamRolePermissionDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 角色ID
     */
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 权限ID
     */
    @Column(name = "permission_id")
    private Long permissionId;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;
}