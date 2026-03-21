package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.core.domain.DO.EaToolConfigDO;
import com.aaa.easyagent.core.domain.DO.EaToolRelationDO;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.request.ToolBindRequest;
import com.aaa.easyagent.core.domain.request.ToolUnbindRequest;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.mapper.EaToolConfigDAO;
import com.aaa.easyagent.core.mapper.EaToolRelationDAO;
import com.aaa.easyagent.core.service.ToolMangerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqlsUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolMangerServiceImpl.java  2025/12/27 21:23
 */
@Service
public class ToolMangerServiceImpl implements ToolMangerService {

    @Resource
    private EaToolConfigDAO eaToolConfigDAO;

    @Resource
    private EaToolRelationDAO eaToolRelationDAO;

    @Override
    public List<EaToolConfigResult> getDefaultTools() {
        List<EaToolConfigDO> selectByWeekendSql = eaToolConfigDAO.selectByExample(
                new Example.Builder(EaToolConfigDO.class)
                        .where(WeekendSqlsUtils.andEqualTo(EaToolConfigDO::getAgentId, 0L))
                        .build());
        return BeanConvertUtil.beanTo(selectByWeekendSql, EaToolConfigResult.class);
    }

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
    public int copyTool(EaToolConfigReq eaToolConfigReq) {
        return eaToolConfigDAO.insertSelective(eaToolConfigReq);
    }

    @Override
    public EaToolConfigResult debug(EaToolConfigReq eaToolConfigReq) {
        return null;
    }

    @Override
    public List<EaToolConfigResult> listBoundToolsByAgentId(Long agentId) {
        // 1. 查询agentId关联的所有工具关系
        Example relationExample = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, agentId))
                .build();
        List<EaToolRelationDO> relations = eaToolRelationDAO.selectByExample(relationExample);

        if (relations.isEmpty()) {
            return List.of();
        }

        // 2. 获取所有关联的工具配置ID
        List<Long> toolConfigIds = relations.stream()
                .map(EaToolRelationDO::getToolConfigId)
                .collect(Collectors.toList());

        // 3. 查询对应的工具配置
        Example toolExample = new Example.Builder(EaToolConfigDO.class)
                .where(WeekendSqlsUtils.andIn(EaToolConfigDO::getId, toolConfigIds))
                .build();
        List<EaToolConfigDO> toolConfigs = eaToolConfigDAO.selectByExample(toolExample);

        // 4. 转换为结果对象
        return BeanConvertUtil.beanTo(toolConfigs, EaToolConfigResult.class);
    }

    @Override
    @Transactional
    public int bindTool(ToolBindRequest request) {
        // 检查是否已存在相同的绑定关系
        Example example = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, Long.parseLong(request.getAgentId()))
                .andEqualTo(EaToolRelationDO::getToolConfigId, request.getToolConfigId()))
                .build();

        List<EaToolRelationDO> existingRelations = eaToolRelationDAO.selectByExample(example);
        if (!existingRelations.isEmpty()) {
            // 已存在绑定关系，返回成功
            return 1;
        }

        // 创建新的绑定关系
        EaToolRelationDO relation = new EaToolRelationDO();
        relation.setAgentId(Long.parseLong(request.getAgentId()));
        relation.setToolConfigId(request.getToolConfigId());
        relation.setCreateTime(new Date());
        relation.setUpdateTime(new Date());
        relation.setCreator(request.getCreator());

        return eaToolRelationDAO.insertSelective(relation);
    }

    @Override
    @Transactional
    public int unbindTool(ToolUnbindRequest request) {
        // 删除绑定关系
        Example example = new Example.Builder(EaToolRelationDO.class)
                .where(WeekendSqlsUtils.andEqualTo(EaToolRelationDO::getAgentId, Long.parseLong(request.getAgentId()))
                .andEqualTo(EaToolRelationDO::getToolConfigId, request.getToolConfigId()))
                .build();

        return eaToolRelationDAO.deleteByExample(example);
    }
}
