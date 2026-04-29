package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.biz.agent.data.AgentMemoryConfig;
import com.aaa.easyagent.biz.agent.data.AgentModelConfig;
import com.aaa.easyagent.core.domain.DO.EaAgentDO;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author liuzhen.tian
 * @version 1.0 EaAgentResult.java  2025/12/27 21:12
 */
@Data
public class EaAgentResult extends EaAgentDO {
    private AgentModelConfig agentModelConfig;

    private String modelIcon;

    /**
     * 缓存的记忆配置对象，避免重复解析 JSON。
     * 外部代码可以通过此对象注入平台级的配置（如 maxToken）。
     */
    private AgentMemoryConfig agentMemoryConfigCache;

    public AgentModelConfig getAgentModelConfig() {
        if (StringUtils.isBlank(super.getModelConfig())) {
            return null;
        }

        return JSONObject.parseObject(super.getModelConfig(), AgentModelConfig.class);
    }


    public AgentMemoryConfig getAgentMemoryConfig() {
        if (StringUtils.isBlank(super.getMemoryConfig())) {
            return new AgentMemoryConfig();
        }
        if (agentMemoryConfigCache == null) {
            agentMemoryConfigCache = JSONObject.parseObject(super.getMemoryConfig(), AgentMemoryConfig.class);
        }
        return agentMemoryConfigCache;
    }

}
