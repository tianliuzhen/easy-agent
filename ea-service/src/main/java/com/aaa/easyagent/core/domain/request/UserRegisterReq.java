package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 用户注册请求
 *
 * @author Claude Code
 * @version 1.0 UserRegisterReq.java  2026/03/14
 */
@Data
public class UserRegisterReq {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;
}
