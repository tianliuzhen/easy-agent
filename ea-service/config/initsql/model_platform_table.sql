-- 模型平台配置表
CREATE TABLE IF NOT EXISTS `ea_model_platform` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `model_platform` varchar(100) NOT NULL COMMENT '模型平台标识 (如 deepseek/siliconflow/openai/ollama)',
  `model_desc` varchar(255) DEFAULT NULL COMMENT '模型平台描述',
  `icon` varchar(500) DEFAULT NULL COMMENT '模型平台图标 URL',
  `official_website` varchar(500) DEFAULT NULL COMMENT '官网链接',
  `base_url` varchar(500) DEFAULT NULL COMMENT '基础 API URL',
  `model_versions` json DEFAULT NULL COMMENT '模型版本数组 (JSON 格式存储)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用 (1=启用，0=禁用)',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_platform` (`model_platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型平台配置表';

-- 初始化数据 (从原有的 ModelTypeEnum 枚举迁移)
INSERT INTO `ea_model_platform` (`model_platform`, `model_desc`, `icon`, `official_website`, `base_url`, `model_versions`, `is_active`, `sort_order`) VALUES
('deepseek', 'deepseek', 'https://www.deepseek.com/favicon.ico', 'https://api.deepseek.com/v1', 'https://api.deepseek.com', '["deepseek-chat", "deepseek-coder"]', 1, 1),
('siliconflow', '硅基流动', 'https://cloud.siliconflow.cn/favicon.ico', 'https://cloud.siliconflow.cn/v1', 'https://api.siliconflow.cn', '["Qwen/Qwen3-30B-A3B-Thinking-2507"]', 1, 2),
('openai', 'openai', 'https://openai.com/favicon.ico', 'https://openai.com/', 'https://api.openai.com/v1', '["gpt-3.5-turbo", "gpt-4"]', 1, 3),
('ollama', 'ollama', 'https://ollama.com/public/ollama.png', 'https://ollama.com/', 'http://localhost:11434', '["llama2", "mistral"]', 1, 4);
