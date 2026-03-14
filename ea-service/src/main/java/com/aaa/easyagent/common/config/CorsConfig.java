package com.aaa.easyagent.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author liuzhen.tian
 * @version 1.0 CorsConfig.java  2025/6/8 11:01
 */
// 处理跨域的配置类
@Configuration
public class CorsConfig {

    /**
     * 允许跨域调用的过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // config.addAllowedOrigin("*");
        // 当 allowCredentials 为 true 时，不能使用 "*"，需要指定具体的来源或使用正则
        config.addAllowedOriginPattern("http://localhost:*");   // 允许所有 localhost 端口
        // config.addAllowedOrigin("http://localhost:5170"); ; // 明确指定前端地址
        config.setAllowCredentials(true); // 允许跨域发送cookie

        config.addAllowedHeader("*"); // 放行全部原始头信息
        config.addAllowedMethod("*"); // 允许所有请求方法跨域调用
        config.setMaxAge(60 * 10L); // 缓存options请求时间，单位：秒，如果不配置默认会一直请求options

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
