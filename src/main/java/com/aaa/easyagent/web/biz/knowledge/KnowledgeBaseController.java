package com.aaa.easyagent.web.biz.knowledge;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.domain.base.BaseResult;
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
 * @version 1.0 KnowledgeBaseController.java  2026/2/1 0:00
 */
@Slf4j
@RestController
@RequestMapping("knowledge/")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文档
     *
     * @param kbName 知识库名称
     * @param kbDesc 知识库描述
     * @param file   文件
     * @return 上传结果
     */
    @PostMapping("upload")
    public BaseResult<EaKnowledgeBaseDO> uploadDocument(
            @RequestParam("kbName") String kbName,
            @RequestParam("kbDesc") String kbDesc,
            @RequestParam("file") MultipartFile file) {
        try {
            EaKnowledgeBaseDO kb = knowledgeBaseService.uploadDocument(kbName, kbDesc, file);
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
     * 删除知识库
     *
     * @param params 包含id的参数
     * @return 删除结果
     */
    @PostMapping("delete")
    public BaseResult<Void> deleteKnowledgeBase(@RequestBody Map<String, Long> params) {
        try {
            Long id = params.get("id");
            knowledgeBaseService.deleteKnowledgeBase(id);
            return BaseResult.buildSuc();
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 搜索知识
     *
     * @param params 包含query和topK的参数
     * @return 搜索结果
     */
    @PostMapping("search")
    public BaseResult<List<String>> searchKnowledge(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            Integer topK = params.get("topK") != null ? (Integer) params.get("topK") : 5;
            List<String> data = knowledgeBaseService.searchKnowledge(query, topK);
            return BaseResult.buildSuc(data);
        } catch (Exception e) {
            log.error("搜索知识失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
