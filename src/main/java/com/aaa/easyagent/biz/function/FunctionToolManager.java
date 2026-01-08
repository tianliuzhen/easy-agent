package com.aaa.easyagent.biz.function;

import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.common.config.exception.AgentException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 FunctionToolManager.java  2025/6/1 19:48
 */
@Component
public class FunctionToolManager {

    @Resource
    private List<ToolExecutor> baseCallbackList;

    private Map<ToolTypeEnum, ToolExecutor> baseCallbackMap;

    public FunctionToolManager() {
        System.out.println();
    }

    @PostConstruct
    public void init() {
        baseCallbackMap = new HashMap<>();
        baseCallbackList.forEach(baseCallback -> {
            ToolTypeChooser annotation = baseCallback.getClass().getAnnotation(ToolTypeChooser.class);
            baseCallbackMap.put(annotation.value(), baseCallback);
        });
    }

    public String call(String functionInput, ToolModel toolModel) {
        ToolTypeEnum toolType = toolModel.getToolType();
        if (baseCallbackMap.get(toolType) == null) {
            throw new AgentException("无法选择工具处理器：" + toolType.getType());
        }
        return baseCallbackMap.get(toolType).call(functionInput, toolModel);
    }

}
