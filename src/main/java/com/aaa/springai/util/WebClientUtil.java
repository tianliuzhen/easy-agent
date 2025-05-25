package com.aaa.springai.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

/**
 * WebClient 工具类（by deepseek）
 *
 * @author liuzhen.tian
 * @version 1.0 WebClient.java  2025/5/25 21:10
 */
public class WebClientUtil {
    private static WebClient webClient;


    /**
     * 构造函数，使用默认配置
     */
    static {
        webClient = WebClient.create();
    }

    /**
     * 同步GET请求
     *
     * @param url          请求地址
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static  <T> T get(String url, Class<T> responseType) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 同步GET请求（带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T get(String url, Map<String, String> headers, Class<T> responseType) {
        return webClient.get()
                .uri(url)
                .headers(buildHeaders(headers))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 异步GET请求
     *
     * @param url          请求地址
     * @param responseType 响应类型
     * @return Mono对象
     */
    public static <T> Mono<T> getAsync(String url, Class<T> responseType) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * 同步POST请求
     *
     * @param url          请求地址
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T post(String url, R body, Class<T> responseType) {
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 同步POST请求（带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T post(String url, Map<String, String> headers, R body, Class<T> responseType) {
        return webClient.post()
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 异步POST请求
     *
     * @param url          请求地址
     * @param body         请求体
     * @param responseType 响应类型
     * @return Mono对象
     */
    public static <T, R> Mono<T> postAsync(String url, R body, Class<T> responseType) {
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * 同步PUT请求
     *
     * @param url          请求地址
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T put(String url, R body, Class<T> responseType) {
        return webClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 同步DELETE请求
     *
     * @param url          请求地址
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T delete(String url, Class<T> responseType) {
        return webClient.delete()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 通用请求方法
     *
     * @param method       请求方法
     * @param url          请求地址
     * @param headers      请求头
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T exchange(HttpMethod method, String url, Map<String, String> headers, R body, Class<T> responseType) {
        return webClient.method(method)
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    /**
     * 处理错误响应
     *
     * @param response 响应
     * @return 错误信息
     */
    public static Mono<String> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("Unknown error")
                .flatMap(errorBody -> Mono.error(new RuntimeException(
                        "HTTP error: " + response.statusCode() + ", Body: " + errorBody)));
    }

    /**
     * 构建请求头
     *
     * @param headers 请求头Map
     * @return 请求头Consumer
     */
    private static Consumer<HttpHeaders> buildHeaders(Map<String, String> headers) {
        return httpHeaders -> {
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
        };
    }
}
