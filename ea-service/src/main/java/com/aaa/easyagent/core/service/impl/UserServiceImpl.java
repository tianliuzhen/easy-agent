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
    private EaIamUserPermissionDAO userPermissionDAO;

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

        // 获取用户权限（包含角色和细粒度权限）
        List<String> permissions = getUserPermissions(user.getId());
        
        // 提取角色（ADMIN/USER等角色级别权限）
        List<String> roles = extractRoles(permissions);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        // 构建用户信息
        CurrentUserInfo userInfo = new CurrentUserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

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

        // 获取用户权限（包含角色和细粒度权限）
        List<String> permissions = getUserPermissions(user.getId());
        
        // 提取角色（ADMIN/USER等角色级别权限）
        List<String> roles = extractRoles(permissions);

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
    public java.util.List<EaIamUserDO> getUserList() {
        return userDAO.selectAll();
    }

    @Override
    public void updateUser(EaIamUserDO userDO) {
        if (userDO.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        // 查询原用户
        EaIamUserDO existingUser = userDAO.selectByPrimaryKey(userDO.getId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新允许修改的字段
        existingUser.setEmail(userDO.getEmail());
        existingUser.setPhone(userDO.getPhone());
        existingUser.setStatus(userDO.getStatus());
        
        // 如果提供了新密码，则加密后更新
        if (StringUtils.isNotBlank(userDO.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(userDO.getPassword()));
        }
        
        int result = userDAO.updateByPrimaryKeySelective(existingUser);
        if (result <= 0) {
            throw new RuntimeException("更新失败");
        }
        
        log.info("用户 {} 信息更新成功", existingUser.getUsername());
    }

    @Override
    public void logout() {
        // 登出操作（清除 Cookie 在 Controller 中处理）
        log.info("用户登出");
    }

    /**
     * 获取用户权限列表（一次性查询，包含角色和细粒度权限）
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    private List<String> getUserPermissions(Long userId) {
        List<EaIamUserPermissionDO> userPermissions = userPermissionDAO.select(
            new EaIamUserPermissionDO().setUserId(userId)
        );
        
        if (userPermissions == null || userPermissions.isEmpty()) {
            return new ArrayList<>();
        }

        return userPermissions.stream()
                .map(EaIamUserPermissionDO::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
    
    /**
     * 从权限列表中提取角色（ADMIN/USER等角色级别权限）
     *
     * @param permissions 权限编码列表
     * @return 角色编码列表
     */
    private List<String> extractRoles(List<String> permissions) {
        return permissions.stream()
                .filter(code -> "ADMIN".equals(code) || "USER".equals(code))
                .collect(Collectors.toList());
    }
}
