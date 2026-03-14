package com.aaa.easyagent.common.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * 用户上下文持有者
 * 从 Spring Security 的 SecurityContext 中获取当前登录用户信息
 *
 * @author liuzhen.tian
 * @version 1.0 UserContextHolder.java  2026/3/9 22:20
 */
public class UserContextHolder {

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未登录返回 null
     */
    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // principal 可能是 Long（用户ID）或 User（用户详情）
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return principal.toString();
        } else if (principal instanceof User) {
            User user = (User) principal;
            // 从用户名获取用户ID（需要查询数据库或从 JWT 中获取）
            return getUserIdFromUsername(user.getUsername());
        }

        return null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录返回 null
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true=已登录，false=未登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 从用户名获取用户ID（简化版本，实际应从缓存或数据库获取）
     *
     * @param username 用户名
     * @return 用户ID
     */
    private static String getUserIdFromUsername(String username) {
        // 注意：这里简化处理，实际应该从服务层获取
        // 如果需要获取完整的用户信息，建议直接注入 UserService
        return null;
    }
}
