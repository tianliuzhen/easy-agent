package com.aaa.easyagent.core.domain.template;

import lombok.Data;

/**
 * SQL参数模板实体类
 * @author liuzhen.tian
 * @version 1.0 SqlParamsTemplate.java  2025/6/1 20:35
 */
@Data
public class SqlParamsTemplate extends ParamsTemplate {
    /**
     * SQL语句
     */
    private String sql;

    /**
     * 数据库主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private String port;

    /**
     * 数据库方言
     */
    private String dialect;

    /**
     * 最大返回行数
     */
    private Integer maxRows;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 数据库名称
     */
    private String database;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 数据库用户名
     */
    private String username;
}
