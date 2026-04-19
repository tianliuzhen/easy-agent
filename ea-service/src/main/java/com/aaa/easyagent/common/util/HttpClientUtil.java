package com.aaa.easyagent.common.util;

/**
 * @author liuzhen.tian
 * @version 1.0 HttpClientUtil.java  2026/4/14 23:36
 */

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HttpClient 工具类
 *
 * @author liuzhen.tian
 * @version 1.0 HttpClientUtil.java
 */
public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final CloseableHttpClient httpClient;

    // 默认超时时间（毫秒）
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 30000;

    static {
        // 使用默认配置创建HttpClient
        httpClient = HttpClients.createDefault();
    }

    /**
     * 创建自定义配置的HttpClient
     *
     * @param connectTimeout   连接超时时间
     * @param socketTimeout    读取超时时间
     * @param connectionRequestTimeout 从连接池获取连接超时时间
     * @return CloseableHttpClient
     */
    private static CloseableHttpClient createHttpClient(Integer connectTimeout,
                                                        Integer socketTimeout,
                                                        Integer connectionRequestTimeout) {
        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectTimeout(connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT)
                .setSocketTimeout(socketTimeout != null ? socketTimeout : DEFAULT_SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(connectionRequestTimeout != null ?
                        connectionRequestTimeout : DEFAULT_CONNECTION_REQUEST_TIMEOUT);

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(configBuilder.build())
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
        return get(url, null, responseType);
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
        HttpGet httpGet = new HttpGet(url);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpGet::setHeader);
        }

        return executeRequest(httpGet, responseType);
    }


    /**
     * 同步GET请求（带请求参数和请求头）
     *
     * @param url           请求地址
     * @param requestParams 请求参数
     * @param headers       请求头
     * @param responseType  响应类型
     * @return 响应结果
     */
    public static <T> T get(String url, Map<String, String> requestParams,
                            Map<String, String> headers, Class<T> responseType) {
        String fullUrl = buildUrlWithParams(url, requestParams);
        HttpGet httpGet = new HttpGet(fullUrl);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpGet::setHeader);
        }

        return executeRequest(httpGet, responseType);
    }

    /**
     * 同步POST请求（JSON格式）
     *
     * @param url          请求地址
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T post(String url, R body, Class<T> responseType) {
        return post(url, null, body, responseType);
    }

    /**
     * 同步POST请求（JSON格式，带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T post(String url, Map<String, String> headers, R body, Class<T> responseType) {
        HttpPost httpPost = new HttpPost(url);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpPost::setHeader);
        }

        // 如果没有设置Content-Type，默认使用application/json
        if (!httpPost.containsHeader("Content-Type")) {
            httpPost.setHeader("Content-Type", "application/json");
        }

        if (body != null) {
            String jsonBody = body instanceof String ? (String) body : JsonUtil.toJson(body);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        return executeRequest(httpPost, responseType);
    }

    /**
     * 同步POST请求（表单格式）
     *
     * @param url          请求地址
     * @param formParams   表单参数
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T postForm(String url, Map<String, String> formParams, Class<T> responseType) {
        return postForm(url, null, formParams, responseType);
    }

    /**
     * 同步POST请求（表单格式，带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param formParams   表单参数
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T postForm(String url, Map<String, String> headers,
                                 Map<String, String> formParams, Class<T> responseType) {
        HttpPost httpPost = new HttpPost(url);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpPost::setHeader);
        }

        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        if (formParams != null && !formParams.isEmpty()) {
            String formBody = formParams.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            httpPost.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));
        }

        return executeRequest(httpPost, responseType);
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
        return put(url, null, body, responseType);
    }

    /**
     * 同步PUT请求（带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T put(String url, Map<String, String> headers, R body, Class<T> responseType) {
        HttpPut httpPut = new HttpPut(url);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpPut::setHeader);
        }

        if (!httpPut.containsHeader("Content-Type")) {
            httpPut.setHeader("Content-Type", "application/json");
        }

        if (body != null) {
            String jsonBody = body instanceof String ? (String) body : JsonUtil.toJson(body);
            httpPut.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        return executeRequest(httpPut, responseType);
    }

    /**
     * 同步DELETE请求
     *
     * @param url          请求地址
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T delete(String url, Class<T> responseType) {
        return delete(url, null, responseType);
    }

    /**
     * 同步DELETE请求（带请求头）
     *
     * @param url          请求地址
     * @param headers      请求头
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T delete(String url, Map<String, String> headers, Class<T> responseType) {
        HttpDelete httpDelete = new HttpDelete(url);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpDelete::setHeader);
        }

        return executeRequest(httpDelete, responseType);
    }

    /**
     * 通用请求方法
     *
     * @param method       HTTP方法
     * @param url          请求地址
     * @param headers      请求头
     * @param requestParams 请求参数
     * @param body         请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String url,
                                   Map<String, String> headers,
                                   Map<String, String> requestParams,
                                   R body,
                                   Class<T> responseType) {
        return request(method, url, headers, requestParams, body, responseType, null, null);
    }

    /**
     * 通用请求方法（支持超时配置）
     *
     * @param method          HTTP方法
     * @param url             请求地址
     * @param headers         请求头
     * @param requestParams   请求参数
     * @param body            请求体
     * @param responseType    响应类型
     * @param connectTimeout  连接超时时间
     * @param socketTimeout   读取超时时间
     * @return 响应结果
     */
    public static <T, R> T request(HttpMethod method, String url,
                                   Map<String, String> headers,
                                   Map<String, String> requestParams,
                                   R body,
                                   Class<T> responseType,
                                   Integer connectTimeout,
                                   Integer socketTimeout) {
        String fullUrl = buildUrlWithParams(url, requestParams);
        HttpRequestBase request = createRequest(method, fullUrl);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(request::setHeader);
        }

        // 设置请求体（POST/PUT/PATCH）
        if (body != null && (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            if (!request.containsHeader("Content-Type")) {
                request.setHeader("Content-Type", "application/json");
            }
            String jsonBody = body instanceof String ? (String) body : JsonUtil.toJson(body);
            entityRequest.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        // 使用自定义超时配置的HttpClient
        if (connectTimeout != null || socketTimeout != null) {
            try (CloseableHttpClient customClient = createHttpClient(connectTimeout, socketTimeout, null)) {
                return executeRequestWithClient(customClient, request, responseType);
            } catch (IOException e) {
                log.error("关闭HttpClient异常", e);
                throw new RuntimeException("HTTP请求失败", e);
            }
        }

        return executeRequest(request, responseType);
    }

    /**
     * 执行HTTP请求（使用默认HttpClient）
     */
    private static <T> T executeRequest(HttpRequestBase request, Class<T> responseType) {
        return executeRequestWithClient(httpClient, request, responseType);
    }

    /**
     * 执行HTTP请求（使用指定的HttpClient）
     */
    private static <T> T executeRequestWithClient(CloseableHttpClient client,
                                                  HttpRequestBase request,
                                                  Class<T> responseType) {
        try (CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";

            if (statusCode >= 200 && statusCode < 300) {
                if (responseType == String.class) {
                    return (T) responseBody;
                }
                return JsonUtil.fromJson(responseBody, responseType);
            } else {
                log.error("HTTP请求失败，状态码: {}, 响应: {}", statusCode, responseBody);
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }
        } catch (IOException e) {
            log.error("HTTP请求异常", e);
            throw new RuntimeException("HTTP请求异常", e);
        } finally {
            request.releaseConnection();
        }
    }

    /**
     * 根据HTTP方法创建对应的请求对象
     */
    private static HttpRequestBase createRequest(HttpMethod method, String url) {
        switch (method) {
            case GET:
                return new HttpGet(url);
            case POST:
                return new HttpPost(url);
            case PUT:
                return new HttpPut(url);
            case DELETE:
                return new HttpDelete(url);
            case PATCH:
                return new HttpPatch(url);
            case HEAD:
                return new HttpHead(url);
            case OPTIONS:
                return new HttpOptions(url);
            default:
                throw new IllegalArgumentException("不支持的HTTP方法: " + method);
        }
    }

    /**
     * 构建带参数的URL
     */
    private static String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        String paramString = params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        return url.contains("?") ? url + "&" + paramString : url + "?" + paramString;
    }

    /**
     * HTTP方法枚举
     */
    public static enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    /**
     * 简单的JSON工具类（需要根据实际项目替换为具体的JSON处理库）
     */
    private static class JsonUtil {
        // 这里需要使用实际的JSON库实现，如Jackson、Gson等
        // 示例使用简单的实现，实际使用时请替换

        private static String toJson(Object obj) {
            // 实际使用时替换为：new ObjectMapper().writeValueAsString(obj)
            throw new UnsupportedOperationException("请替换为实际的JSON序列化实现");
        }

        private static <T> T fromJson(String json, Class<T> clazz) {
            // 实际使用时替换为：new ObjectMapper().readValue(json, clazz)
            throw new UnsupportedOperationException("请替换为实际的JSON反序列化实现");
        }
    }
}
