package com.aaa.easyagent.biz.tool;

import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolOnlineDebug.java  2026/1/1 22:23
 */
@Service
@RequiredArgsConstructor
public class ToolOnlineDebug {
    private final FunctionToolManager functionToolManager;

    /**
     * 调试工具
     *
     * @param eaToolConfigReq
     * @return
     */
    public Object debug(EaToolConfigReq eaToolConfigReq) {
        ToolTypeEnum toolTypeEnum = ToolTypeEnum.getByType(eaToolConfigReq.getToolType());
        String toolValue = eaToolConfigReq.getToolValue();

        ToolModel toolModel = ToolModel.builder()
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

        String call = functionToolManager.call(toolValue, toolModel);
        return JSON.parse(call);
    }
}
