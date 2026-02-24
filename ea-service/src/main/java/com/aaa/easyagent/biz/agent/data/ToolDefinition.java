package com.aaa.easyagent.biz.agent.data;

import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.aaa.easyagent.core.domain.template.ParamsTemplate;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用于存储工具相关的信息
 * 包括工具ID、名称、描述、类型以及输入输出参数模式等
 * 供AI代理调用工具时使用
 *
 * @author liuzhen.tian
 * @version 1.0 ToolDefinition.java  2025/5/25 21:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolDefinition<T extends ParamsTemplate> {
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

    /**
     * 调试模式
     */
    private boolean isDebug;


    public static ToolDefinition buildToolDefinition(EaToolConfigDO eaToolConfigReq) {
        ToolTypeEnum toolTypeEnum = ToolTypeEnum.getByType(eaToolConfigReq.getToolType());
        String toolValue = eaToolConfigReq.getToolValue();
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .toolId(eaToolConfigReq.getId())
                .toolName(eaToolConfigReq.getToolInstanceName())
                .toolDesc(eaToolConfigReq.getToolInstanceDesc())
                // 工具类型
                .toolType(toolTypeEnum)
                // 解析大模型识别的参数
                .inputTypeSchemas(JSON.parseArray(eaToolConfigReq.getInputTemplate(), InputTypeSchema.class))
                // .outputTypeSchema(JSON.parseArray(eaToolConfigReq.getOutTemplate(), InputTypeSchema.class))
                // 解析工具参数元数据
                .paramsTemplate(JSON.parseObject(toolValue, toolTypeEnum.getParamsTemplate()))
                .build();
        return toolDefinition;
    }
}
