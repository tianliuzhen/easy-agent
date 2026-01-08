package com.aaa.easyagent.biz.tool.instance;

import com.aaa.easyagent.biz.function.ToolTypeChooser;
import com.aaa.easyagent.biz.tool.ToolExecutor;
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
@ToolTypeChooser(ToolTypeEnum.HTTP)
public class HttpExecutor implements ToolExecutor<HttpReqParamsTemplate> {


    @Override
    public String call(String functionInput, ToolModel<HttpReqParamsTemplate> toolModel) {
        HttpReqParamsTemplate paramsTemplate = toolModel.getParamsTemplate();

        if (paramsTemplate.getRequestBody() != null &&
                paramsTemplate.getRequestBody().getClass() == Object.class) {
            throw new RuntimeException("paramsTemplate.getRequestBody() is Object");
        }

        // String res = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);

        HttpMethod httpMethod = HttpMethod.valueOf(paramsTemplate.getMethod().toUpperCase());
        String res = WebClientUtil.exchange(httpMethod,
                paramsTemplate.getUrl(),
                paramsTemplate.buildHeader(),
                paramsTemplate.buildRequestParams(),
                paramsTemplate.getRequestBody(),
                String.class);

        return res;
    }

}
