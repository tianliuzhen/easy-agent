package com.aaa.easyagent.core.domain.result;

import lombok.Data;

/**
 * 登录响应结果
 *
 * @author Claude Code
 * @version 1.0 LoginResult.java  2026/03/14
 */
@Data
public class LoginResult {
    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private CurrentUserInfo userInfo;
}
