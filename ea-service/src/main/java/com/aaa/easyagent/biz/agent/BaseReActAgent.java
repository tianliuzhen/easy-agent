package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.biz.agent.data.AgentContext;

/**
 * @author liuzhen.tian
 * @version 1.0 BaseReActAgent.java  2026/1/11 21:55
 */
public abstract class BaseReActAgent extends BaseAgent {

    public BaseReActAgent(AgentContext agentContext) {
        super(agentContext);
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
