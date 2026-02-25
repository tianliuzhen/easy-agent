package com.aaa.mcp.config;

import com.aaa.mcp.service.WeatherService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 服务器配置
 *
 * 注意：Spring AI 2.0.0-M2 的 MCP 功能可能还在开发中，
 * 当前版本使用 FunctionCallback 方式定义工具。
 *
 * 具体工具定义请参考 tools 包下的实现。
 */
@Configuration
public class McpServerConfig {

    // 当前版本暂不配置 McpServer Bean
    // 工具通过 FunctionCallback Bean 自动注册
    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        return  MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }
}
