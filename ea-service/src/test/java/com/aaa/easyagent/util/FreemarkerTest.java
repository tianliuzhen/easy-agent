package com.aaa.easyagent.util;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 FreemarkerTest.java  2026/3/18 22:21
 */
public class FreemarkerTest {
    public static void main(String[] args) {
        try {
            // 1. 创建 FreeMarker 配置
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setDefaultEncoding("UTF-8");

            // 2. 定义模板字符串（就是您提供的那个）
            String templateString = """
                你是一位客服助手，正在处理用户的${inquiryType}。
                
                <#if inquiryType == "投诉">
                请先表达歉意，然后询问具体问题细节。
                语气要温和诚恳，避免使用推卸责任的表达。
                <#elseif inquiryType == "咨询">
                请提供准确、详细的信息，如果涉及多个方案可以对比说明。
                <#elseif inquiryType == "售后">
                请先确认订单信息，再根据用户问题提供相应的售后流程。
                <#else>
                请礼貌询问用户的具体需求，并表示愿意提供帮助。
                </#if>
                
                用户问题：${userMessage}
                """;

            // 3. 从字符串创建模板
            Template template = new Template("prompt",
                    new StringReader(templateString), cfg);

            // 4. 准备测试数据
            testScenario(template, "投诉", "你们的产品收到就坏了，太差了！");
            testScenario(template, "咨询", "你们家手机哪个型号拍照最好？");
            testScenario(template, "售后", "我买的电脑可以退货吗？订单号是123456");
            testScenario(template, "表扬", "你们客服态度真好！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testScenario(Template template,
                                     String inquiryType,
                                     String userMessage) {
        try {
            // 准备数据
            Map<String, Object> data = new HashMap<>();
            data.put("inquiryType", inquiryType);
            data.put("userMessage", userMessage);

            // 渲染
            StringWriter writer = new StringWriter();
            template.process(data, writer);

            // 输出结果
            System.out.println("\n========== " + inquiryType + "场景 ==========");
            System.out.println(writer.toString());
            System.out.println("=====================================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
