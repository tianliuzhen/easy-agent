package com.aaa.easyagent.web.auth;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.UserLoginReq;
import com.aaa.easyagent.core.domain.request.UserRegisterReq;
import com.aaa.easyagent.core.domain.result.CurrentUserInfo;
import com.aaa.easyagent.core.domain.result.LoginResult;
import com.aaa.easyagent.core.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Claude Code
 * @version 1.0 AuthController.java  2026/03/14
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Resource
    private UserService userService;

    @Value("${jwt.cookie-name:AUTH_TOKEN}")
    private String cookieName;

    @Value("${jwt.cookie-max-age:604800}")
    private int cookieMaxAge;

    @Value("${jwt.cookie-domain:}")
    private String cookieDomain;

    /**
     * 用户登录
     *
     * @param req      登录请求
     * @param response HTTP 响应
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResult<LoginResult> login(@RequestBody UserLoginReq req, HttpServletResponse response) {
        try {
            LoginResult result = userService.login(req);

            // 设置 JWT Cookie
            Cookie cookie = new Cookie(cookieName, result.getToken());
            cookie.setPath("/");
            cookie.setMaxAge(cookieMaxAge);
            cookie.setHttpOnly(true);
            // 设置 Cookie domain（如果配置了） // 设置了 HttpOnly=true 的 Cookie，JavaScript 无法通过 document.cookie 读取, 存在 XSS 安全风险：恶意脚本可以窃取 token
            if (StringUtils.hasText(cookieDomain)) {
                cookie.setDomain(cookieDomain);
            }
            // 设置 SameSite 属性为 Lax，允许在相同站点内发送 Cookie
            cookie.setAttribute("SameSite", "Lax");
            // 生产环境建议启用 Secure: cookie.setSecure(true);

            response.addCookie(cookie);

            return BaseResult.success(result);
        } catch (RuntimeException e) {
            log.error("登录失败: {}", e.getMessage());
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 用户注册
     *
     * @param req 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResult<Long> register(@RequestBody UserRegisterReq req) {
        try {
            Long userId = userService.register(req);
            return BaseResult.success(userId);
        } catch (RuntimeException e) {
            log.error("注册失败: {}", e.getMessage());
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/currentUser")
    public BaseResult<CurrentUserInfo> getCurrentUser() {
        try {
            CurrentUserInfo userInfo = userService.getCurrentUser();
            if (userInfo == null) {
                return BaseResult.fail("未登录");
            }
            return BaseResult.success(userInfo);
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
            return BaseResult.fail("获取用户信息失败");
        }
    }

    /**
     * 用户登出
     *
     * @param response HTTP 响应
     * @return 登出结果
     */
    @PostMapping("/logout")
    public BaseResult<Void> logout(HttpServletResponse response) {
        try {
            // 清除 JWT Cookie
            Cookie cookie = new Cookie(cookieName, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            // 设置 Cookie domain（如果配置了）
            if (StringUtils.hasText(cookieDomain)) {
                cookie.setDomain(cookieDomain);
            }
            // 设置 SameSite 属性为 Lax
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);

            userService.logout();
            return BaseResult.success();
        } catch (Exception e) {
            log.error("登出失败", e);
            return BaseResult.fail("登出失败");
        }
    }
}
