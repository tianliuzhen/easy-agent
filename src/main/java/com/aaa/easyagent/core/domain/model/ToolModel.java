package com.aaa.easyagent.core.domain.model;

import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.ParamsTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolModel.java  2025/5/25 21:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolModel<T extends ParamsTemplate> {
    /**
     * 工具Id
     */
    private Long toolId;
    /**
     * 工具名称
     */
    private String toolName;
    /**
     * 工具描述
     */
    private String toolDesc;

    /**
     * 工具类型
     */
    private ToolTypeEnum toolType;

    /**
     * 请求参数结构体
     */
    private List<InputTypeSchema> inputTypeSchemas;
    /**
     * 返回参数结构体
     */
    private String outputTypeSchema;


    /**
     * 参数实例
     * HttpReqParamsTemplate
     * SqlParamsTemplate
     */
    private T paramsTemplate;
}
