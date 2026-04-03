package com.aaa.easyagent.core.domain.enums;

import com.aaa.easyagent.core.domain.template.HttpReqParamsTemplate;
import com.aaa.easyagent.core.domain.template.SqlParamsTemplate;
import com.aaa.easyagent.core.domain.template.ParamsTemplate;
import com.aaa.easyagent.core.domain.template.McpParamsTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolTypeEnum.java  2025/5/26 20:33
 */
@Getter
@AllArgsConstructor
public enum ToolTypeEnum {
    HTTP("HTTP", "htt调用", HttpReqParamsTemplate.class),
    SQL("SQL", "sql查询", SqlParamsTemplate.class),
    MCP("MCP", "MCP调用", McpParamsTemplate.class);

    private final String type;
    private final String desc;
    private final Class<? extends ParamsTemplate> paramsTemplate;


    public static ToolTypeEnum getByType(String type) {
        for (ToolTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new RuntimeException("未找到对应的工具类型:" + type);
    }
}
