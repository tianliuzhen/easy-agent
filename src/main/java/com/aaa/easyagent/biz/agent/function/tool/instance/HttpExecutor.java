package com.aaa.easyagent.biz.agent.function.tool.instance;

import com.aaa.easyagent.biz.agent.function.ToolTypeChooser;
import com.aaa.easyagent.biz.agent.function.tool.ToolExecutor;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.core.domain.template.HttpReqParamsTemplate;
import com.aaa.easyagent.common.util.WebClientUtil;
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

        if (paramsTemplate.getRequestBody() != null &&
                paramsTemplate.getRequestBody().getClass() == Object.class) {
            throw new RuntimeException("paramsTemplate.getRequestBody() is Object");
        }

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
