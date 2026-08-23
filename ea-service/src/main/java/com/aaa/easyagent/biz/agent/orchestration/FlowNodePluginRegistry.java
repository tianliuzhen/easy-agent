package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FlowNodePluginRegistry {
    private final Map<String, FlowNodePlugin> plugins = new LinkedHashMap<>();

    public FlowNodePluginRegistry(List<FlowNodePlugin> pluginList) {
        pluginList.forEach(plugin -> {
            NodeMetadata metadata = plugin.metadata();
            String key = key(metadata.getType(), metadata.getVersion());
            if (plugins.putIfAbsent(key, plugin) != null) {
                throw new IllegalStateException("重复的 Flow 节点插件：" + key);
            }
        });
    }

    public FlowNodePlugin get(String type, String version) {
        FlowNodePlugin plugin = plugins.get(key(type, version == null ? "1" : version));
        if (plugin == null) {
            throw new AgentException("未注册 Flow 节点插件：" + type + "@" + version);
        }
        return plugin;
    }

    public boolean contains(String type, String version) {
        return plugins.containsKey(key(type, version == null ? "1" : version));
    }

    public List<NodeMetadata> metadata() {
        return plugins.values().stream().map(FlowNodePlugin::metadata).toList();
    }

    private String key(String type, String version) {
        return type + "@" + version;
    }
}
