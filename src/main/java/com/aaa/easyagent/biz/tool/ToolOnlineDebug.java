package com.aaa.easyagent.biz.tool;

import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

/**
 * 工具在线调试服务
 * 提供工具配置的在线调试功能，用于测试和验证工具定义是否正确
 *
 * @author liuzhen.tian
 * @version 1.0 ToolOnlineDebug.java  2026/1/1 22:23
 */
@Service
public class ToolOnlineDebug {

    /**
     * 调试工具
     * 根据传入的工具配置请求，构建工具定义并执行调试调用
     *
     * @param eaToolConfigReq 工具配置请求对象，包含工具的配置信息
     * @return 返回工具执行结果，如果结果是JSON格式则返回解析后的对象，否则返回原始字符串
     */
    public Object debug(EaToolConfigReq eaToolConfigReq) {
        // 根据工具配置请求构建工具定义对象
        ToolDefinition toolDefinition = ToolDefinition.buildToolDefinition(eaToolConfigReq);

        // 设置为调试模式
        toolDefinition.setDebug(true);
        
        // 调用工具管理器执行工具，第一个参数为null表示不传入额外的函数输入
        String call = FunctionToolManager.call(null, toolDefinition);
        
        // 尝试将返回结果解析为JSON对象
        Object parse = null;
        try {
            // 如果返回的是JSON格式字符串，解析为对象返回
            parse = JSON.parse(call);
        } catch (Exception e) {
            // 如果解析失败（非JSON格式），直接返回原始字符串结果
            return call;
        }
        return parse;
    }
}
