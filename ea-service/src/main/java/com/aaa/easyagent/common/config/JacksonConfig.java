package com.aaa.easyagent.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Jackson 序列化配置
 * 统一处理时间时区和格式
 * 或者使用 yml
 * <pre>
 *  spring:
 *   jackson:
 *     time-zone: Asia/Shanghai
 *     date-format: yyyy-MM-dd HH:mm:ss
 * </pre>
 *
 * @author EasyAgent 系统
 * @version 1.0 JacksonConfig.java  2026/03/15
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 ObjectMapper，统一时区为东八区
     *
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .dateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .build();
    }
}
