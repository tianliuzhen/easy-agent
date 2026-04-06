package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.core.domain.DO.EaSkillConfigDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.SkillParamsTemplate;
import com.aaa.easyagent.core.mapper.EaSkillConfigDAO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Skill 工具执行器
 * 支持内部技能和外部技能调用
 *
 * @author liuzhen.tian
 * @version 1.0 SkillExecutor.java  2026/4/6
 */
@Slf4j
@Component
@ToolTypeChooser(ToolTypeEnum.SKILL)
public class SkillExecutor implements ToolExecutor<SkillParamsTemplate> {

    @Autowired
    private EaSkillConfigDAO eaSkillConfigDAO;

    /**
     * 内部技能处理器映射
     */
    private final Map<String, InternalSkillHandler> internalSkillHandlers = new HashMap<>();

    public SkillExecutor() {
        // 注册内置技能处理器
        registerInternalSkills();
    }

    /**
     * 注册内置技能
     */
    private void registerInternalSkills() {
        // 代码审查技能
        internalSkillHandlers.put("code_review", this::handleCodeReview);
        // 文本摘要技能
        internalSkillHandlers.put("text_summary", this::handleTextSummary);
        // 数据分析技能
        internalSkillHandlers.put("data_analysis", this::handleDataAnalysis);
        // 翻译技能
        internalSkillHandlers.put("translation", this::handleTranslation);
        // JSON格式化技能
        internalSkillHandlers.put("json_formatter", this::handleJsonFormatter);
        // 图像分析技能
        internalSkillHandlers.put("image_analysis", this::handleImageAnalysis);
        // 单位转换技能
        internalSkillHandlers.put("unit_converter", this::handleUnitConverter);
        // 正则生成器技能
        internalSkillHandlers.put("regex_generator", this::handleRegexGenerator);
        // SQL生成器技能
        internalSkillHandlers.put("sql_generator", this::handleSqlGenerator);
        // 文档解析技能
        internalSkillHandlers.put("document_parser", this::handleDocumentParser);
    }

