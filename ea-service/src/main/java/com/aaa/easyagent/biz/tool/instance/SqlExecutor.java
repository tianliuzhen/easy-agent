package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.SqlParamsTemplate;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL执行器，用于执行SQL查询并返回结果
 *
 * @author liuzhen.tian
 * @version 1.0 SqlExecutor.java  2025/5/25 17:50
 */
@Component
@ToolTypeChooser(ToolTypeEnum.SQL)
@Slf4j
public class SqlExecutor implements ToolExecutor<SqlParamsTemplate> {

    private static final ThreadPoolExecutor NETWORK_TIMEOUT_EXECUTOR = new ThreadPoolExecutor(
            10, // 核心线程数
            12, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(500), // 队列大小
            Executors.defaultThreadFactory(), // 线程工厂
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    ) {
        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
        }
    }; // 允许核心线程超时

    static {
        NETWORK_TIMEOUT_EXECUTOR.allowCoreThreadTimeOut(true); // 允许核心线程超时
    }

    /**
     * 禁止出现的写操作/危险关键字（按单词边界匹配，避免误伤列名）
     */
    private static final Pattern FORBIDDEN_KEYWORD = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|replace|merge|grant|revoke|call|exec|execute)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * SQL 末尾必须存在 LIMIT 子句：limit N / limit N, M / limit M offset N
     */
    private static final Pattern ENDS_WITH_LIMIT = Pattern.compile(
            "\\blimit\\s+\\d+(\\s*,\\s*\\d+)?(\\s+offset\\s+\\d+)?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String call(String functionInput, ToolDefinition<SqlParamsTemplate> toolDefinition) {
        SqlParamsTemplate paramsTemplate = toolDefinition.getParamsTemplate();

        try {
            // 验证必要参数
            if (paramsTemplate.getHost() == null || paramsTemplate.getPort() == null ||
                    paramsTemplate.getDatabase() == null || paramsTemplate.getUsername() == null ||
                    paramsTemplate.getPassword() == null || paramsTemplate.getSql() == null) {
                throw new AgentToolException("缺少必要的数据库连接参数或SQL语句");
            }

            // 通过占位符 {xxx} / '{xxx}' 动态替换 SQL，referenceValue 为占位符 token
            String sql = resolveSql(functionInput, paramsTemplate.getSql(), toolDefinition);

            // 安全校验：仅允许 SELECT 查询，且必须带 LIMIT 子句
            validateSelectWithLimit(sql);

            // 构建数据库连接URL
            String jdbcUrl = buildJdbcUrl(paramsTemplate);

            // 建立数据库连接
            try (Connection connection = DriverManager.getConnection(jdbcUrl,
                    paramsTemplate.getUsername(), paramsTemplate.getPassword())) {

                // 设置查询超时时间
                if (paramsTemplate.getTimeout() != null && paramsTemplate.getTimeout() > 0) {
                    connection.setNetworkTimeout(NETWORK_TIMEOUT_EXECUTOR, paramsTemplate.getTimeout());
                } else {
                    connection.setNetworkTimeout(NETWORK_TIMEOUT_EXECUTOR, 30); // 默认30秒
                }

                // 执行SQL查询
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    // 设置最大返回行数
                    if (paramsTemplate.getMaxRows() != null) {
                        statement.setMaxRows(paramsTemplate.getMaxRows());
                    }

                    // 执行查询
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        // 将结果转换为List<Map>
                        List<Map<String, Object>> result = convertResultSetToList(resultSet);

                        // 将结果转换为JSON字符串返回
                        return JacksonUtil.beanToStr(result);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        } catch (SQLException e) {
            log.error("执行SQL查询时发生错误:" + e.getMessage(), e);
            return "执行SQL查询时发生错误: " + e.getMessage();
        } catch (AgentToolException e) {
            log.error("执行SQL查询时发生未知错误: {}", e.getMessage(), e);
            return "执行SQL查询时发生未知错误: " + e.getMessage();
        }
    }

    /**
     * 安全校验：只允许执行 SELECT 查询，禁止写操作。
     *
     * @param sql 已完成占位符替换的最终 SQL
     */
    private void validateSelectWithLimit(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new AgentToolException("SQL语句不能为空");
        }
        // 去除末尾分号与空白，避免多语句拼接
        String normalized = sql.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        // 禁止语句分隔符，防止多语句注入
        if (normalized.contains(";")) {
            throw new AgentToolException("禁止执行多条SQL语句");
        }
        // 禁止出现写操作/危险关键字（按单词边界匹配，避免误伤列名如 is_deleted）
        if (FORBIDDEN_KEYWORD.matcher(normalized).find()) {
            throw new AgentToolException("SQL中包含禁止的写操作关键字，仅允许 SELECT 查询");
        }
    }

    /**
     * 用 functionInput 中的入参替换 SQL 中的占位符。
     * <p>
     * SQL 中占位符形如 {@code {xxx}} 或 {@code '{xxx}'}，每个 {@link InputTypeSchema#getReferenceValue()}
     * 保存占位符 token（如 {@code {table_name}}），functionInput 为 LLM 按参数名提供的 JSON {@code {name: value}}。
     * 按参数名匹配 schema，取其 referenceValue 占位符，替换为对应的值。未提供的参数使用 schema 默认值。
     *
     * @param functionInput  LLM 提供的入参 JSON
     * @param rawSql         含占位符的原始 SQL
     * @param toolDefinition 工具定义
     * @return 替换后的可执行 SQL
     */
    private String resolveSql(String functionInput, String rawSql, ToolDefinition<SqlParamsTemplate> toolDefinition) {
        boolean debug = toolDefinition.isDebug();
        List<InputTypeSchema> inputTypeSchemas = toolDefinition.getInputTypeSchemas();

        // 无入参配置：SQL 不含占位符，直接执行
        if (CollectionUtils.isEmpty(inputTypeSchemas)) {
            return rawSql;
        }

        Map<String, InputTypeSchema> inputTypeSchemaMap = inputTypeSchemas.stream()
                .collect(Collectors.toMap(InputTypeSchema::getName, Function.identity(), (o, n) -> o));

        String sql = rawSql;

        // 1. 用 functionInput 提供的值替换占位符
        JSONObject functionInputJson = StringUtils.isBlank(functionInput) ? new JSONObject() : JSON.parseObject(functionInput);
        if (!CollectionUtils.isEmpty(functionInputJson)) {
            for (Map.Entry<String, Object> entry : functionInputJson.entrySet()) {
                String name = entry.getKey();
                InputTypeSchema schema = inputTypeSchemaMap.get(name);
                if (schema == null) {
                    if (debug) {
                        continue;
                    }
                    throw new AgentToolException(name + ": 无法匹配inputTypeSchemas");
                }
                if (StringUtils.isBlank(schema.getReferenceValue())) {
                    throw new AgentToolException(name + ":缺少referenceValue");
                }
                sql = replacePlaceholder(sql, schema.getReferenceValue(), entry.getValue());
            }
        }

        // 2. 未提供值的参数，用默认值兜底
        for (InputTypeSchema schema : inputTypeSchemas) {
            if (StringUtils.isBlank(schema.getReferenceValue())) {
                continue;
            }
            if (functionInputJson.containsKey(schema.getName())) {
                continue;
            }
            if (schema.getDefaultValue() != null) {
                sql = replacePlaceholder(sql, schema.getReferenceValue(), schema.getDefaultValue());
            }
        }

        log.info("SqlExecutor resolveSql: {}", sql);

        // SELECT 语句末尾没有 LIMIT 子句时自动追加，行数取前端配置的 maxRows
        if (sql.trim().toUpperCase().startsWith("SELECT") && !ENDS_WITH_LIMIT.matcher(sql.trim()).find()) {
            Integer maxRows = toolDefinition.getParamsTemplate().getMaxRows();
            int limit = maxRows != null ? maxRows : 1000;
            sql = sql + " LIMIT " + limit;
        }

        return sql;
    }

    /**
     * 替换占位符。referenceValue 为占位符 token（如 {@code {table_name}}），
     * 同时覆盖 {@code '{table_name}'}（SQL 已带引号）与裸 {@code {table_name}} 两种写法。
     */
    private String replacePlaceholder(String sql, String referenceValue, Object value) {
        String str = value == null ? "" : String.valueOf(value);
        return sql.replace(referenceValue, str);
    }

    /**
     * 构建JDBC连接URL
     *
     * @param paramsTemplate 参数模板
     * @return JDBC连接URL
     */
    private String buildJdbcUrl(SqlParamsTemplate paramsTemplate) {
        String dialect = paramsTemplate.getDialect();
        String host = paramsTemplate.getHost();
        String port = paramsTemplate.getPort();
        String database = paramsTemplate.getDatabase();

        if ("mysql".equalsIgnoreCase(dialect)) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);
        } else if ("postgresql".equalsIgnoreCase(dialect)) {
            return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        } else {
            // 默认使用MySQL
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);
        }
    }

    /**
     * 将ResultSet转换为List<Map<String, Object>>
     *
     * @param resultSet 结果集
     * @return 转换后的列表
     * @throws SQLException SQL异常
     */
    private List<Map<String, Object>> convertResultSetToList(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();

        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            result.add(row);
        }

        return result;
    }
}
