package com.aaa.springai.domain.model;

import lombok.Data;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolModel.java  2025/5/25 21:43
 */
@Data
public class ToolModel {
    private String toolId;
    private String toolName;
    private String toolDesc;

    /**
     * 请求参数结构体
     */
    private String inputTypeSchema;
    /**
     * 返回参数结构体
     */
    private String outputTypeSchema;

}
