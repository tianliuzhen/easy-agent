package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.common.util.FunFiledHelper;
import com.aaa.easyagent.common.util.PinyinUtil;
import com.aaa.easyagent.core.domain.DO.EaAgentDO;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.mapper.EaAgentDAO;
import com.aaa.easyagent.core.service.AgentManagerService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

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
        // 根据agent_name 拼音 生成
        String pinyin = PinyinUtil.getPinyin(req.getAgentName());
        pinyin = pinyin.length() > 255 ? pinyin.substring(0, 255) : pinyin;
        req.setAgentKey(pinyin);
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
    public EaAgentResult queryAgent(EaAgentReq req) {
        return BeanConvertUtil.beanTo(eaAgentDAO.selectByPrimaryKey(req.getId()), EaAgentResult.class);
    }

    @Override
    public int delAgent(EaAgentReq req) {
        return eaAgentDAO.deleteByPrimaryKey(req.getId());
    }

    @Override
    public EaAgentResult getAgent(Long agentId) {
        EaAgentResult eaAgentResult = BeanConvertUtil.beanTo(eaAgentDAO.selectByPrimaryKey(agentId), EaAgentResult.class);
        return eaAgentResult;
    }

    @Override
    public EaAgentResult getAgent(String agentId) {
        return this.getAgent(Long.valueOf(agentId));
    }
}
