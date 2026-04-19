-- Skill 技能表结构和初始数据
-- 参考 MCP 表结构实现

-- 1. Skill 配置表
CREATE TABLE IF NOT EXISTS `ea_skill_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT 0 COMMENT '用户ID（0表示官方技能）',
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称（唯一标识）',
  `skill_display_name` varchar(100) DEFAULT NULL COMMENT '技能显示名称',
  `skill_description` text COMMENT '技能描述',
  `skill_type` varchar(50) NOT NULL DEFAULT 'INTERNAL' COMMENT '技能类型：INTERNAL(内部)/EXTERNAL(外部)/PLUGIN(插件)',
  `skill_category` varchar(50) DEFAULT 'general' COMMENT '技能分类：general/development/data/media/etc',
  `skill_icon` varchar(500) DEFAULT NULL COMMENT '技能图标URL或emoji',
  `skill_version` varchar(20) DEFAULT '1.0.0' COMMENT '技能版本号',
  `skill_provider` varchar(100) DEFAULT 'System' COMMENT '技能提供者',
  `skill_capabilities` json DEFAULT NULL COMMENT '技能能力列表（JSON数组）',
  `input_schema` json DEFAULT NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json DEFAULT NULL COMMENT '输出参数Schema（JSON格式）',
  `skill_metadata` json DEFAULT NULL COMMENT '技能元数据（JSON格式）',
  `skill_config` json DEFAULT NULL COMMENT '技能配置（JSON格式，存储执行所需配置）',
  `execution_mode` varchar(20) DEFAULT 'sync' COMMENT '执行模式：sync(同步)/async(异步)',
  `timeout` int(11) DEFAULT 30 COMMENT '执行超时时间（秒）',
  `max_retries` int(11) DEFAULT 3 COMMENT '最大重试次数',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active/inactive/error/deprecated',
  `last_executed_at` datetime DEFAULT NULL COMMENT '最后执行时间',
  `last_error` text COMMENT '最后错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_name` (`skill_name`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_skill_type` (`skill_type`),
  KEY `idx_skill_category` (`skill_category`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill技能配置表';

-- 2. Skill 关系表（Agent 与 Skill 的绑定）
CREATE TABLE IF NOT EXISTS `ea_skill_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID',
  `skill_config_id` bigint(20) NOT NULL COMMENT 'Skill配置ID',
  `binding_config` json DEFAULT NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_skill` (`agent_id`, `skill_config_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_skill_config_id` (`skill_config_id`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill关系表（Agent绑定）';

-- 3. 初始化官方 Skill 数据
INSERT INTO `ea_skill_config` (`user_id`, `skill_name`, `skill_display_name`, `skill_description`, `skill_type`, `skill_category`, `skill_icon`, `skill_version`, `skill_provider`, `skill_capabilities`, `input_schema`, `output_schema`, `skill_config`, `status`) VALUES
(0, 'code_review', '代码审查', '自动审查代码质量，检查潜在问题和改进建议', 'INTERNAL', 'development', '🔍', '1.0.0', 'System', '["code-analysis", "quality-check", "suggestion"]', '{"type": "object", "properties": {"code": {"type": "string", "description": "要审查的代码"}, "language": {"type": "string", "description": "编程语言"}}, "required": ["code"]}', '{"type": "object", "properties": {"issues": {"type": "array"}, "suggestions": {"type": "array"}, "score": {"type": "number"}}}', '{"timeout": 60}', 'active'),

(0, 'text_summary', '文本摘要', '将长文本自动摘要为简洁的概述', 'INTERNAL', 'general', '📝', '1.0.0', 'System', '["text-processing", "summarization"]', '{"type": "object", "properties": {"text": {"type": "string", "description": "需要摘要的文本"}, "max_length": {"type": "integer", "description": "摘要最大长度", "default": 200}}, "required": ["text"]}', '{"type": "object", "properties": {"summary": {"type": "string"}, "key_points": {"type": "array"}}}', '{}', 'active'),

(0, 'data_analysis', '数据分析', '分析结构化数据，生成统计报告和可视化建议', 'INTERNAL', 'data', '📊', '1.0.0', 'System', '["data-processing", "statistics", "visualization"]', '{"type": "object", "properties": {"data": {"type": "object", "description": "要分析的数据"}, "analysis_type": {"type": "string", "description": "分析类型：summary/trend/correlation"}}, "required": ["data"]}', '{"type": "object", "properties": {"report": {"type": "object"}, "charts": {"type": "array"}}}', '{}', 'active'),

(0, 'translation', '智能翻译', '多语言文本翻译，支持上下文感知', 'INTERNAL', 'general', '🌐', '1.0.0', 'System', '["translation", "multilingual"]', '{"type": "object", "properties": {"text": {"type": "string", "description": "要翻译的文本"}, "source_lang": {"type": "string", "description": "源语言"}, "target_lang": {"type": "string", "description": "目标语言"}}, "required": ["text", "target_lang"]}', '{"type": "object", "properties": {"translated_text": {"type": "string"}, "confidence": {"type": "number"}}}', '{}', 'active'),

(0, 'json_formatter', 'JSON格式化', '格式化、验证和美化JSON数据', 'INTERNAL', 'development', '🔧', '1.0.0', 'System', '["json", "formatting", "validation"]', '{"type": "object", "properties": {"json_text": {"type": "string", "description": "JSON字符串"}, "operation": {"type": "string", "description": "操作：format/validate/minify", "default": "format"}}, "required": ["json_text"]}', '{"type": "object", "properties": {"result": {"type": "string"}, "is_valid": {"type": "boolean"}, "error": {"type": "string"}}}', '{}', 'active'),

(0, 'image_analysis', '图像分析', '分析图像内容，提取文字和描述场景', 'INTERNAL', 'media', '🖼️', '1.0.0', 'System', '["image", "ocr", "scene-description"]', '{"type": "object", "properties": {"image_url": {"type": "string", "description": "图像URL或base64"}, "analysis_type": {"type": "string", "description": "分析类型：ocr/scene/objects", "default": "scene"}}, "required": ["image_url"]}', '{"type": "object", "properties": {"description": {"type": "string"}, "text_content": {"type": "string"}, "objects": {"type": "array"}}}', '{}', 'active'),

(0, 'unit_converter', '单位转换', '支持多种单位类型的智能转换', 'INTERNAL', 'general', '⚖️', '1.0.0', 'System', '["conversion", "calculation"]', '{"type": "object", "properties": {"value": {"type": "number", "description": "要转换的数值"}, "from_unit": {"type": "string", "description": "源单位"}, "to_unit": {"type": "string", "description": "目标单位"}}, "required": ["value", "from_unit", "to_unit"]}', '{"type": "object", "properties": {"result": {"type": "number"}, "formula": {"type": "string"}}}', '{}', 'active'),

(0, 'regex_generator', '正则生成器', '根据描述生成正则表达式并提供测试', 'INTERNAL', 'development', '🔤', '1.0.0', 'System', '["regex", "pattern-matching", "code-generation"]', '{"type": "object", "properties": {"description": {"type": "string", "description": "正则表达式要匹配的描述"}, "test_string": {"type": "string", "description": "测试字符串"}}, "required": ["description"]}', '{"type": "object", "properties": {"regex": {"type": "string"}, "explanation": {"type": "string"}, "test_result": {"type": "object"}}}', '{}', 'active'),

(0, 'sql_generator', 'SQL生成器', '根据自然语言描述生成SQL查询语句', 'INTERNAL', 'data', '🗄️', '1.0.0', 'System', '["sql", "code-generation", "database"]', '{"type": "object", "properties": {"description": {"type": "string", "description": "SQL查询描述"}, "schema": {"type": "object", "description": "数据库表结构"}, "dialect": {"type": "string", "description": "SQL方言：mysql/postgresql/sqlite", "default": "mysql"}}, "required": ["description"]}', '{"type": "object", "properties": {"sql": {"type": "string"}, "explanation": {"type": "string"}, "parameters": {"type": "array"}}}', '{}', 'active'),

(0, 'document_parser', '文档解析', '解析各种文档格式，提取文本内容', 'INTERNAL', 'media', '📄', '1.0.0', 'System', '["document", "parsing", "extraction"]', '{"type": "object", "properties": {"document_url": {"type": "string", "description": "文档URL或base64"}, "format": {"type": "string", "description": "文档格式：pdf/doc/docx/txt"}, "extract_options": {"type": "object", "description": "提取选项"}}, "required": ["document_url", "format"]}', '{"type": "object", "properties": {"text": {"type": "string"}, "metadata": {"type": "object"}, "pages": {"type": "integer"}}}', '{}', 'active');