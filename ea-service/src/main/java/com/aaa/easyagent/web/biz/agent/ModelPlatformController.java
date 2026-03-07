package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaModelPlatformReq;
import com.aaa.easyagent.core.domain.result.EaModelPlatformResult;
import com.aaa.easyagent.core.service.ModelPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型平台配置控制器
 *
 * @author liuzhen.tian
 * @version 1.0 ModelPlatformController.java  2026/3/7
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/modelPlatform")
@RequiredArgsConstructor
public class ModelPlatformController {
    
    private final ModelPlatformService modelPlatformService;
    
    /**
     * 查询所有模型平台列表
     *
     * @return 模型平台列表
     */
    @PostMapping("/list")
    public BaseResult list() {
        try {
            List<EaModelPlatformResult> result = modelPlatformService.listAll();
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("查询模型平台列表失败", e);
            return BaseResult.buildFail("查询模型平台列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据 ID 查询模型平台详情
     *
     * @param req 请求对象，包含 ID
     * @return 模型平台详情
     */
    @PostMapping("/getById")
    public BaseResult getById(@RequestBody EaModelPlatformReq req) {
        try {
            if (req.getId() == null) {
                return BaseResult.buildFail("模型平台 ID 不能为空");
            }
            EaModelPlatformResult result = modelPlatformService.getById(req.getId());
            if (result == null) {
                return BaseResult.buildFail("模型平台不存在");
            }
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("根据 ID 查询模型平台失败", e);
            return BaseResult.buildFail("查询模型平台详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 保存模型平台配置 (新增或更新)
     *
     * @param req 请求对象
     * @return 保存结果
     */
    @PostMapping("/save")
    public BaseResult save(@RequestBody EaModelPlatformReq req) {
        try {
            int result = modelPlatformService.save(req);
            if (result > 0) {
                return BaseResult.buildSuc("保存成功");
            } else {
                return BaseResult.buildFail("保存失败");
            }
        } catch (IllegalArgumentException e) {
            log.error("保存模型平台参数验证失败", e);
            return BaseResult.buildFail(e.getMessage());
        } catch (Exception e) {
            log.error("保存模型平台失败", e);
            return BaseResult.buildFail("保存失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除模型平台配置
     *
     * @param req 请求对象
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResult delete(@RequestBody EaModelPlatformReq req) {
        try {
            int result = modelPlatformService.delete(req);
            if (result > 0) {
                return BaseResult.buildSuc("删除成功");
            } else {
                return BaseResult.buildFail("删除失败");
            }
        } catch (IllegalArgumentException e) {
            log.error("删除模型平台参数验证失败", e);
            return BaseResult.buildFail(e.getMessage());
        } catch (Exception e) {
            log.error("删除模型平台失败", e);
            return BaseResult.buildFail("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新模型平台启用状态
     *
     * @param req 请求对象，包含 id 和 isActive
     * @return 更新结果
     */
    @PostMapping("/updateActiveStatus")
    public BaseResult updateActiveStatus(@RequestBody EaModelPlatformReq req) {
        try {
            if (req.getId() == null || req.getIsActive() == null) {
                return BaseResult.buildFail("参数错误");
            }
            int result = modelPlatformService.updateActiveStatus(req.getId(), req.getIsActive());
            if (result > 0) {
                return BaseResult.buildSuc("更新成功");
            } else {
                return BaseResult.buildFail("更新失败");
            }
        } catch (IllegalArgumentException e) {
            log.error("更新模型平台状态参数验证失败", e);
            return BaseResult.buildFail(e.getMessage());
        } catch (Exception e) {
            log.error("更新模型平台状态失败", e);
            return BaseResult.buildFail("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询所有模型平台 (兼容旧接口的 Map 格式返回)
     * 用于替换原来的 ModelTypeEnum.getAll() 方法
     *
     * @return Map<模型平台标识，Map<属性名，属性值>>
     */
    @PostMapping("/queryChatModelTypeList")
    public BaseResult queryChatModelTypeList() {
        try {
            List<EaModelPlatformResult> platforms = modelPlatformService.listAll();
            
            // 转换为 Map 格式，与原来的枚举返回格式保持一致
            Map<String, HashMap<String, Object>> result = new HashMap<>();
            
            for (EaModelPlatformResult platform : platforms) {
                HashMap<String, Object> platformInfo = new HashMap<>();
                platformInfo.put("model", platform.getModelPlatform());
                platformInfo.put("desc", platform.getModelDesc());
                platformInfo.put("links", platform.getOfficialWebsite());
                platformInfo.put("defaultBaseUrl", platform.getBaseUrl());
                platformInfo.put("icon", platform.getIcon());
                platformInfo.put("isActive", platform.getIsActive());
                platformInfo.put("sortOrder", platform.getSortOrder());
                platformInfo.put("modelVersions", platform.getModelVersionArray());
                
                result.put(platform.getModelPlatform(), platformInfo);
            }
            
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("查询模型平台类型列表失败", e);
            return BaseResult.buildFail("查询模型平台类型列表失败：" + e.getMessage());
        }
    }
}
