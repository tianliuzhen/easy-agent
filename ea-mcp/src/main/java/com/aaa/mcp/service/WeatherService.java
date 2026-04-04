package com.aaa.mcp.service;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 天气查询服务 - MCP 工具实现
 */
@Service
public class WeatherService {

    private final RestTemplate restTemplate;

    public WeatherService() {
        // 配置超时时间，避免 SSL 握手超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 连接超时 30 秒
        factory.setReadTimeout(30000);    // 读取超时 30 秒
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 查询天气信息
     *
     * @param city 城市名称
     * @return 天气信息
     */
    @Tool(description = "查询指定城市的天气信息，包括温度、湿度、风速等数据")
    public WeatherResponse getWeather(
            @ToolParam(description = "城市名称：北京") String city) {
        try {
            // 使用免费的天气 API (Open-Meteo - 无需 API key)
            // 首先需要将城市名转换为经纬度坐标
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + city + "&count=1&language=zh&format=json";

            GeoResponse geoResponse = restTemplate.getForObject(geoUrl, GeoResponse.class);
            if (geoResponse == null || geoResponse.results == null || geoResponse.results.isEmpty()) {
                return new WeatherResponse(city, "未找到该城市信息", null, null, null, null);
            }

            GeoResult geo = geoResponse.results.get(0);
            double lat = geo.latitude;
            double lon = geo.longitude;

            // 获取天气信息
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto",
                lat, lon
            );

            WeatherData weatherData = restTemplate.getForObject(weatherUrl, WeatherData.class);
            if (weatherData != null && weatherData.current != null) {
                return new WeatherResponse(
                    city,
                    getWeatherDescription(weatherData.current.weather_code),
                    weatherData.current.temperature_2m,
                    weatherData.current.relative_humidity_2m,
                    weatherData.current.wind_speed_10m,
                    geo.timezone
                );
            }

            return new WeatherResponse(city, "无法获取天气数据", null, null, null, null);

        } catch (HttpClientErrorException e) {
            return new WeatherResponse(city, "请求天气服务失败: " + e.getMessage(), null, null, null, null);
        } catch (Exception e) {
            return new WeatherResponse(city, "发生错误: " + e.getMessage(), null, null, null, null);
        }
    }

    /**
     * 根据 WMO 天气代码获取天气描述
     */
    private String getWeatherDescription(Integer code) {
        if (code == null) return "未知";

        return switch (code) {
            case 0 -> "晴朗";
            case 1 -> "大部晴朗";
            case 2 -> "多云";
            case 3 -> "阴天";
            case 45, 48 -> "有雾";
            case 51, 53, 55 -> "毛毛雨";
            case 61, 63, 65 -> "雨";
            case 71, 73, 75 -> "雪";
            case 77 -> "雪粒";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "雷暴伴有冰雹";
            default -> "未知天气";
        };
    }

    // ========== 数据类 ==========

    @JsonClassDescription("天气查询响应结果")
    public record WeatherResponse(
        @JsonPropertyDescription("城市名称")
        String city,

        @JsonPropertyDescription("天气描述，如：晴朗、多云、小雨等")
        String description,

        @JsonPropertyDescription("温度，单位：摄氏度")
        Double temperature,

        @JsonPropertyDescription("湿度，单位：百分比")
        Integer humidity,

        @JsonPropertyDescription("风速，单位：公里/小时")
        Double windSpeed,

        @JsonPropertyDescription("时区")
        String timezone
    ) {
        @Override
        public String toString() {
            return String.format(
                "%s 天气: %s%n温度: %.1f°C%n湿度: %d%%n风速: %.1f km/h%n时区: %s",
                city, description,
                temperature != null ? temperature : 0,
                humidity != null ? humidity : 0,
                windSpeed != null ? windSpeed : 0,
                timezone != null ? timezone : "未知"
            );
        }
    }

    private static class GeoResponse {
        @JsonProperty("results")
        java.util.List<GeoResult> results;
    }

    private static class GeoResult {
        @JsonProperty("latitude")
        double latitude;

        @JsonProperty("longitude")
        double longitude;

        @JsonProperty("timezone")
        String timezone;
    }

    private static class WeatherData {
        @JsonProperty("current")
        CurrentWeather current;
    }

    private static class CurrentWeather {
        @JsonProperty("temperature_2m")
        double temperature_2m;

        @JsonProperty("relative_humidity_2m")
        int relative_humidity_2m;

        @JsonProperty("weather_code")
        int weather_code;

        @JsonProperty("wind_speed_10m")
        double wind_speed_10m;
    }
}
