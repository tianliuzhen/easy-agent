package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class FlowValidationResult {
    private boolean valid;
    private List<Issue> issues = new ArrayList<>();

    public FlowValidationResult addError(String targetType, String targetId, String code, String message) {
        issues.add(new Issue().setSeverity("ERROR").setTargetType(targetType)
                .setTargetId(targetId).setCode(code).setMessage(message));
        valid = false;
        return this;
    }

    public FlowValidationResult addWarning(String targetType, String targetId, String code, String message) {
        issues.add(new Issue().setSeverity("WARNING").setTargetType(targetType)
                .setTargetId(targetId).setCode(code).setMessage(message));
        return this;
    }

    @Data
    @Accessors(chain = true)
    public static class Issue {
        private String severity;
        private String targetType;
        private String targetId;
        private String code;
        private String message;
    }
}
