package com.aaa.easyagent.agent;

import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 读取项目根目录的 {@code llm-secrets.properties}（已被 .gitignore 忽略，不会提交）。
 * 测试运行目录通常是 ea-service 模块，故从当前目录向上回溯查找仓库根。
 *
 * @author liuzhen.tian
 */
public final class LlmSecrets {

    private static final Properties SECRETS = load();

    private LlmSecrets() {
    }

    public static String get(String key) {
        String value = SECRETS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("llm-secrets.properties 缺少配置项: " + key);
        }
        return value;
    }

    private static Properties load() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("llm-secrets.properties");
            if (Files.isRegularFile(candidate)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(candidate)) {
                    props.load(in);
                    return props;
                } catch (Exception e) {
                    throw new IllegalStateException("读取密钥文件失败: " + candidate, e);
                }
            }
        }
        throw new IllegalStateException("未找到 llm-secrets.properties，请在项目根目录创建");
    }

    public static void main(String[] args) {
        String s = JsonSchemaGenerator.generateForType(SkillsInput.class);
        System.out.println(s);
    }

    public static record SkillsInput(
            @ToolParam(description = "The skill name (no arguments). E.g., \"pdf\" or \"xlsx\"") String command) {
    }
}
