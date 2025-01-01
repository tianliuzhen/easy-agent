package com.aaa.springai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author liuzhen.tian
 * @version 1.0 TextUniqueValueCalculator.java  2025/1/1 18:24
 */
public class TextUniqueValueCalculator {
    // 静态方法，计算字符串的唯一短值
    public static String calculateUniqueShortValue(String input) {
        try {
            // 获取SHA-256 MessageDigest实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 更新摘要以包含输入字符串的字节
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 将哈希值转换为Base64编码
            String base64Hash = Base64.getEncoder().encodeToString(hash);

            // 截取Base64编码后的前16个字符作为唯一短值
            // 注意：截取长度可以根据需要进行调整，但会影响唯一性
            return base64Hash.substring(0, Math.min(16, base64Hash.length()));

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
