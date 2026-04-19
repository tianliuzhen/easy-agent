package com.aaa.easyagent.common.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 配置
 *
 * @author Claude Code
 * @version 1.0 SecurityConfig.java  2026/03/14
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * 密码编码器
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证入口点 - 返回 401 状态码
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                                 AuthenticationException authException) {
                try {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    String jsonResponse = "{\"code\":\"401\",\"message\":\"未认证或认证已过期，请重新登录\"}";
                    response.getWriter().write(jsonResponse);
                } catch (IOException e) {
                    // 忽略IO异常，因为客户端可能已经断开连接
                    // 这里不能使用logger，因为匿名内部类没有logger实例
                    // 可以记录到控制台或使用其他日志方式
                    System.err.println("写入401响应时发生IO异常: " + e.getMessage());
                }
            }
        };
    }

    /**
     * Security 过滤器链配置
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（使用 Cookie 存储 JWT）
                .csrf(csrf -> csrf.disable())

                // 启用 CORS 支持
                .cors(cors -> cors.configure(http))

                // 无状态 Session（使用 JWT）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        // 公开接口（无需认证）
                        .requestMatchers(
                                "/auth/**",           // 认证相关接口
                                "/error",             // 错误页面
                                "/favicon.ico",       // 网站图标
                                "/static/**",         // 静态资源
                                "/webjars/**",        // WebJars资源
                                "/actuator/health",   // 健康检查
                                "/v3/api-docs/**",    // OpenAPI文档
                                "/swagger-ui/**",     // Swagger UI
                                "/swagger-ui.html",    // Swagger页面
                                "/example/**",         // 演示接口
                                "/eaAgent/ai/streamChatWith",   // ai流接口
                                "/eaAgent/ai/chat"    // ai流接口
                        ).permitAll()

                        // 所有其他请求需要认证
                        .anyRequest().authenticated())

                // 配置异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint()))

                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
