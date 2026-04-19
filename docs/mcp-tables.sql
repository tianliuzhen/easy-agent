-- MCP 集成数据库表结构（简化版）
-- 生成日期：2026-04-02

-- 1. MCP 服务配置表
CREATE TABLE `ea_mcp_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `server_name` varchar(100) NOT NULL COMMENT '服务器名称',
  `server_url` varchar(500) DEFAULT NULL COMMENT '服务器URL（SSE模式使用）',
  `transport_type` varchar(20) NOT NULL DEFAULT 'SSE' COMMENT '传输类型：SSE/STDIO',
  `command` text COMMENT '启动命令（STDIO模式使用）',
  `env_vars` json DEFAULT NULL COMMENT '环境变量（JSON数组）',
  `tool_name` varchar(100) NOT NULL COMMENT '工具名称（MCP Server中的原始名称）',
  `tool_display_name` varchar(100) DEFAULT NULL COMMENT '工具显示名称',
  `tool_description` text COMMENT '工具描述',
  `input_schema` json DEFAULT NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json DEFAULT NULL COMMENT '输出参数Schema（JSON格式）',
  `tool_metadata` json DEFAULT NULL COMMENT '工具元数据（JSON格式）',
  `connection_timeout` int(11) DEFAULT 30 COMMENT '连接超时时间（秒）',
  `max_retries` int(11) DEFAULT 3 COMMENT '最大重试次数',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active/inactive/error',
  `last_connected_at` datetime DEFAULT NULL COMMENT '最后连接时间',
  `last_error` text COMMENT '最后错误信息',
  `description` text COMMENT '描述信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_server_tool` (`server_name`, `tool_name`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP服务配置表';

-- 2. MCP 关系表（Agent 与 MCP 工具的绑定）
CREATE TABLE `ea_mcp_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID',
  `mcp_config_id` bigint(20) NOT NULL COMMENT 'MCP配置ID',
  `binding_config` json DEFAULT NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_mcp` (`agent_id`, `mcp_config_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_mcp_config_id` (`mcp_config_id`),
  KEY `idx_is_active` (`is_active`),
  CONSTRAINT `fk_mcp_relation_agent` FOREIGN KEY (`agent_id`) REFERENCES `ea_agent_config` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mcp_relation_config` FOREIGN KEY (`mcp_config_id`) REFERENCES `ea_mcp_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP关系表（Agent绑定）';

-- 初始化数据示例（可选）
-- INSERT INTO `ea_mcp_config` (`server_name`, `server_url`, `transport_type`, `tool_name`, `tool_display_name`, `tool_description`)
-- VALUES ('本地天气服务', 'http://localhost:8080/mcp/sse', 'SSE', 'getWeather', '获取天气', '查询指定城市的天气信息');
