package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.core.domain.model.AgentModel;

/**
 * @author liuzhen.tian
 * @version 1.0 BaseReActAgent.java  2026/1/11 21:55
 */
public abstract class BaseReActAgent extends BaseAgent {

    public BaseReActAgent(AgentModel agentModel) {
        super(agentModel);
    }

    /**
     * 思考
     *
     * @return
     */
    public abstract AgentOutput think();

    /**
     * 行动
     *
     * @return
     */
    public abstract String act(FunctionUseAction functionUseAction);
}
