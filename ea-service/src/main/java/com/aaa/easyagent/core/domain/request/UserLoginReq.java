package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 用户登录请求
 *
 * @author Claude Code
 * @version 1.0 UserLoginReq.java  2026/03/14
 */
@Data
public class UserLoginReq {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
