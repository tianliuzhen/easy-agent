package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.util.HttpClientUtil;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.HttpReqParamsTemplate;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * org.springframework.ai.tool.function.FunctionToolCallback
 *
 * @author liuzhen.tian
 * @version 1.0 HttpExecutor.java  2025/2/23 19:34
 */
@Slf4j
@Component
@ToolTypeChooser(ToolTypeEnum.HTTP)
public class HttpExecutor implements ToolExecutor<HttpReqParamsTemplate> {


    public static final String $_REQUEST_PARAMS = "$.requestParams";
    public static final String $_HEADERS = "$.headers";
    public static final String $_REQUEST_BODY = "$.requestBody";

    @Override
    public String call(String functionInput, ToolDefinition<HttpReqParamsTemplate> toolDefinition) {
        boolean debug = toolDefinition.isDebug();
        log.info("HttpExecutor call {}: {}", debug ? "debug" : "", functionInput);
        if (StringUtils.isBlank(functionInput) && !debug) {
            throw new AgentToolException("functionInput 不能为空");
        }

        // 通过jsonPath 动态赋值
        List<InputTypeSchema> inputTypeSchemas = toolDefinition.getInputTypeSchemas();
        if (CollectionUtils.isEmpty(inputTypeSchemas) && !debug) {
            throw new AgentToolException("inputTypeSchemas 不能为空");
        }

        // http实例参数
        HttpReqParamsTemplate paramsTemplate = toolDefinition.getParamsTemplate();
        // 请求Header
        Map<String, String> headers = paramsTemplate.buildHeader();
        // 请求request 参数
        Map<String, String> requestParams = paramsTemplate.buildRequestParams();
        // 请求Body
        Object requestBody = paramsTemplate.getRequestBody();

        Map<String, InputTypeSchema> inputTypeSchemaMap = inputTypeSchemas.stream().collect(Collectors.toMap(InputTypeSchema::getName, Function.identity(), (o, n) -> o));
        JSONObject functionInputJson = JSON.parseObject(functionInput);
        if (!CollectionUtils.isEmpty(functionInputJson)) {
            functionInputJson.forEach((name, value) -> {
                if (!inputTypeSchemaMap.containsKey(name)) {
                    throw new AgentToolException(name + ": 无法匹配inputTypeSchemas");
                }
                InputTypeSchema inputTypeSchema = inputTypeSchemaMap.get(name);
                if (StringUtils.isBlank(inputTypeSchema.getReferenceValue())) {
                    throw new AgentToolException(name + ":缺少referenceValue");
                }

            /*
              jsonPath 赋值
              $.requestParams.xxx = 1
              $.requestParams.yyy = 2
              $.requestBody[0].name = tom
              $.requestBody.status = running
             */
                if (inputTypeSchema.getReferenceValue().startsWith($_REQUEST_PARAMS)) {
                    JSONPath.set(requestParams, inputTypeSchema.getReferenceValue().replace($_REQUEST_PARAMS, ""), value);
                }
                if (inputTypeSchema.getReferenceValue().startsWith($_HEADERS)) {
                    JSONPath.set(headers, inputTypeSchema.getReferenceValue().replace($_HEADERS, ""), value);
                }
                if (inputTypeSchema.getReferenceValue().startsWith($_REQUEST_BODY)) {
                    JSONPath.set(requestBody, inputTypeSchema.getReferenceValue().replace($_REQUEST_BODY, ""), value);
                }
            });
        }


        if (requestBody != null &&
                requestBody.getClass() == Object.class) {
            throw new AgentToolException("paramsTemplate.getRequestBody() is Object");
        }

        // String res = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);
        HttpClientUtil.HttpMethod httpMethod = HttpClientUtil.HttpMethod.valueOf(paramsTemplate.getMethod().toUpperCase());
        log.info("HttpExecutor call: url:{} \n header:{} \n requestParam:{} \n requestBody:{}",
                paramsTemplate.getUrl(), headers, requestParams, requestBody);
        String res = HttpClientUtil.request(httpMethod,
                paramsTemplate.getUrl(),
                headers,
                requestParams,
                requestBody,
                String.class);
        log.info("HttpExecutor call resp: {}", res);
        return res;
    }

}
