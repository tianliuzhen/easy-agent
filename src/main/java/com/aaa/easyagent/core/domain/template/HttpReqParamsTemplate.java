package com.aaa.easyagent.core.domain.template;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求参数模板类
 * 用于封装HTTP请求所需的各种参数，包括URL、请求方法、参数、请求体和请求头
 *
 * @author liuzhen.tian
 * @version 1.0 HttpReqParamsTemplate.java  2025/5/26 21:19
 */
@Data
public class HttpReqParamsTemplate extends ParamsTemplate {
    /**
     * 请求的URL地址
     */
    private String url;

    /**
     * HTTP请求方法（GET、POST、PUT、DELETE等）
     */
    private String method;

    /**
     * URL查询参数或表单参数
     */
    private List<Map<String, String>> requestParams;

    /**
     * 请求体内容，可以是任意对象
     */
    private Object requestBody;

    /**
     * HTTP请求头信息
     */
    private List<Map<String, String>> headers;

    public Map<String, String> buildRequestParams() {
        return getKeyValueMap(requestParams);
    }

    public Map<String, String> buildHeader() {
        return getKeyValueMap(headers);
    }

    private HashMap<String, String> getKeyValueMap(List<Map<String, String>> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return new HashMap<>();
        }
        return headers.stream()
                .map(item -> {
                    String key = item.get("key");
                    String value = item.get("value");
                    return Map.entry(key, value);
                })
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }
}
