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
        config.addAllowedOriginPattern("*");   // Spring 5.3 版本引入的新方法，支持正则
        config.setAllowCredentials(true); // 允许跨域发送cookie
        config.addAllowedHeader("*"); // 放行全部原始头信息
        config.addAllowedMethod("*"); // 允许所有请求方法跨域调用
        config.setMaxAge(60*10L); // 缓存options请求时间，单位：秒，如果不配置默认会一直请求options

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
