
// ... existing code ...
package com.aaa.easyagent.web.biz.function;

import com.aaa.easyagent.biz.tool.ToolOnlineDebug;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaToolConfigReq;
import com.aaa.easyagent.core.domain.request.ToolBindRequest;
import com.aaa.easyagent.core.domain.request.ToolUnbindRequest;
import com.aaa.easyagent.core.domain.result.EaToolConfigResult;
import com.aaa.easyagent.core.service.ToolMangerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具管理控制器
 * 提供工具配置的增删改查等管理功能
 *
 * @author liuzhen.tian
 * @version 1.0 ToolManagerController.java  2025/12/22 21:55
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/tool/")
@RequiredArgsConstructor
public class ToolManagerController {

    private final ToolMangerService toolMangerService;
    private final ToolOnlineDebug toolOnlineDebug;

    /**
     * 查询官方工具列表
     *
     * @return 工具配置结果列表
     */
    @GetMapping("getDefaultTools")
    public BaseResult getDefaultTools() {
        List<EaToolConfigResult> eaToolConfigResults = toolMangerService.getDefaultTools();
        return BaseResult.buildSuc(eaToolConfigResults);
    }

    /**
     * 根据Agent ID获取工具配置列表
     *
     * @param agentId Agent标识
     * @return 工具配置结果列表
     */
    @GetMapping("getToolConfigByUserId")
    public BaseResult getToolConfigByUserId() {
        List<EaToolConfigResult> toolConfigByAgentId = toolMangerService.getToolConfigByUserId();
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
     * 复制工具配置
     *
     * @param eaToolConfigReq 工具配置请求对象
     * @return 复制结果，成功返回1，失败返回0
     */
    @PostMapping("copyTool")
    public BaseResult copyTool(@RequestBody EaToolConfigReq eaToolConfigReq) {
        return BaseResult.buildSuc(toolMangerService.copyTool(eaToolConfigReq));
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
        Object debug = null;
        try {
            debug = toolOnlineDebug.debug(eaToolConfigReq);
            return BaseResult.buildSuc(debug);
        } catch (Exception e) {
            log.error("debug error:{}", e.getMessage());
            return BaseResult.buildFail(e.getMessage());
        }

    }

    /**
     * 根据Agent ID获取已绑定的工具列表
     *
     * @param agentId Agent标识
     * @return 已绑定的工具配置列表
     */
    @GetMapping("listBoundToolsByAgentId/{agentId}")
    public BaseResult listBoundToolsByAgentId(@PathVariable Long agentId) {
        List<EaToolConfigResult> boundTools = toolMangerService.listBoundToolsByAgentId(agentId);
        return BaseResult.buildSuc(boundTools);
    }

    /**
     * 绑定工具到Agent
     *
     * @param request 工具绑定请求
     * @return 绑定结果，成功返回1，失败返回0
     */
    @PostMapping("bind")
    public BaseResult bindTool(@RequestBody ToolBindRequest request) {
        int result = toolMangerService.bindTool(request);
        return BaseResult.buildSuc(result);
    }

    /**
     * 从Agent解绑工具
     *
     * @param request 工具解绑请求
     * @return 解绑结果，成功返回1，失败返回0
     */
    @PostMapping("unbind")
    public BaseResult unbindTool(@RequestBody ToolUnbindRequest request) {
        int result = toolMangerService.unbindTool(request);
        return BaseResult.buildSuc(result);
    }

}
