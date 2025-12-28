package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.mapper.EaToolConfigDAO;
import com.aaa.easyagent.core.service.ToolMangerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqlsUtils;

import java.util.Date;
import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolMangerServiceImpl.java  2025/12/27 21:23
 */
@Service
public class ToolMangerServiceImpl implements ToolMangerService {

    @Resource
    private EaToolConfigDAO eaToolConfigDAO;


    @Override
    public List<EaToolConfigResult> getToolConfigByAgentId(Long agentId) {
        List<EaToolConfigDO> selectByWeekendSql = eaToolConfigDAO.selectByExample(
                new Example.Builder(EaToolConfigDO.class)
                        .where(WeekendSqlsUtils.andEqualTo(EaToolConfigDO::getAgentId, agentId))
                        .build());
        return BeanConvertUtil.beanTo(selectByWeekendSql, EaToolConfigResult.class);
    }

    @Override
    public int saveTool(EaToolConfigReq eaToolConfigReq) {
        if (eaToolConfigReq.getId() == null) {
            return eaToolConfigDAO.insertSelective(eaToolConfigReq);
        }

        return eaToolConfigDAO.updateByPrimaryKeySelective(eaToolConfigReq);
    }

    @Override
    public int delTool(EaToolConfigReq eaToolConfigReq) {
        return eaToolConfigDAO.deleteByPrimaryKey(eaToolConfigReq.getId());
    }

    @Override
    public EaToolConfigResult debug(EaToolConfigReq eaToolConfigReq) {
        return null;
    }
}
