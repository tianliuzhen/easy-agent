package com.aaa.easyagent.core.domain.result;

import lombok.Data;

import java.util.List;

/**
 * 当前用户信息
 *
 * @author Claude Code
 * @version 1.0 CurrentUserInfo.java  2026/03/14
 */
@Data
public class CurrentUserInfo {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色编码列表
     */
    private List<String> roles;

    /**
     * 权限编码列表
     */
    private List<String> permissions;
}
