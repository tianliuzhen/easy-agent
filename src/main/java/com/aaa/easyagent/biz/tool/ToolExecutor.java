package com.aaa.easyagent.biz.tool;

import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.core.domain.template.ParamsTemplate;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolCallback.java  2025/5/26 20:54
 */
public interface ToolExecutor<T extends ParamsTemplate> {
    String call(String functionInput, ToolModel<T> toolModel);
}
