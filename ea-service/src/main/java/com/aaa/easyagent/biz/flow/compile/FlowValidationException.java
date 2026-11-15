package com.aaa.easyagent.biz.flow.compile;

import com.aaa.easyagent.core.domain.flow.FlowValidationResult;
import lombok.Getter;

@Getter
public class FlowValidationException extends RuntimeException {
    private final FlowValidationResult validation;

    public FlowValidationException(FlowValidationResult validation) {
        super(validation.getIssues().stream().map(FlowValidationResult.Issue::getMessage)
                .findFirst().orElse("Flow 校验失败"));
        this.validation = validation;
    }
}
