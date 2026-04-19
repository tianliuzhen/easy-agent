package com.aaa.easyagent.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密测试工具
 * 用于生成BCrypt加密后的密码哈希值
 *
 * @author Claude Code
 */
public class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 生成密码哈希
     */
    @Test
    public void testEncodePassword() {
        // 需要加密的明文密码
        String[] passwords = {
            "admin123",
            "user123",
            "123456",
            "password"
        };

        System.out.println("========== BCrypt 密码加密结果 ==========");
        for (String password : passwords) {
            String encoded = passwordEncoder.encode(password);
            System.out.println("明文: " + password);
            System.out.println("加密: " + encoded);
            System.out.println("----------------------------------------");
        }
    }

    /**
     * 验证密码是否匹配
     */
    @Test
    public void testVerifyPassword() {
        String rawPassword = "admin123";
        String encodedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH";

        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        
        System.out.println("========== 密码验证结果 ==========");
        System.out.println("明文密码: " + rawPassword);
        System.out.println("加密哈希: " + encodedPassword);
        System.out.println("是否匹配: " + matches);
    }

    /**
     * 生成单个密码（方便复制）
     */
    @Test
    public void generateSinglePassword() {
        String password = "admin123";
        String encoded = passwordEncoder.encode(password);
        
        System.out.println("\n-- SQL INSERT 语句 --");
        System.out.println("INSERT INTO `ea_iam_user` (`username`, `password`) VALUES ('admin', '" + encoded + "');");
        System.out.println("\n-- 纯哈希值 --");
        System.out.println(encoded);
    }
}
