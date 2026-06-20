package com.aaa.easyagent.agent;

import com.aaa.easyagent.llm.deepseek.DeepSeekTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Skills Agent 集成测试
 * <p>
 * 演示如何把 spring-ai-agent-utils 的 {@link SkillsTool} 作为工具挂载到 ChatClient，
 * 让大模型在对话中自主发现并加载技能（Skill）来完成任务。
 * <p>
 * 复用 {@link DeepSeekTest#createChatModel()} 构建模型；技能目录用 JUnit 的
 * {@link TempDir} 注入，测试结束后自动递归删除。
 *
 * @author liuzhen.tian
 */
public class SkillsAgentTest {

    /**
     * 在临时目录中创建若干 SKILL.md，并构建 SkillsTool。
     * 每个技能目录结构为：{baseDir}/{skill-name}/SKILL.md
     */
    private static ToolCallback buildSkillsTool(Path baseDir) throws IOException {
        writeSkill(baseDir, "weather-query", """
                ---
                name: weather-query
                description: 查询指定城市的实时天气，包括温度、天气状况、空气质量
                ---
                # 天气查询技能

                根据城市名称返回天气信息。

                ## 使用说明
                1. 解析用户提供的城市名称
                2. 返回该城市的温度、天气状况和空气质量
                3. 若用户未指定城市，提示其补充

                ## 示例
                - 输入：北京 -> 输出：北京晴，25°C，空气质量良好
                """);

        writeSkill(baseDir, "unit-converter", """
                ---
                name: unit-converter
                description: 在不同度量单位之间转换数值，支持长度、重量、温度
                ---
                # 单位转换技能

                将数值从一种单位转换为另一种单位。

                ## 支持的单位
                - 长度：m、km、cm、mm、ft、in
                - 温度：摄氏度(C)、华氏度(F)

                ## 转换公式
                - 1 km = 1000 m
                - F = C * 9/5 + 32
                """);

        return SkillsTool.builder()
                .addSkillsDirectory(baseDir.toString())
                .build();
    }

    private static void writeSkill(Path baseDir, String skillName, String content) throws IOException {
        Path skillDir = baseDir.resolve(skillName);
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }

    /**
     * 测试：Agent 集成 Skills，进行一轮对话。
     * <p>
     * 大模型会先调用 Skill 工具列出/加载可用技能，再依据技能说明完成回答。
     */
    @Test
    public void testSkillsAgentConversation(@TempDir Path skillsDir) throws IOException {
        System.out.println("========== Skills Agent 对话测试 ==========");

        ToolCallback skillsTool = buildSkillsTool(skillsDir);

        DeepSeekChatModel chatModel = DeepSeekTest.createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(skillsTool)
                .build();

        String systemPrompt = """
                你是一个具备技能调用能力的智能助手。
                当你需要完成某项任务时，请先使用 Skill 工具查看可用技能列表，
                并按照技能说明（SKILL.md）的步骤来完成用户的请求。
                """;

        String question = "帮我查一下北京今天的天气";
        System.out.println("用户: " + question);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        System.out.println("助手: " + response);
    }

    /**
     * 测试：Agent 使用单位转换技能完成一轮对话。
     */
    @Test
    public void testSkillsAgentUnitConvert(@TempDir Path skillsDir) throws IOException {
        System.out.println("========== Skills Agent 单位转换对话 ==========");

        ToolCallback skillsTool = buildSkillsTool(skillsDir);

        DeepSeekChatModel chatModel = DeepSeekTest.createChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(skillsTool)
                .build();

        String question = "请把 100 摄氏度转换成华氏度，先查阅你的单位转换技能再回答";
        System.out.println("用户: " + question);

        String response = chatClient.prompt()
                .user(question)
                .call()
                .content();

        System.out.println("助手: " + response);
    }
}
