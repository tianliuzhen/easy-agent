package com.aaa.easyagent.util;

import com.hubspot.jinjava.Jinjava;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 JinjiaTest.java  2026/3/18 21:35
 */
public class JinjiaTest {


    public static void main(String[] args) {
        // 1. 初始化 Jinjava 引擎
        Jinjava jinjava = new Jinjava();

        // 2. 定义 Jinja2 模板（就是您提供的那个）
        String template = """
                你是一位客服助手，正在处理用户的{{ inquiry_type }}。
                
                {% if inquiry_type == "投诉" %}
                请先表达歉意，然后询问具体问题细节。
                语气要温和诚恳，避免使用推卸责任的表达。
                {% elif inquiry_type == "咨询" %}
                请提供准确、详细的信息，如果涉及多个方案可以对比说明。
                {% elif inquiry_type == "售后" %}
                请先确认订单信息，再根据用户问题提供相应的售后流程。
                {% else %}
                请礼貌询问用户的具体需求，并表示愿意提供帮助。
                {% endif %}
                
                用户问题：{{ user_message }}
                """;

        // 3. 准备测试数据 - 演示不同场景
        testComplaintScenario(jinjava, template);
        testConsultationScenario(jinjava, template);
        testAfterSalesScenario(jinjava, template);
        testOtherScenario(jinjava, template);
    }

    /**
     * 测试投诉场景
     */
    private static void testComplaintScenario(Jinjava jinjava, String template) {
        System.out.println("\n=== 场景1：投诉 ===");

        Map<String, Object> context = new HashMap<>();
        context.put("inquiry_type", "投诉");
        context.put("user_message", "你们的产品收到就坏了，太差了！");

        String rendered = jinjava.render(template, context);
        System.out.println(rendered);
    }

    /**
     * 测试咨询场景
     */
    private static void testConsultationScenario(Jinjava jinjava, String template) {
        System.out.println("\n=== 场景2：咨询 ===");

        Map<String, Object> context = new HashMap<>();
        context.put("inquiry_type", "咨询");
        context.put("user_message", "你们家手机哪个型号拍照最好？");

        String rendered = jinjava.render(template, context);
        System.out.println(rendered);
    }

    /**
     * 测试售后场景
     */
    private static void testAfterSalesScenario(Jinjava jinjava, String template) {
        System.out.println("\n=== 场景3：售后 ===");

        Map<String, Object> context = new HashMap<>();
        context.put("inquiry_type", "售后");
        context.put("user_message", "我买的电脑可以退货吗？订单号是123456");

        String rendered = jinjava.render(template, context);
        System.out.println(rendered);
    }

    /**
     * 测试其他类型
     */
    private static void testOtherScenario(Jinjava jinjava, String template) {
        System.out.println("\n=== 场景4：其他（表扬） ===");

        Map<String, Object> context = new HashMap<>();
        context.put("inquiry_type", "表扬");
        context.put("user_message", "你们客服态度真好！");

        String rendered = jinjava.render(template, context);
        System.out.println(rendered);
    }
}
