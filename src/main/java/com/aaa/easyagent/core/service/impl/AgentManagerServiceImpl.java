package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.mapper.EaAgentDAO;
import com.aaa.easyagent.core.service.AgentManagerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentManagerServiceImpl.java  2025/12/27 21:09
 */
@Service
public class AgentManagerServiceImpl implements AgentManagerService {
    @Resource
    private EaAgentDAO eaAgentDAO;

    @Override
    public int save(EaAgentReq req) {
        return eaAgentDAO.save(req);
    }

    @Override
    public List<EaAgentResult> selectAll() {
        return BeanConvertUtil.beanTo(eaAgentDAO.selectAll(), EaAgentResult.class);
    }

    @Override
    public int delAgent(EaAgentReq req) {
        return eaAgentDAO.deleteByPrimaryKey(req.getId());
    }
}
