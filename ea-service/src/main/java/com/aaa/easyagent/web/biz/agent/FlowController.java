package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaFlowReq;
import com.aaa.easyagent.core.domain.result.EaFlowResult;
import com.aaa.easyagent.core.service.FlowAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 多 Agent 编排（Flow）后台管理控制器：列表 / 详情 / 保存 / 删除。
 *
 * @author liuzhen.tian
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowAdminService flowAdminService;

    /**
     * 编排列表（不含成员节点）。
     */
    @GetMapping("list")
    public BaseResult list() {
        try {
            List<EaFlowResult> results = flowAdminService.listFlow();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取编排列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 编排详情（含有序成员节点）。
     *
     * @param id 编排 ID
     */
    @GetMapping("detail/{id}")
    public BaseResult detail(@PathVariable Long id) {
        try {
            EaFlowResult result = flowAdminService.getFlow(id);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("获取编排详情失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 保存编排（id 为空新增，否则更新），成员节点全量替换。
     *
     * @param req 编排请求
     * @return 编排 ID
     */
    @PostMapping("save")
    public BaseResult save(@RequestBody EaFlowReq req) {
        try {
            Long flowId = flowAdminService.saveFlow(req);
            return BaseResult.buildSuc(flowId);
        } catch (Exception e) {
            log.error("保存编排失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 删除编排及其成员节点。
     *
     * @param id 编排 ID
     */
    @DeleteMapping("delete/{id}")
    public BaseResult delete(@PathVariable Long id) {
        try {
            boolean success = flowAdminService.deleteFlow(id);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("删除编排失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
