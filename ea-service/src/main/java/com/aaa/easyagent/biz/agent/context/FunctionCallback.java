package com.aaa.easyagent.biz.agent.context;

/**
 * @author liuzhen.tian
 * @version 1.0 FunctionCallback.java  2026/4/3 23:11
 */
public interface FunctionCallback {
    String getName();

    String getDescription();

    String getInputTypeSchema();

    String call(String actionInput);
}
