package com.aaa.easyagent.biz.flow.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowState {
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public void put(String nodeId, String portKey, Object value) {
        if (value != null) {
            values.put(key(nodeId, portKey), value);
        }
    }

    public Object get(String nodeId, String portKey) {
        return values.get(key(nodeId, portKey));
    }

    public Map<String, Object> nodeOutputs(String nodeId) {
        Map<String, Object> result = new LinkedHashMap<>();
        String prefix = nodeId + ":";
        values.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), value);
            }
        });
        return result;
    }

    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private String key(String nodeId, String portKey) {
        return nodeId + ":" + portKey;
    }
}
