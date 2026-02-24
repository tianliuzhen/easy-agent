package com.aaa.easyagent.common.util;

import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
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
        // HttpClient httpClient = HttpClient.create();
        webClient = WebClient.builder()
                .build();
    }

    /**
     * 同步GET请求
     *
     * @param url          请求地址
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T get(String url, Class<T> responseType) {
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
    public static <T, R> T exchange(HttpMethod method, String url, Map<String, String> headers, Map<String, String> requestParams, R body, Class<T> responseType) {
        return exchange(method, url, headers, requestParams, body, responseType, null, null, null);
    }

    /**
     * 通用请求方法，支持超时配置和请求参数
     *
     * @param method          请求方法
     * @param url             请求地址
     * @param headers         请求头
     * @param requestParams   请求参数
     * @param body            请求体
     * @param responseType    响应类型
     * @param connectTimeout  连接超时时间
     * @param responseTimeout 响应超时时间
     * @param maxInMemorySize 最大内存大小
     * @return 响应结果
     */
    public static <T, R> T exchange(HttpMethod method, String url, Map<String, String> headers, Map<String, String> requestParams, R body, Class<T> responseType,
                                    Integer connectTimeout, Integer responseTimeout, Integer maxInMemorySize) {
        // 创建HttpClient并配置超时
        HttpClient httpClient = HttpClient.create();

        if (connectTimeout != null) {
            httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        }

        if (responseTimeout != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(responseTimeout));
        }

        // 配置ExchangeStrategies以设置最大内存大小
        ExchangeStrategies.Builder strategiesBuilder = ExchangeStrategies.builder();
        if (maxInMemorySize != null) {
            strategiesBuilder.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize));
        }

        // 创建带连接器的WebClient
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(connector)
                .exchangeStrategies(strategiesBuilder.build());

        WebClient client = webClientBuilder.build();

        if (body == null) {
            return client.method(method)
                    .uri(uriBuilder -> {
                        return getUri(url, requestParams, uriBuilder);
                    })
                    .headers(buildHeaders(headers))
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        }

        return client.method(method)
                .uri(uriBuilder -> {
                    // 解析完整的URL，正确处理协议、主机、端口和路径
                    return getUri(url, requestParams, uriBuilder);
                })
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private static URI getUri(String url, Map<String, String> requestParams, UriBuilder uriBuilder) {
        // 解析完整的URL，正确处理协议、主机、端口和路径
        URI parsedUri = URI.create(url);
        uriBuilder.scheme(parsedUri.getScheme())
                .host(parsedUri.getHost())
                .port(parsedUri.getPort())
                .path(parsedUri.getPath());

        // 添加URL中已有的查询参数
        if (parsedUri.getQuery() != null) {
            String[] pairs = parsedUri.getQuery().split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : null;
                if (value != null) {
                    uriBuilder.queryParam(key, value);
                } else {
                    uriBuilder.queryParam(key);
                }
            }
        }

        // 添加额外的查询参数
        if (requestParams != null && !requestParams.isEmpty()) {
            requestParams.forEach((key, value) -> {
                if (value != null) {
                    uriBuilder.queryParam(key, value);
                } else {
                    uriBuilder.queryParam(key);
                }
            });
        }

        return uriBuilder.build();
    }

    /**
     * 通用HTTP请求方法，可动态指定请求类型、路径、请求体、请求头和超时配置
     *
     * @param method          HTTP请求方法 (GET, POST, PUT, DELETE等)
     * @param requestPath     请求路径/URL
     * @param requestHeader   请求头Map
     * @param requestBody     请求体对象
     * @param responseType    响应类型
     * @param connectTimeout  连接超时时间
     * @param responseTimeout 响应超时时间
     * @param maxInMemorySize 最大内存大小
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String requestPath, Map<String, String> requestHeader, R requestBody,
                                   Class<T> responseType, Integer connectTimeout, Integer responseTimeout, Integer maxInMemorySize) {
        return exchange(method, requestPath, requestHeader, null, requestBody, responseType, connectTimeout, responseTimeout, maxInMemorySize);
    }

    /**
     * 通用HTTP请求方法，可动态指定请求类型、路径、请求参数、请求体、请求头和超时配置
     *
     * @param method          HTTP请求方法 (GET, POST, PUT, DELETE等)
     * @param requestPath     请求路径/URL
     * @param requestHeader   请求头Map
     * @param requestParams   请求参数Map
     * @param requestBody     请求体对象
     * @param responseType    响应类型
     * @param connectTimeout  连接超时时间
     * @param responseTimeout 响应超时时间
     * @param maxInMemorySize 最大内存大小
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String requestPath, Map<String, String> requestHeader, Map<String, String> requestParams, R requestBody,
                                   Class<T> responseType, Integer connectTimeout, Integer responseTimeout, Integer maxInMemorySize) {
        return exchange(method, requestPath, requestHeader, requestParams, requestBody, responseType, connectTimeout, responseTimeout, maxInMemorySize);
    }

    /**
     * 通用HTTP请求方法，可动态指定请求类型、路径、请求体、请求头和超时配置
     *
     * @param method        HTTP请求方法 (GET, POST, PUT, DELETE等)
     * @param requestPath   请求路径/URL
     * @param requestHeader 请求头Map
     * @param requestBody   请求体对象
     * @param responseType  响应类型
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String requestPath, Map<String, String> requestHeader, R requestBody,
                                   Class<T> responseType) {
        return exchange(method, requestPath, requestHeader, null, requestBody, responseType, null, null, null);
    }

    /**
     * 通用HTTP请求方法，可动态指定请求类型、路径、请求参数、请求体、请求头
     *
     * @param method        HTTP请求方法 (GET, POST, PUT, DELETE等)
     * @param requestPath   请求路径/URL
     * @param requestHeader 请求头Map
     * @param requestParams 请求参数Map
     * @param requestBody   请求体对象
     * @param responseType  响应类型
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String requestPath, Map<String, String> requestHeader, Map<String, String> requestParams, R requestBody,
                                   Class<T> responseType) {
        return exchange(method, requestPath, requestHeader, requestParams, requestBody, responseType, null, null, null);
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
