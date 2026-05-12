package com.aaa.easyagent.web.biz.knowledge;

import com.aaa.easyagent.biz.agent.service.KnowledgeService;
import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.*;
import com.aaa.easyagent.core.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeController.java  2026/2/1 0:00
 */
@Slf4j
@RestController
@RequestMapping("knowledge/")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeService knowledgeService;

    /**
     * 上传文档
     *
     * @param request 上传请求（JSON格式）
     * @param file    文件
     * @return 上传结果
     */
    @PostMapping("upload")
    public BaseResult<EaKnowledgeBaseDO> uploadDocument(
            @RequestPart("request") KnowledgeBaseUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        try {
            EaKnowledgeBaseDO kb = knowledgeService.uploadDocument(request, file);
            return BaseResult.buildSuc(kb);
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 查询知识库列表
     *
     * @return 知识库列表
     */
    @PostMapping("list")
    public BaseResult<List<EaKnowledgeBaseDO>> listKnowledgeBase() {
        try {
            List<EaKnowledgeBaseDO> list = knowledgeBaseService.listKnowledgeBase();
            return BaseResult.buildSuc(list);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据Agent ID查询知识库列表
     *
     * @param agentId Agent ID
     * @return 知识库列表
     */
    @PostMapping("listByAgentId")
    public BaseResult<List<EaKnowledgeBaseDO>> listKnowledgeBaseByAgentId(@RequestParam("agentId") String agentId) {
        try {
            List<EaKnowledgeBaseDO> list = knowledgeBaseService.listKnowledgeBaseByAgentId(agentId);
            return BaseResult.buildSuc(list);
        } catch (Exception e) {
            log.error("根据Agent ID查询知识库列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据条件查询知识库列表（使用Request对象）
     *
     * @param request 查询请求
     * @return 知识库列表
     */
    @PostMapping("listByCondition")
    public BaseResult<List<EaKnowledgeBaseDO>> listKnowledgeBaseByCondition(@RequestBody KnowledgeBaseQueryRequest request) {
        try {
            List<EaKnowledgeBaseDO> list = knowledgeBaseService.listKnowledgeBaseByCondition(request);
            return BaseResult.buildSuc(list);
        } catch (Exception e) {
            log.error("根据条件查询知识库列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 绑定知识库到Agent
     *
     * @param request 绑定请求
     * @return 绑定结果
     */
    @PostMapping("bind")
    public BaseResult<Void> bindKnowledgeBase(@RequestBody KnowledgeBaseBindRequest request) {
        try {
            knowledgeBaseService.bindKnowledgeBaseToAgent(request);
            return BaseResult.buildSuc();
        } catch (Exception e) {
            log.error("绑定知识库失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 从Agent解绑知识库
     *
     * @param request 解绑请求
     * @return 解绑结果
     */
    @PostMapping("unbind")
    public BaseResult<Void> unbindKnowledgeBase(@RequestBody KnowledgeBaseUnbindRequest request) {
        try {
            knowledgeBaseService.unbindKnowledgeBaseFromAgent(request);
            return BaseResult.buildSuc();
        } catch (Exception e) {
            log.error("解绑知识库失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 删除知识库
     *
     * @param params 包含id的参数
     * @return 删除结果
     */
    @PostMapping("delete")
    public BaseResult<Void> deleteKnowledgeBase(@RequestBody Map<String, Long> params) {
        try {
            Long id = params.get("id");
            knowledgeService.deleteKnowledgeBase(id);
            return BaseResult.buildSuc();
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 搜索知识
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping("search")
    public BaseResult<?> searchKnowledge(@RequestBody KnowledgeBaseSearchRequest request) {
        try {
            return BaseResult.buildSuc(knowledgeService.searchKnowledgeWithFilter(request));
        } catch (Exception e) {
            log.error("搜索知识失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
