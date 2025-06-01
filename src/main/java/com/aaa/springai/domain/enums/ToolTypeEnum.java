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
    common_http("common_http", "htt调用"),
    mysql("mysql", "mysql查询");

    private String type;
    private String desc;
}
