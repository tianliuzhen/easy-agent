package com.aaa.easyagent.biz.agent.context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentHolder.java  2025/5/10 18:45
 */
public class AgentHolder {

    private static final ThreadLocal<List<String>> agentThink = new InheritableThreadLocal<>();

    public static List<String> getAgentThink() {
        List<String> res = agentThink.get();
        if (res == null) {
            res = new ArrayList<>();
            agentThink.set(res);
        }
        return res;
    }

    public static void setAgentThink(List<String> res) {
        agentThink.set(res);
    }

    /**
     * 写入当前线程思考过程
     *
     * @param prefix
     */
    public static void writeThink(String prefix, String data) {
        String name = Thread.currentThread().getName();
        List<String> buffer = agentThink.get();
        if (buffer == null) {
            buffer = new ArrayList<>();
            agentThink.set(buffer);
        }
        buffer.add(prefix + data);
    }

    public static String getThink() {
        String name = Thread.currentThread().getName();
        List<String> buffer = agentThink.get();
        if (CollectionUtils.isEmpty(buffer)) {
            return "";
        }

        // 用于存储结果的 Map
        Map<String, StringBuilder> resultMap = new HashMap<>();

        // 构建最终结果
        StringBuilder finalResult = new StringBuilder();

        // 遍历输入列表
        String tempPrefix = "";
        for (int i = 0; i < buffer.size(); i++) {
            String[] parts = buffer.get(i).split(":", 1);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                boolean endIndex = (i == buffer.size() - 1);
                if (endIndex) {
                    resultMap.putIfAbsent(key, new StringBuilder());
                    resultMap.get(key).append(value.trim());
                }
                if ((!key.equals(tempPrefix) || endIndex) && StringUtils.isNotBlank(tempPrefix)) {
                    for (Map.Entry<String, StringBuilder> entry : resultMap.entrySet()) {
                        finalResult.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    resultMap.clear();
                }

                if (!endIndex) {
                    resultMap.putIfAbsent(key, new StringBuilder());
                    resultMap.get(key).append(value.trim());
                }

                tempPrefix = key;
            }
        }

        return finalResult.toString();
    }

    public static void clearThink() {
        List<String> buffer = agentThink.get();
        if (buffer != null) {
            agentThink.remove();
        }
    }

}
