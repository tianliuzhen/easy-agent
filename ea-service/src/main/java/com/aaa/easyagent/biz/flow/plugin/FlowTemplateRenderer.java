package com.aaa.easyagent.biz.flow.plugin;

import com.aaa.easyagent.common.util.JacksonUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FlowTemplateRenderer {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.:-]+)\\s*}}");

    public String render(String template, Map<String, Object> values) {
        if (template == null || template.isBlank()) {
            return stringify(values.values().stream().findFirst().orElse(""));
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Object value = resolve(values, matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(stringify(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Object resolve(Map<String, Object> values, String path) {
        Object current = values;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
        }
        return current;
    }

    private String stringify(Object value) {
        if (value == null) return "";
        if (value instanceof String string) return string;
        return JacksonUtil.beanToStr(value);
    }
}
