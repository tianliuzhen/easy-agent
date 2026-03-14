package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.common.util.JwtUtil;
import com.aaa.easyagent.core.domain.DO.*;
import com.aaa.easyagent.core.domain.request.UserLoginReq;
import com.aaa.easyagent.core.domain.request.UserRegisterReq;
import com.aaa.easyagent.core.domain.result.CurrentUserInfo;
import com.aaa.easyagent.core.domain.result.LoginResult;
import com.aaa.easyagent.core.mapper.*;
import com.aaa.easyagent.core.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author Claude Code
 * @version 1.0 UserServiceImpl.java  2026/03/14
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private EaIamUserDAO userDAO;

    @Resource
    private EaIamUserRoleDAO userRoleDAO;

    @Resource
    private EaIamRoleDAO roleDAO;

    @Resource
    private EaIamRolePermissionDAO rolePermissionDAO;

    @Resource
    private EaIamPermissionDAO permissionDAO;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.cookie-name:AUTH_TOKEN}")
    private String cookieName;

    @Value("${jwt.cookie-max-age:604800}")
    private int cookieMaxAge;

    @Override
    public LoginResult login(UserLoginReq req) {
        // 查询用户
        EaIamUserDO user = userDAO.selectOne(new EaIamUserDO().setUsername(req.getUsername()));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 获取用户角色
        List<String> roles = getUserRoles(user.getId());

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        // 构建用户信息
        CurrentUserInfo userInfo = new CurrentUserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(roles);
        userInfo.setPermissions(getUserPermissions(roles));

        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setUserInfo(userInfo);

        log.info("用户 {} 登录成功", user.getUsername());
        return result;
    }

    @Override
    public Long register(UserRegisterReq req) {
        // 检查用户名是否已存在
        EaIamUserDO existingUser = userDAO.selectOne(new EaIamUserDO().setUsername(req.getUsername()));
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        EaIamUserDO user = new EaIamUserDO();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus((byte) 1);

        int result = userDAO.insertSelective(user);
        if (result <= 0) {
            throw new RuntimeException("注册失败");
        }

        log.info("用户 {} 注册成功", req.getUsername());
        return user.getId();
    }

    @Override
    public CurrentUserInfo getCurrentUser() {
        // 从 SecurityContext 获取当前用户
        String userId = UserContextHolder.getUserId();

        // 查询用户
        EaIamUserDO user = userDAO.selectByPrimaryKey(Long.valueOf(userId));
        if (user == null) {
            return null;
        }

        // 获取角色和权限
        List<String> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(roles);

        // 构建用户信息
        CurrentUserInfo userInfo = new CurrentUserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    @Override
    public EaIamUserDO getUserById(Long userId) {
        return userDAO.selectByPrimaryKey(userId);
    }

    @Override
    public void logout() {
        // 登出操作（清除 Cookie 在 Controller 中处理）
        log.info("用户登出");
    }

    /**
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    private List<String> getUserRoles(Long userId) {
        List<EaIamUserRoleDO> userRoles = userRoleDAO.select(new EaIamUserRoleDO().setUserId(userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        return userRoles.stream()
                .map(EaIamUserRoleDO::getRoleId)
                .map(roleId -> {
                    EaIamRoleDO role = roleDAO.selectByPrimaryKey(roleId);
                    return role != null ? role.getRoleCode() : null;
                })
                .filter(roleCode -> roleCode != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户权限列表
     *
     * @param roleCodes 角色编码列表
     * @return 权限编码列表
     */
    private List<String> getUserPermissions(List<String> roleCodes) {
        List<String> permissions = new ArrayList<>();

        for (String roleCode : roleCodes) {
            EaIamRoleDO role = roleDAO.selectOne(new EaIamRoleDO().setRoleCode(roleCode));
            if (role != null) {
                List<EaIamRolePermissionDO> rolePermissions =
                        rolePermissionDAO.select(new EaIamRolePermissionDO().setRoleId(role.getId()));
                if (rolePermissions != null) {
                    for (EaIamRolePermissionDO rp : rolePermissions) {
                        EaIamPermissionDO permission = permissionDAO.selectByPrimaryKey(rp.getPermissionId());
                        if (permission != null && StringUtils.isNotBlank(permission.getPermissionCode())) {
                            permissions.add(permission.getPermissionCode());
                        }
                    }
                }
            }
        }

        return permissions;
    }
}
