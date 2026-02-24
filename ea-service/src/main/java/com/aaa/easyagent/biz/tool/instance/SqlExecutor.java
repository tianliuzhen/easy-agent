package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.template.SqlParamsTemplate;
import com.aaa.easyagent.common.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
                PreparedStatement statement = connection.prepareStatement(paramsTemplate.getSql());
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
                        return JacksonUtil.toStr(result);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        } catch (SQLException e) {
            log.error("执行SQL查询时发生错误:" + e.getMessage(), e);
            throw new AgentToolException("执行SQL查询时发生错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("执行SQL查询时发生未知错误: {}", e.getMessage(), e);
            throw new AgentToolException("执行SQL查询时发生未知错误: " + e.getMessage());
        }
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
