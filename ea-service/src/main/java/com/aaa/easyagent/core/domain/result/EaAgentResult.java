package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.biz.agent.data.AgentModelConfig;
import com.aaa.easyagent.core.domain.DO.EaAgentDO;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

/**
 * @author liuzhen.tian
 * @version 1.0 EaAgentResult.java  2025/12/27 21:12
 */
public class EaAgentResult extends EaAgentDO {
    private AgentModelConfig agentModelConfig;



    public AgentModelConfig getAgentModelConfig() {
        if (StringUtils.isBlank(super.getModelConfig())) {
            return null;
        }

        return JSONObject.parseObject(super.getModelConfig(), AgentModelConfig.class);
    }


}
