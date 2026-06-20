package com.aaa.easyagent.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.SkillsTool.Skill;
import org.springaicommunity.agent.utils.MarkdownParser;
import org.springaicommunity.agent.utils.Skills;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * spring-ai-agent-utils Skills 功能测试
 * <p>
 * 演示 Skills 的加载、解析、SkillsTool 构建及 MarkdownParser 用法。
 */
@DisplayName("Skills 工具测试")
class SkillsToolTest {

    // ==================== Skills 加载 ====================

    @Nested
    @DisplayName("从目录加载 Skill")
    class LoadFromDirectory {

        /**
         * 模拟的 SKILL.md 文件内容，包含 YAML frontmatter + Markdown body。
         */
        private static final String SKILL_MD = """
                ---
                name: code-review
                description: Review code for bugs, style, and security issues
                metadata:
                  type: tool
                  category: development
                ---

                # Code Review Skill

                Review code changes and provide feedback.

                ## Instructions

                1. Read the provided code
                2. Check for common issues
                3. Report findings
                """;

        @Test
        @DisplayName("loadDirectory 加载单个技能目录")
        void loadSingleSkillDirectory(@TempDir Path tempDir) throws Exception {
            // 准备技能目录结构：{tempDir}/code-review/SKILL.md
            Path skillDir = tempDir.resolve("code-review");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), SKILL_MD);

            List<Skill> skills = Skills.loadDirectory(tempDir.toString());

            assertNotNull(skills);
            assertEquals(1, skills.size());

            Skill skill = skills.get(0);
            assertEquals("code-review", skill.name());
            assertNotNull(skill.content());
            assertTrue(skill.content().contains("Code Review Skill"));

