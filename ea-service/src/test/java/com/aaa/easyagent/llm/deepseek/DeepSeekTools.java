package com.aaa.easyagent.llm.deepseek;

/**
 * @author liuzhen.tian
 * @version 1.0 DeepSeekTools.java  2026/4/4 16:55
 */
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek 工具类 - 用于 Function Calling
 */
public class DeepSeekTools {

    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(
            @ToolParam(description = "城市名称，如：北京、上海") String city) {

        Map<String, String> weatherMap = new HashMap<>();
        weatherMap.put("北京", "晴天，温度 25°C，空气质量良好");
        weatherMap.put("上海", "多云，温度 22°C，湿度 65%");
        weatherMap.put("深圳", "阵雨，温度 28°C，注意带伞");
        weatherMap.put("广州", "阴天，温度 26°C，东南风 3级");
        weatherMap.put("杭州", "小雨，温度 20°C，出门记得带伞");

        String weather = weatherMap.getOrDefault(city, "抱歉，暂时没有" + city + "的天气信息");
        return weather;
    }

    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        return "当前时间：" + now.format(formatter);
    }

    @Tool(description = "计算两个整数的和")
    public int calculateSum(
            @ToolParam(description = "第一个整数") int a,
            @ToolParam(description = "第二个整数") int b) {
        return a + b;
    }

    @Tool(description = "计算两个整数的差")
    public int calculateDifference(
            @ToolParam(description = "被减数") int a,
            @ToolParam(description = "减数") int b) {
        return a - b;
    }
}
