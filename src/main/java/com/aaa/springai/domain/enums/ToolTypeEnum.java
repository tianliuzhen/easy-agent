package com.aaa.springai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolTypeEnum.java  2025/5/26 20:33
 */
@Getter
@AllArgsConstructor
public enum ToolTypeEnum {
    http("http", "htt调用"),
    sql("sql", "sql查询");

    private String type;
    private String desc;
}