            Map<String, Object> fm = skill.frontMatter();
            assertNotNull(fm);
            assertEquals("code-review", fm.get("name"));
            assertEquals("Review code for bugs, style, and security issues", fm.get("description"));
        }

        @Test
        @DisplayName("loadDirectories 加载多个技能目录")
        void loadMultipleSkillDirectories(@TempDir Path tempDir) throws Exception {
            Path codeReviewDir = tempDir.resolve("code-review");
            Path textSummaryDir = tempDir.resolve("text-summary");
            Files.createDirectory(codeReviewDir);
            Files.createDirectory(textSummaryDir);

            Files.writeString(codeReviewDir.resolve("SKILL.md"), """
                    ---
                    name: code-review
                    description: Code review skill
                    ---
                    # Code Review
                    """);
            Files.writeString(textSummaryDir.resolve("SKILL.md"), """
                    ---
                    name: text-summary
                    description: Text summary skill
                    ---
                    # Text Summary
                    """);

            List<Skill> skills = Skills.loadDirectories(
                    List.of(codeReviewDir.toString(), textSummaryDir.toString()));

            assertEquals(2, skills.size());
            List<String> names = skills.stream().map(Skill::name).sorted().toList();
            assertEquals(List.of("code-review", "text-summary"), names);
        }

        @Test
        @DisplayName("加载空目录返回空列表")
        void loadEmptyDirectory(@TempDir Path tempDir) {
            List<Skill> skills = Skills.loadDirectory(tempDir.toString());
            assertNotNull(skills);
            assertTrue(skills.isEmpty());
        }
    }

    // ==================== SkillsTool 构建 ====================

    @Nested
    @DisplayName("SkillsTool 构建与使用")
    class SkillsToolBuilding {

        @Test
        @DisplayName("通过目录构建 SkillsTool 并验证 ToolCallback")
        void buildSkillsToolFromDirectory(@TempDir Path tempDir) throws Exception {
            Path skillDir = tempDir.resolve("unit-converter");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                    ---
                    name: unit-converter
                    description: Convert between units of measurement
                    metadata:
                      category: utility
                    ---
                    # Unit Converter
                    Convert values between different units.
                    """);

            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsDirectory(tempDir.toString())
                    .toolDescriptionTemplate("Available skills: {skills}")
                    .build();

            assertNotNull(toolCallback);
            assertEquals("Skill", toolCallback.getToolDefinition().name());

            String description = toolCallback.getToolDefinition().description();
            assertNotNull(description);
            // 描述应包含模板文字或技能名称
            assertTrue(description.contains("unit-converter")
                            || description.contains("Available skills"),
                    "description should contain skill name or template text, but got: " + description);
        }

        @Test
        @DisplayName("通过多个目录构建 SkillsTool")
        void buildSkillsToolFromMultipleDirectories(@TempDir Path tempDir) throws Exception {
            Path dir1 = tempDir.resolve("json-formatter");
            Path dir2 = tempDir.resolve("regex-generator");
            Files.createDirectory(dir1);
            Files.createDirectory(dir2);

            Files.writeString(dir1.resolve("SKILL.md"), """
                    ---
                    name: json-formatter
                    description: Format and validate JSON
                    ---
                    # JSON Formatter
                    """);
            Files.writeString(dir2.resolve("SKILL.md"), """
                    ---
                    name: regex-generator
                    description: Generate regex from description
                    ---
                    # Regex Generator
                    """);

            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsDirectories(List.of(dir1.toString(), dir2.toString()))
                    .build();

            assertNotNull(toolCallback);
            String description = toolCallback.getToolDefinition().description();
            assertTrue(description.contains("json-formatter"));
            assertTrue(description.contains("regex-generator"));
        }

        @Test
        @DisplayName("自定义 toolDescriptionTemplate")
        void customDescriptionTemplate(@TempDir Path tempDir) throws Exception {
            Path skillDir = tempDir.resolve("data-analysis");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                    ---
                    name: data-analysis
                    description: Analyze datasets and generate reports
                    ---
                    # Data Analysis
                    """);

            String customTemplate = "你可以使用以下技能来完成任务：{skills}";

            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsDirectory(tempDir.toString())
                    .toolDescriptionTemplate(customTemplate)
                    .build();

            String description = toolCallback.getToolDefinition().description();
            assertTrue(description.contains("你可以使用以下技能来完成任务"));
        }
    }

    // ==================== Skill 记录操作 ====================

    @Nested
    @DisplayName("Skill Record 操作")
    class SkillRecordOperations {

        @Test
        @DisplayName("Skill 的 name、content、frontMatter 字段访问")
        void skillRecordFields(@TempDir Path tempDir) throws Exception {
            Path skillDir = tempDir.resolve("text-summary");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                    ---
                    name: text-summary
                    description: Summarize long texts into concise summaries
                    version: "1.0"
                    metadata:
                      category: text
                      author: easy-agent
                    ---

                    # Text Summary Skill

                    Summarize any text to the specified length.

                    ## Parameters
                    - text: The text to summarize
                    - max_length: Maximum summary length (default 200)
                    """);

            Skill skill = Skills.loadDirectory(tempDir.toString()).get(0);

            assertAll("Skill record 字段验证",
                    () -> assertEquals("text-summary", skill.name()),
                    () -> assertNotNull(skill.content()),
                    () -> assertTrue(skill.content().contains("Text Summary Skill")),
                    () -> assertNotNull(skill.frontMatter()),
                    () -> assertEquals("text-summary", skill.frontMatter().get("name")),
                    () -> assertEquals("Summarize long texts into concise summaries",
                            skill.frontMatter().get("description")),
                    () -> assertEquals("1.0", skill.frontMatter().get("version"))
            );
        }

        @Test
        @DisplayName("toXml 生成 XML 格式")
        void skillToXml(@TempDir Path tempDir) throws Exception {
            Path skillDir = tempDir.resolve("json-formatter");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                    ---
                    name: json-formatter
                    description: Format and validate JSON strings
                    ---
                    # JSON Formatter

                    Format, minify, or validate JSON strings.
                    """);

            Skill skill = Skills.loadDirectory(tempDir.toString()).get(0);
            String xml = skill.toXml();

            assertNotNull(xml);
            assertTrue(xml.contains("json-formatter"));
        }

        @Test
        @DisplayName("Skill 内容不包含 frontmatter 分隔符")
        void skillContentExcludesFrontmatter(@TempDir Path tempDir) throws Exception {
            Path skillDir = tempDir.resolve("sql-generator");
            Files.createDirectory(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                    ---
                    name: sql-generator
                    description: Generate SQL from natural language
                    ---
                    # SQL Generator
                    Body content here.
                    """);

            Skill skill = Skills.loadDirectory(tempDir.toString()).get(0);
            String content = skill.content();

            assertFalse(content.contains("---"),
                    "content 不应包含 frontmatter 分隔符");
            assertTrue(content.contains("SQL Generator"));
        }
    }

    // ==================== MarkdownParser ====================

    @Nested
    @DisplayName("MarkdownParser 解析")
    class MarkdownParserUsage {

        @Test
        @DisplayName("解析包含 YAML frontmatter 的 Markdown")
        void parseYamlFrontmatter() {
            String md = """
                    ---
                    title: My Skill
                    version: 2.0
                    tags:
                      - java
                      - spring
                    ---

                    # Body Content

                    This is the body.
                    """;

            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> frontMatter = parser.getFrontMatter();

            assertNotNull(frontMatter);
            assertEquals("My Skill", frontMatter.get("title"));
            assertEquals("2.0", frontMatter.get("version"));

            String content = parser.getContent();
            assertNotNull(content);
            assertTrue(content.contains("# Body Content"));
            assertFalse(content.contains("---"));
            assertFalse(content.contains("title:"));
        }

        @Test
        @DisplayName("解析无 frontmatter 的纯 Markdown")
        void parsePlainMarkdown() {
            String md = "# Just a heading\n\nSome content without frontmatter.";

            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> frontMatter = parser.getFrontMatter();

            assertTrue(frontMatter.isEmpty(), "无 frontmatter 时返回空 Map");
            String content = parser.getContent();
            assertEquals("# Just a heading\n\nSome content without frontmatter.", content);
        }

        @Test
        @DisplayName("解析复杂嵌套 frontmatter")
        void parseNestedFrontmatter() {
            String md = """
                    ---
                    name: complex-skill
                    metadata:
                      author: easy-agent
                      tags: [ai, tools]
                      config:
                        timeout: 30
                        retries: 3
                    parameters:
                      - name: input
                        type: string
                      - name: mode
                        type: enum
                        values: [fast, detailed]
                    ---

                    # Complex Skill
                    """;

            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> fm = parser.getFrontMatter();

            assertEquals("complex-skill", fm.get("name"));
            assertNotNull(fm.get("metadata"), "metadata 键应存在");
        }

        @Test
        @DisplayName("解析带列表 tags 的 frontmatter")
        void parseListTagsFrontmatter() {
            String md = """
                    ---
                    title: My Skill
                    version: 2.0
                    tags:
                      - java
                      - spring
                    ---

                    # Body Content
                    """;

            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> fm = parser.getFrontMatter();

            assertEquals("My Skill", fm.get("title"));
            assertEquals("2.0", fm.get("version"));
            assertNotNull(fm.get("tags"), "列表 tags 键应存在");
        }
    }
}
