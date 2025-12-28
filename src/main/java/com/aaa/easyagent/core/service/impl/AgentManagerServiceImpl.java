package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.common.util.FunFiledHelper;
import com.aaa.easyagent.core.domain.DO.EaAgentDO;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.mapper.EaAgentDAO;
import com.aaa.easyagent.core.service.AgentManagerService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.Fn;
import tk.mybatis.mapper.weekend.WeekendSqls;
import tk.mybatis.mapper.weekend.WeekendSqlsUtils;
import tk.mybatis.mapper.weekend.reflection.Reflections;

import java.util.Date;
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
        if (req.getId() == null) {
            return eaAgentDAO.insertSelective(req);
        }
        return eaAgentDAO.updateByPrimaryKeySelective(req);
    }

    @Override
    public List<EaAgentResult> listAgent(EaAgentReq req) {
        String agentName = req.getAgentName();

        Example example = new Example(EaAgentDO.class);
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(agentName)) {
            String property = FunFiledHelper.getFieldName(EaAgentReq::getAgentName);
            criteria.andLike(property, "%" + agentName + "%");
        }

        return BeanConvertUtil.beanTo(eaAgentDAO.selectByExample(example), EaAgentResult.class);
    }

    @Override
    public int delAgent(EaAgentReq req) {
        return eaAgentDAO.deleteByPrimaryKey(req.getId());
    }
}
