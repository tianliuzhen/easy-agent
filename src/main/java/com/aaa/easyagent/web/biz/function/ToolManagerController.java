
// ... existing code ...
package com.aaa.easyagent.web.biz.function;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.ToolMangerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具管理控制器
 * 提供工具配置的增删改查等管理功能
 *
 * @author liuzhen.tian
 * @version 1.0 ToolManagerController.java  2025/12/22 21:55
 */
@RestController
@RequestMapping("eaAgent/tool/")
@RequiredArgsConstructor
public class ToolManagerController {

    private final ToolMangerService toolMangerService;

    /**
     * 根据Agent ID获取工具配置列表
     *
     * @param agentId Agent标识
     * @return 工具配置结果列表
     */
    @GetMapping("getToolConfigByAgentId/{agentId}")
    public BaseResult getToolConfigByAgentId(@PathVariable Long agentId) {
        List<EaToolConfigResult> toolConfigByAgentId = toolMangerService.getToolConfigByAgentId(agentId);
        return BaseResult.buildSuc(toolConfigByAgentId);
    }

    /**
     * 删除工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象
     * @return 删除结果，成功返回1，失败返回0
     */
    @DeleteMapping("delTool")
    public BaseResult delTool(@RequestBody EaToolConfigReq eaToolConfigReq) {
        return BaseResult.buildSuc(toolMangerService.delTool(eaToolConfigReq));
    }

    /**
     * 添加工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象
     * @return 添加结果
     */
    @PostMapping("addTool")
    public BaseResult addTool(@RequestBody EaToolConfigReq eaToolConfigReq) {
        // 实现添加工具配置逻辑
        return BaseResult.buildSuc(toolMangerService.saveTool(eaToolConfigReq));
    }

    /**
     * 添加工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象
     * @return 添加结果
     */
    @PostMapping("debug")
    public BaseResult debug(@RequestBody EaToolConfigReq eaToolConfigReq) {
        // 实现添加工具配置逻辑
        return BaseResult.buildSuc(toolMangerService.debug(eaToolConfigReq));
    }

}
