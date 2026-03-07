package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.core.domain.DO.EaModelPlatformDO;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaModelPlatformReq;
import com.aaa.easyagent.core.domain.result.EaModelPlatformResult;
import com.aaa.easyagent.core.mapper.EaModelPlatformDAO;
import com.aaa.easyagent.core.service.ModelPlatformService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型平台配置服务实现类
 *
 * @author liuzhen.tian
 * @version 1.0 ModelPlatformServiceImpl.java  2026/3/7
 */
@Slf4j
@Service
public class ModelPlatformServiceImpl implements ModelPlatformService {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Resource
    private EaModelPlatformDAO eaModelPlatformDAO;
    
    @Override
    public List<EaModelPlatformResult> listAll() {
        try {
            // 按排序顺序查询所有模型平台
            Example example = new Example(EaModelPlatformDO.class);
            example.orderBy("sortOrder").asc();
            List<EaModelPlatformDO> modelPlatforms = eaModelPlatformDAO.selectByExample(example);
            return BeanConvertUtil.beanTo(modelPlatforms, EaModelPlatformResult.class);
        } catch (Exception e) {
            log.error("查询模型平台列表失败", e);
            throw new RuntimeException("查询模型平台列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public EaModelPlatformResult getById(Long id) {
        try {
            EaModelPlatformDO platformDO = eaModelPlatformDAO.selectByPrimaryKey(id);
            if (platformDO == null) {
                return null;
            }
            return BeanConvertUtil.beanTo(platformDO, EaModelPlatformResult.class);
        } catch (Exception e) {
            log.error("根据 ID 查询模型平台失败，id={}", id, e);
            throw new RuntimeException("根据 ID 查询模型平台失败：" + e.getMessage());
        }
    }
    
    @Override
    public int save(EaModelPlatformReq req) {
        try {
            // 验证参数
            if (!req.isValid()) {
                throw new IllegalArgumentException("模型平台标识不能为空");
            }
            
            // 将模型版本数组转换为 JSON 字符串存储
            String[] versions = req.getModelVersionsAsArray();
            if (versions != null && versions.length > 0) {
                String modelVersionsJson = ModelPlatformServiceImpl.objectMapper.writeValueAsString(versions);
                req.setModelVersions(modelVersionsJson);
            } else {
                req.setModelVersions("[]");
            }
            
            Date now = new Date();
            
            if (req.getId() == null) {
                // 新增
                req.setCreatedAt(now);
                req.setUpdatedAt(now);
                if (req.getIsActive() == null) {
                    req.setIsActive(true);
                }
                if (req.getSortOrder() == null) {
                    req.setSortOrder(0);
                }
                return eaModelPlatformDAO.insertSelective(req);
            } else {
                // 更新
                req.setUpdatedAt(now);
                // 清空不需要更新的字段
                req.setCreatedAt(null);
                return eaModelPlatformDAO.updateByPrimaryKeySelective(req);
            }
        } catch (IllegalArgumentException e) {
            log.error("保存模型平台参数验证失败", e);
            throw e;
        } catch (Exception e) {
            log.error("保存模型平台失败", e);
            throw new RuntimeException("保存模型平台失败：" + e.getMessage());
        }
    }
    
    @Override
    public int delete(EaModelPlatformReq req) {
        try {
            if (req.getId() == null) {
                throw new IllegalArgumentException("删除模型平台 ID 不能为空");
            }
            
            EaModelPlatformDO platformDO = eaModelPlatformDAO.selectByPrimaryKey(req.getId());
            if (platformDO == null) {
                throw new IllegalArgumentException("模型平台不存在");
            }
            
            return eaModelPlatformDAO.deleteByPrimaryKey(req.getId());
        } catch (IllegalArgumentException e) {
            log.error("删除模型平台参数验证失败", e);
            throw e;
        } catch (Exception e) {
            log.error("删除模型平台失败", e);
            throw new RuntimeException("删除模型平台失败：" + e.getMessage());
        }
    }
    
    @Override
    public int updateActiveStatus(Long id, Boolean isActive) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("模型平台 ID 不能为空");
            }
            
            EaModelPlatformDO platformDO = eaModelPlatformDAO.selectByPrimaryKey(id);
            if (platformDO == null) {
                throw new IllegalArgumentException("模型平台不存在");
            }
            
            EaModelPlatformDO updateDO = new EaModelPlatformDO();
            updateDO.setId(id);
            updateDO.setIsActive(isActive);
            updateDO.setUpdatedAt(new Date());
            
            return eaModelPlatformDAO.updateByPrimaryKeySelective(updateDO);
        } catch (IllegalArgumentException e) {
            log.error("更新模型平台状态参数验证失败", e);
            throw e;
        } catch (Exception e) {
            log.error("更新模型平台状态失败", e);
            throw new RuntimeException("更新模型平台状态失败：" + e.getMessage());
        }
    }
}
