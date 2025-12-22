package com.aaa.easyagent.core.domain.template;

import lombok.Data;

import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 HttpReqParamsTemplate.java  2025/5/26 21:19
 */
@Data
public class HttpReqParamsTemplate extends ParamsTemplate {
    private String url;
    private String methodType;

    private Object requestParams;
    private Object requestBody;

    private Map<String, String> headers;
}
