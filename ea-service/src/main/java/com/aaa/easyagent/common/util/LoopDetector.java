package com.aaa.easyagent.common.util;

/**
 * @author liuzhen.tian
 * @version 1.0 LoopDetector.java  2026/3/26 22:51
 */
import java.security.MessageDigest;
import java.util.*;

public class LoopDetector {

    private static final String CHARSET = "UTF-8";


    public static void main(String[] args) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        params.put("age", 30);
        params.put("isStudent", false);

        String hash = normalizeAndSHA256(params);
        System.out.println("Normalized and SHA-256: " + hash);


        Map<String, Object> params2 = new HashMap<>();

        params2.put("name", "Alice");
        params2.put("age", 30);
        params2.put("isStudent", false);

        String hash2 = normalizeAndSHA256(params2);
        System.out.println("Normalized and SHA-256: " + hash2);
    }

    /**
     * 规范化参数并计算 SHA-256
     * @param params 参数 Map
     * @return SHA-256 哈希值（64位十六进制字符串）
     */
    public static String normalizeAndSHA256(Map<String, Object> params) {
        try {
            // 1. 使用 TreeMap 自动按键排序，解决顺序问题
            TreeMap<String, Object> sortedMap = new TreeMap<>(params);

            // 2. 递归处理嵌套结构，转为规范化的字符串
            String normalized = normalizeValue(sortedMap);

            // 3. 计算 SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(CHARSET));

            // 4. 转为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }

    /**
     * 递归规范化任意类型的值
     */
    private static String normalizeValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Map) {
            // 处理嵌套 Map
            @SuppressWarnings("unchecked")
            TreeMap<String, Object> sortedMap = new TreeMap<>((Map<String, Object>) value);
            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (count++ > 0) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(normalizeValue(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();

        } else if (value instanceof List) {
            // 处理列表（如果顺序不重要，可以排序；如果顺序重要，保留原序）
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(normalizeValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();

        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";

        } else if (value instanceof Number) {
            return value.toString();

        } else if (value instanceof Boolean) {
            return value.toString();

        } else {
            // 其他类型转为字符串
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * 简单的 JSON 字符串转义
     */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 检测重复调用
     * @param toolName 工具名称
     * @param params 参数
     * @param history 历史记录 Set
     * @return true-重复调用，false-首次调用
     */
    public static boolean isDuplicate(String toolName,
                                      Map<String, Object> params,
                                      Set<String> history) {
        String paramHash = normalizeAndSHA256(params);
        String key = toolName + ":" + paramHash;

        if (history.contains(key)) {
            return true;
        }

        history.add(key);
        return false;
    }

    /**
     * 带大小限制的重复检测
     * @param toolName 工具名称
     * @param params 参数
     * @param history 历史记录（建议使用 LinkedHashSet）
     * @param maxSize 最大历史记录数
     * @return true-重复调用，false-首次调用
     */
    public static boolean isDuplicateWithLimit(String toolName,
                                               Map<String, Object> params,
                                               LinkedHashSet<String> history,
                                               int maxSize) {
        String paramHash = normalizeAndSHA256(params);
        String key = toolName + ":" + paramHash;

        if (history.contains(key)) {
            return true;
        }

        history.add(key);

        // 超出限制时移除最早的记录
        if (history.size() > maxSize) {
            Iterator<String> iterator = history.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        return false;
    }
}
