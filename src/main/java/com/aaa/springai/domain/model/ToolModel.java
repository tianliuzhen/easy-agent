package com.aaa.springai.domain.model;

import com.aaa.springai.domain.enums.ToolTypeEnum;
import com.aaa.springai.domain.schema.HttpReqParamsTemplate;
import com.aaa.springai.domain.schema.InputTypeSchema;
import lombok.Data;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolModel.java  2025/5/25 21:43
 */
@Data
public class ToolModel {
    private Long toolId;
    private String toolName;
    private String toolDesc;

    /**
     * 工具类型
     */
    private ToolTypeEnum toolType;

    /**
     * http 参数模板
     */
    private HttpReqParamsTemplate httpReqParamsTemplate;


    /**
     * 请求参数结构体
     */
    private List<InputTypeSchema> inputTypeSchemas;
    /**
     * 返回参数结构体
     */
    private String outputTypeSchema;

}
