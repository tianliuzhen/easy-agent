package com.aaa.easyagent.biz.agent.function.tool.example;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * @author liuzhen.tian
 * @version 1.0 MockWeatherService.java  2024/12/28 20:22
 */
public class MockWeatherService implements Function<MockWeatherService.Request, MockWeatherService.Response> {

    public enum Unit {C, F}

    public record Request(@JsonPropertyDescription("从问题中获取，不要编造，格式如：杭州") String location, Unit unit) {
    }

    public record Response(double temp, Unit unit) {
    }

    @Override
    public Response apply(Request request) {
        if (request.location.equals("杭州")){
            return new Response(15, Unit.C);
        }
        if (request.location.equals("上海")){
            return new Response(16, Unit.C);
        }

        return new Response(-10.0, Unit.F);
    }
}
