package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理服务接口
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseService.java  2026/2/1 0:00
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档到知识库
     *
     * @param kbName 知识库名称
     * @param kbDesc 知识库描述
     * @param file   文件
     * @return 知识库记录
     */
    EaKnowledgeBaseDO uploadDocument(String kbName, String kbDesc, MultipartFile file);

    /**
     * 查询知识库列表
     *
     * @return 知识库列表
     */
    List<EaKnowledgeBaseDO> listKnowledgeBase();

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     */
    void deleteKnowledgeBase(Long id);

    /**
     * 搜索知识
     *
     * @param query 搜索内容
     * @param topK  返回结果数量
     * @return 搜索结果
     */
    List<String> searchKnowledge(String query, Integer topK);
}
