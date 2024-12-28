package com.aaa.springai.web.function;

import com.aaa.springai.web.function.tool.MockWeatherService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * @author liuzhen.tian
 * @version 1.0 Config.java  2024/12/28 20:23
 */
@Configuration
public class Config {

    @Bean
    @Description("Get the weather in location") // function description
    public Function<MockWeatherService.Request, MockWeatherService.Response> currentWeather() {
        return new MockWeatherService();
    }
}
