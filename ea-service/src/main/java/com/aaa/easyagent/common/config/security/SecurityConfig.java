package com.aaa.easyagent.common.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.DispatcherType;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

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
     * CORS 配置 Bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5170", "http://127.0.0.1:5170"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
        var a = "";
        http
                // 禁用 CSRF（使用 Cookie 存储 JWT）
                .csrf(csrf -> csrf.disable())

                // 启用 CORS 支持，使用自定义配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 无状态 Session（使用 JWT）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        /*
                         * SSE 等异步请求在完成时会被 Tomcat 再次派发（ASYNC）回过滤器链，
                         * 那时原始请求的 SecurityContext 已清理，授权检查会判定未认证并拒绝该次派发；
                         * 但响应早已提交 200 与流数据，无法再返回 401，连接被直接掐断、
                         * chunked 终止块未写出，浏览器报 ERR_INCOMPLETE_CHUNKED_ENCODING。
                         *
                         * 放行 ASYNC/ERROR 派发：这两种是容器内部的二次派发，同一请求的身份在首次
                         * REQUEST 派发时已校验完成。浏览器发起的请求恒为 REQUEST 类型，匹配不到
                         * 该规则，认证未被削弱。
                         *
                         * 本项目受此影响的流式（SSE）接口：
                         *   GET  /eaAgent/ai/streamChatWith   单 Agent 聊天流
                         *   POST /eaAgent/ai/chat             单 Agent 聊天流（流式/同步双模）
                         *   POST /eaAgent/flow/{flowId}/debug-run   多 Agent 编排调试试运行
                         * 以上均需登录，由本规则保证收尾派发不被授权检查拒绝。
                         * （/example/** 下的演示流式接口本身免认证，不受影响）
                         *
                         * 注意：把 SSE 路径放进下方免认证名单同样能规避本问题，但那是副作用而非
                         * 解法——免认证意味着接口对任何人开放。
                         */
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
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
                                "/imageSearch/file/**",   // 图片文件读取（<img> 不带 JWT）
                                "/imageSearch/import/**"   // 批量导入接口
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
