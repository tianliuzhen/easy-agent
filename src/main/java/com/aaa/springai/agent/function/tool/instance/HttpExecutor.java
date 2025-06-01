package com.aaa.springai.agent.function.tool.instance;

import com.aaa.springai.agent.function.ToolTypeChooser;
import com.aaa.springai.agent.function.tool.ToolExecutor;
import com.aaa.springai.domain.enums.ToolTypeEnum;
import com.aaa.springai.domain.model.ToolModel;
import com.aaa.springai.domain.template.HttpReqParamsTemplate;
import com.aaa.springai.util.WebClientUtil;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * org.springframework.ai.tool.function.FunctionToolCallback
 *
 * @author liuzhen.tian
 * @version 1.0 HttpExecutor.java  2025/2/23 19:34
 */
@Component
@ToolTypeChooser(ToolTypeEnum.common_http)
public class HttpExecutor implements ToolExecutor {


    @Override
    public String call(String functionInput, ToolModel toolModel) {
        HttpReqParamsTemplate paramsTemplate = (HttpReqParamsTemplate) toolModel.getParamsTemplate();


        // String res = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);

        HttpMethod httpMethod = HttpMethod.valueOf(paramsTemplate.getMethodType().toUpperCase());
        String res = WebClientUtil.exchange(httpMethod,
                paramsTemplate.getUrl(),
                paramsTemplate.getHeaders(),
                paramsTemplate.getRequestBody(),
                String.class);

        return res;
    }

}
