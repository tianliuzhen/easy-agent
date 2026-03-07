package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaModelPlatformReq;
import com.aaa.easyagent.core.domain.result.EaModelPlatformResult;

import java.util.List;

/**
 * 模型平台配置服务
 *
 * @author liuzhen.tian
 * @version 1.0 ModelPlatformService.java  2026/3/7
 */
public interface ModelPlatformService {
    
    /**
     * 获取所有模型平台列表
     *
     * @return 模型平台结果列表
     */
    List<EaModelPlatformResult> listAll();
    
    /**
     * 根据 ID 获取模型平台信息
     *
     * @param id 模型平台 ID
     * @return 模型平台结果对象，如果不存在则返回 null
     */
    EaModelPlatformResult getById(Long id);
    
    /**
     * 保存模型平台配置
     *
     * @param req 模型平台请求对象
     * @return 保存成功返回 1，否则返回 0
     */
    int save(EaModelPlatformReq req);
    
    /**
     * 删除模型平台配置
     *
     * @param req 模型平台请求对象
     * @return 删除成功返回 1，否则返回 0
     */
    int delete(EaModelPlatformReq req);
    
    /**
     * 更新模型平台启用状态
     *
     * @param id 模型平台 ID
     * @param isActive 是否启用
     * @return 更新成功返回 1，否则返回 0
     */
    int updateActiveStatus(Long id, Boolean isActive);
}
