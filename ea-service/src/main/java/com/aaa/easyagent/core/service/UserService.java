package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaIamUserDO;
import com.aaa.easyagent.core.domain.request.UserLoginReq;
import com.aaa.easyagent.core.domain.request.UserRegisterReq;
import com.aaa.easyagent.core.domain.result.CurrentUserInfo;
import com.aaa.easyagent.core.domain.result.LoginResult;

/**
 * 用户服务接口
 *
 * @author Claude Code
 * @version 1.0 UserService.java  2026/03/14
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param req 登录请求
     * @return 登录结果（包含 Token）
     */
    LoginResult login(UserLoginReq req);

    /**
     * 用户注册
     *
     * @param req 注册请求
     * @return 用户ID
     */
    Long register(UserRegisterReq req);

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    CurrentUserInfo getCurrentUser();

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    EaIamUserDO getUserById(Long userId);

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    java.util.List<EaIamUserDO> getUserList();

    /**
     * 更新用户信息
     *
     * @param userDO 用户信息
     */
    void updateUser(EaIamUserDO userDO);

    /**
     * 用户登出
     */
    void logout();
}
