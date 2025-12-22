package com.aaa.easyagent.biz.agent.function.tool;

import com.aaa.easyagent.core.domain.model.ToolModel;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolCallback.java  2025/5/26 20:54
 */
public interface ToolExecutor {
    String call(String functionInput, ToolModel toolModel);
}
