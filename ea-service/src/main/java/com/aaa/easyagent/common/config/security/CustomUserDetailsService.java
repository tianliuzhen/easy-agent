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
    private EaIamUserRoleDAO userRoleDAO;

    @Autowired
    private EaIamRoleDAO roleDAO;

    @Autowired
    private EaIamRolePermissionDAO rolePermissionDAO;

    @Autowired
    private EaIamPermissionDAO permissionDAO;

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

        // 查询用户角色和权限
        List<String> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(roles);

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
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    private List<String> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleDAO.select(new EaIamUserRoleDO().setUserId(userId))
                .stream()
                .map(EaIamUserRoleDO::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        return roleIds.stream()
                .map(roleId -> {
                    EaIamRoleDO role = roleDAO.selectByPrimaryKey(roleId);
                    return role != null ? role.getRoleCode() : null;
                })
                .filter(roleCode -> roleCode != null)
                .toList();
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
                // 查询角色的权限
                List<Long> permissionIds = rolePermissionDAO.select(new EaIamRolePermissionDO().setRoleId(role.getId()))
                        .stream()
                        .map(EaIamRolePermissionDO::getPermissionId)
                        .toList();

                if (!permissionIds.isEmpty()) {
                    List<String> rolePermissions = permissionIds.stream()
                            .map(permissionId -> {
                                EaIamPermissionDO permission = permissionDAO.selectByPrimaryKey(permissionId);
                                return permission != null ? permission.getPermissionCode() : null;
                            })
                            .filter(permissionCode -> permissionCode != null)
                            .toList();
                    permissions.addAll(rolePermissions);
                }
            }
        }

        return permissions;
    }
}