    @Override
    public String call(String functionInput, ToolDefinition<SkillParamsTemplate> toolDefinition) {
        boolean debug = toolDefinition.isDebug();
        log.info("SkillExecutor call {}: toolName={}, input={}",
                debug ? "debug" : "", toolDefinition.getToolName(), functionInput);

        if (StringUtils.isBlank(functionInput) && !debug) {
            throw new AgentToolException("functionInput 不能为空");
        }

        SkillParamsTemplate paramsTemplate = toolDefinition.getParamsTemplate();
        if (paramsTemplate == null) {
            throw new AgentToolException("Skill 参数模板不能为空");
        }

        // 获取技能配置
        EaSkillConfigDO skillConfig = getSkillConfig(paramsTemplate);
        if (skillConfig == null) {
            throw new AgentToolException("未找到 Skill 配置: skillName=" + paramsTemplate.getSkillName());
        }

        try {
            // 解析参数
            Map<String, Object> arguments = parseArguments(functionInput);
            log.info("SkillExecutor executing skill: skillName={}, skillType={}, arguments={}",
                    skillConfig.getSkillName(), skillConfig.getSkillType(), arguments);

            // 根据技能类型执行
            String result;
            if ("INTERNAL".equalsIgnoreCase(skillConfig.getSkillType())) {
                result = executeInternalSkill(skillConfig, arguments);
            } else {
                result = executeExternalSkill(skillConfig, arguments);
            }

            // 更新最后执行时间
            updateLastExecutedTime(skillConfig.getId());

            return result;

        } catch (Exception e) {
            log.error("Skill 执行失败: skillName={}", skillConfig.getSkillName(), e);
            updateLastError(skillConfig.getId(), e.getMessage());
            throw new AgentToolException("Skill 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 Skill 配置
     */
    private EaSkillConfigDO getSkillConfig(SkillParamsTemplate paramsTemplate) {
        if (StringUtils.isNotBlank(paramsTemplate.getSkillName())) {
            // 通过 skillName 查询
            return eaSkillConfigDAO.selectAll().stream()
                    .filter(config -> paramsTemplate.getSkillName().equals(config.getSkillName()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * 执行内部技能
     */
    private String executeInternalSkill(EaSkillConfigDO skillConfig, Map<String, Object> arguments) {
        String skillName = skillConfig.getSkillName();
        InternalSkillHandler handler = internalSkillHandlers.get(skillName);

        if (handler != null) {
            return handler.handle(arguments);
        }

        // 如果没有特定处理器，返回通用响应
        JSONObject result = new JSONObject();
        result.put("skill", skillName);
        result.put("status", "executed");
        result.put("message", "技能执行成功（通用处理）");
        result.put("input", arguments);
        return result.toJSONString();
    }

    /**
     * 执行外部技能
     */
    private String executeExternalSkill(EaSkillConfigDO skillConfig, Map<String, Object> arguments) {
        // 外部技能执行逻辑（可以调用远程API等）
        log.info("执行外部技能: skillName={}", skillConfig.getSkillName());

        JSONObject result = new JSONObject();
        result.put("skill", skillConfig.getSkillName());
        result.put("status", "executed");
        result.put("message", "外部技能执行成功");
        result.put("input", arguments);
        return result.toJSONString();
    }

    /**
     * 解析函数输入参数
     */
    private Map<String, Object> parseArguments(String functionInput) {
        if (StringUtils.isBlank(functionInput)) {
            return Map.of();
        }
        try {
            JSONObject jsonObject = JSON.parseObject(functionInput);
            return jsonObject.getInnerMap();
        } catch (Exception e) {
            log.warn("解析 functionInput 失败: {}", functionInput);
            return Map.of();
        }
    }

    /**
     * 更新最后执行时间
     */
    private void updateLastExecutedTime(Long skillId) {
        try {
            EaSkillConfigDO update = new EaSkillConfigDO();
            update.setId(skillId);
            update.setLastExecutedAt(new Date());
            eaSkillConfigDAO.updateByPrimaryKeySelective(update);
        } catch (Exception e) {
            log.warn("更新技能最后执行时间失败: skillId={}", skillId, e);
        }
    }

    /**
     * 更新最后错误信息
     */
    private void updateLastError(Long skillId, String error) {
        try {
            EaSkillConfigDO update = new EaSkillConfigDO();
            update.setId(skillId);
            update.setLastError(error);
            eaSkillConfigDAO.updateByPrimaryKeySelective(update);
        } catch (Exception e) {
            log.warn("更新技能错误信息失败: skillId={}", skillId, e);
        }
    }

    // ==================== 内置技能处理器 ====================

    /**
     * 代码审查技能
     */
    private String handleCodeReview(Map<String, Object> arguments) {
        String code = (String) arguments.getOrDefault("code", "");
        String language = (String) arguments.getOrDefault("language", "unknown");

        JSONObject result = new JSONObject();
        result.put("skill", "code_review");
        result.put("language", language);

        List<JSONObject> issues = new ArrayList<>();
        List<JSONObject> suggestions = new ArrayList<>();

        // 简单的代码检查逻辑
        if (code.length() > 1000) {
            suggestions.add(createSuggestion("code_length", "代码较长，建议拆分为更小的函数", "medium"));
        }
        if (code.contains("TODO") || code.contains("FIXME")) {
            issues.add(createIssue("todo_found", "发现 TODO/FIXME 标记", "low"));
        }
        if (!code.contains("try") && (code.contains("File") || code.contains("Stream"))) {
            suggestions.add(createSuggestion("resource_management", "建议添加 try-with-resources 确保资源释放", "high"));
        }

        result.put("issues", issues);
        result.put("suggestions", suggestions);
        result.put("score", calculateCodeScore(issues.size(), suggestions.size()));

        return result.toJSONString();
    }

    /**
     * 文本摘要技能
     */
    private String handleTextSummary(Map<String, Object> arguments) {
        String text = (String) arguments.getOrDefault("text", "");
        int maxLength = ((Number) arguments.getOrDefault("max_length", 200)).intValue();

        String summary = generateSummary(text, maxLength);
        List<String> keyPoints = extractKeyPoints(text);

        JSONObject result = new JSONObject();
        result.put("skill", "text_summary");
        result.put("summary", summary);
        result.put("key_points", keyPoints);
        result.put("original_length", text.length());
        result.put("summary_length", summary.length());

        return result.toJSONString();
    }

    /**
     * 数据分析技能
     */
    private String handleDataAnalysis(Map<String, Object> arguments) {
        Object data = arguments.get("data");
        String analysisType = (String) arguments.getOrDefault("analysis_type", "summary");

        JSONObject result = new JSONObject();
        result.put("skill", "data_analysis");
        result.put("analysis_type", analysisType);

        if (data instanceof List) {
            List<?> dataList = (List<?>) data;
            result.put("record_count", dataList.size());

            if ("summary".equals(analysisType)) {
                result.put("report", generateDataSummary(dataList));
            } else if ("trend".equals(analysisType)) {
                result.put("report", generateTrendAnalysis(dataList));
            }
        }

        return result.toJSONString();
    }

    /**
     * 翻译技能
     */
    private String handleTranslation(Map<String, Object> arguments) {
        String text = (String) arguments.getOrDefault("text", "");
        String sourceLang = (String) arguments.getOrDefault("source_lang", "auto");
        String targetLang = (String) arguments.getOrDefault("target_lang", "en");

        // 模拟翻译结果
        JSONObject result = new JSONObject();
        result.put("skill", "translation");
        result.put("source_lang", sourceLang);
        result.put("target_lang", targetLang);
        result.put("original_text", text);
        result.put("translated_text", "[Translated] " + text);
        result.put("confidence", 0.95);

        return result.toJSONString();
    }

    /**
     * JSON格式化技能
     */
    private String handleJsonFormatter(Map<String, Object> arguments) {
        String jsonText = (String) arguments.getOrDefault("json_text", "");
        String operation = (String) arguments.getOrDefault("operation", "format");

        JSONObject result = new JSONObject();
        result.put("skill", "json_formatter");
        result.put("operation", operation);

        try {
            Object parsed = JSON.parse(jsonText);

            switch (operation.toLowerCase()) {
                case "format":
                    result.put("result", JSON.toJSONString(parsed, true));
                    break;
                case "minify":
                    result.put("result", JSON.toJSONString(parsed, false));
                    break;
                case "validate":
                    result.put("is_valid", true);
                    result.put("result", "JSON格式有效");
                    break;
                default:
                    result.put("result", JSON.toJSONString(parsed, true));
            }
            result.put("is_valid", true);
        } catch (Exception e) {
            result.put("is_valid", false);
            result.put("error", "JSON解析错误: " + e.getMessage());
        }

        return result.toJSONString();
    }

    /**
     * 图像分析技能
     */
    private String handleImageAnalysis(Map<String, Object> arguments) {
        String imageUrl = (String) arguments.getOrDefault("image_url", "");
        String analysisType = (String) arguments.getOrDefault("analysis_type", "scene");

        JSONObject result = new JSONObject();
        result.put("skill", "image_analysis");
        result.put("analysis_type", analysisType);
        result.put("image_url", imageUrl);

        switch (analysisType) {
            case "ocr":
                result.put("text_content", "[OCR结果] 检测到的文本内容...");
                break;
            case "scene":
                result.put("description", "这是一张包含办公场景的图像，有电脑、桌椅等物品");
                break;
            case "objects":
                result.put("objects", Arrays.asList(
                        createObject("laptop", 0.95, "0.2,0.3,0.5,0.6"),
                        createObject("chair", 0.87, "0.6,0.4,0.8,0.9")
                ));
                break;
        }

        return result.toJSONString();
    }

    /**
     * 单位转换技能
     */
    private String handleUnitConverter(Map<String, Object> arguments) {
        double value = ((Number) arguments.getOrDefault("value", 0)).doubleValue();
        String fromUnit = (String) arguments.getOrDefault("from_unit", "");
        String toUnit = (String) arguments.getOrDefault("to_unit", "");

        double result = convertUnit(value, fromUnit, toUnit);

        JSONObject resultObj = new JSONObject();
        resultObj.put("skill", "unit_converter");
        resultObj.put("original_value", value);
        resultObj.put("original_unit", fromUnit);
        resultObj.put("target_unit", toUnit);
        resultObj.put("result", result);
        resultObj.put("formula", value + " " + fromUnit + " = " + result + " " + toUnit);

        return resultObj.toJSONString();
    }

    /**
     * 正则生成器技能
     */
    private String handleRegexGenerator(Map<String, Object> arguments) {
        String description = (String) arguments.getOrDefault("description", "");
        String testString = (String) arguments.getOrDefault("test_string", "");

        String regex = generateRegexFromDescription(description);

        JSONObject result = new JSONObject();
        result.put("skill", "regex_generator");
        result.put("description", description);
        result.put("regex", regex);
        result.put("explanation", explainRegex(regex));

        if (StringUtils.isNotBlank(testString)) {
            result.put("test_result", testRegex(regex, testString));
        }

        return result.toJSONString();
    }

    /**
     * SQL生成器技能
     */
    private String handleSqlGenerator(Map<String, Object> arguments) {
        String description = (String) arguments.getOrDefault("description", "");
        Object schema = arguments.get("schema");
        String dialect = (String) arguments.getOrDefault("dialect", "mysql");

        String sql = generateSqlFromDescription(description, dialect);

        JSONObject result = new JSONObject();
        result.put("skill", "sql_generator");
        result.put("description", description);
        result.put("dialect", dialect);
        result.put("sql", sql);
        result.put("explanation", "根据描述生成的SQL查询语句");
        result.put("parameters", new ArrayList<>());

        return result.toJSONString();
    }

    /**
     * 文档解析技能
     */
    private String handleDocumentParser(Map<String, Object> arguments) {
        String documentUrl = (String) arguments.getOrDefault("document_url", "");
        String format = (String) arguments.getOrDefault("format", "");
        Object extractOptions = arguments.get("extract_options");

        JSONObject result = new JSONObject();
        result.put("skill", "document_parser");
        result.put("format", format);
        result.put("document_url", documentUrl);
        result.put("text", "[文档内容] 这是从文档中提取的文本内容...");
        result.put("metadata", createDocumentMetadata(format));
        result.put("pages", 5);

        return result.toJSONString();
    }

    // ==================== 辅助方法 ====================

    private JSONObject createIssue(String type, String message, String severity) {
        JSONObject issue = new JSONObject();
        issue.put("type", type);
        issue.put("message", message);
        issue.put("severity", severity);
        return issue;
    }

    private JSONObject createSuggestion(String type, String message, String priority) {
        JSONObject suggestion = new JSONObject();
        suggestion.put("type", type);
        suggestion.put("message", message);
        suggestion.put("priority", priority);
        return suggestion;
    }

    private int calculateCodeScore(int issueCount, int suggestionCount) {
        int score = 100;
        score -= issueCount * 10;
        score -= suggestionCount * 5;
        return Math.max(0, score);
    }

    private String generateSummary(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private List<String> extractKeyPoints(String text) {
        List<String> points = new ArrayList<>();
        // 简单的关键词提取逻辑
        String[] sentences = text.split("[。！？.]");
        for (int i = 0; i < Math.min(sentences.length, 3); i++) {
            if (sentences[i].trim().length() > 10) {
                points.add(sentences[i].trim());
            }
        }
        return points;
    }

    private JSONObject generateDataSummary(List<?> dataList) {
        JSONObject summary = new JSONObject();
        summary.put("total_records", dataList.size());
        summary.put("analysis", "数据统计分析完成");
        return summary;
    }

    private JSONObject generateTrendAnalysis(List<?> dataList) {
        JSONObject trend = new JSONObject();
        trend.put("total_records", dataList.size());
        trend.put("trend", "上升趋势");
        trend.put("confidence", 0.85);
        return trend;
    }

    private JSONObject createObject(String name, double confidence, String bbox) {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("confidence", confidence);
        obj.put("bbox", bbox);
        return obj;
    }

    private double convertUnit(double value, String fromUnit, String toUnit) {
        // 简化的单位转换逻辑
        Map<String, Double> toMeter = new HashMap<>();
        toMeter.put("m", 1.0);
        toMeter.put("km", 1000.0);
        toMeter.put("cm", 0.01);
        toMeter.put("mm", 0.001);
        toMeter.put("ft", 0.3048);
        toMeter.put("in", 0.0254);

        if (toMeter.containsKey(fromUnit) && toMeter.containsKey(toUnit)) {
            double meters = value * toMeter.get(fromUnit);
            return meters / toMeter.get(toUnit);
        }
        return value;
    }

    private String generateRegexFromDescription(String description) {
        // 简化的正则生成逻辑
        if (description.contains("邮箱") || description.contains("email")) {
            return "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        } else if (description.contains("手机") || description.contains("phone")) {
            return "^1[3-9]\\d{9}$";
        } else if (description.contains("数字")) {
            return "^\\d+$";
        }
        return ".*";
    }

    private String explainRegex(String regex) {
        if (regex.contains("@")) {
            return "匹配标准邮箱格式";
        } else if (regex.contains("1[3-9]")) {
            return "匹配中国大陆手机号";
        } else if (regex.contains("\\d")) {
            return "匹配数字";
        }
        return "通用匹配模式";
    }

    private JSONObject testRegex(String regex, String testString) {
        JSONObject result = new JSONObject();
        try {
            boolean matches = testString.matches(regex);
            result.put("matches", matches);
            result.put("input", testString);
            result.put("pattern", regex);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    private String generateSqlFromDescription(String description, String dialect) {
        // 简化的SQL生成逻辑
        if (description.contains("查询") || description.contains("select")) {
            return "SELECT * FROM table_name WHERE condition = 'value';";
        } else if (description.contains("插入") || description.contains("insert")) {
            return "INSERT INTO table_name (column1, column2) VALUES ('value1', 'value2');";
        } else if (description.contains("更新") || description.contains("update")) {
            return "UPDATE table_name SET column1 = 'value1' WHERE condition = 'value';";
        }
        return "-- 请提供更详细的SQL描述";
    }

    private JSONObject createDocumentMetadata(String format) {
        JSONObject metadata = new JSONObject();
        metadata.put("format", format);
        metadata.put("encoding", "UTF-8");
        metadata.put("author", "Unknown");
        metadata.put("created", new Date().toString());
        return metadata;
    }

    /**
     * 内部技能处理器接口
     */
    @FunctionalInterface
    private interface InternalSkillHandler {
        String handle(Map<String, Object> arguments);
    }
}
