package com.aaa.easyagent.common.config.security;

import com.aaa.easyagent.core.domain.DO.*;
import com.aaa.easyagent.core.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 * 从数据库加载用户信息和权限
 *
 * @author Claude Code
 * @version 1.0 CustomUserDetailsService.java  2026/03/14
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private EaIamUserDAO userDAO;

    @Autowired
    private EaIamUserPermissionDAO userPermissionDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        EaIamUserDO user = userDAO.selectOne(new EaIamUserDO().setUsername(username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 查询用户权限（一次性查询，包含角色和细粒度权限）
        List<String> permissions = getUserPermissions(user.getId());
        
        // 提取角色（ADMIN/USER等角色级别权限）
        List<String> roles = extractRoles(permissions);

        // 构建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        log.debug("加载用户: {}, 角色: {}, 权限: {}", username, roles, permissions);

        return User.builder()
                .username(username)
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
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
                .filter(permissionCode -> permissionCode != null)
                .toList();
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
                .toList();
    }
}
