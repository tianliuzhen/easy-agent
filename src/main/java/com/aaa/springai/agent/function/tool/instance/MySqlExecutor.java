package com.aaa.springai.agent.function.tool.instance;

import com.aaa.springai.agent.function.ToolTypeChooser;
import com.aaa.springai.agent.function.tool.ToolExecutor;
import com.aaa.springai.domain.enums.ToolTypeEnum;
import com.aaa.springai.domain.model.ToolModel;
import org.springframework.stereotype.Component;

/**
 * @author liuzhen.tian
 * @version 1.0 MySqlExecutor.java  2025/5/25 17:50
 */
@Component
@ToolTypeChooser(ToolTypeEnum.mysql)
public class MySqlExecutor implements ToolExecutor {
    @Override
    public String call(String functionInput, ToolModel toolModel) {
        return null;
    }
}
