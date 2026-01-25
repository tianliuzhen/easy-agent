package com.aaa.easyagent.biz.tool;

import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
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
        ToolDefinition toolDefinition = ToolDefinition.buildToolDefinition(eaToolConfigReq);

        toolDefinition.setDebug(true);
        String call = functionToolManager.call(null, toolDefinition);
        Object parse = null;
        try {
            parse = JSON.parse(call);
        } catch (Exception e) {
            return call;
        }
        return parse;
    }


}
