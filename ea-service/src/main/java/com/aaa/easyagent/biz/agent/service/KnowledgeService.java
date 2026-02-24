package com.aaa.easyagent.biz.agent.service;

import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库业务服务接口
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeService.java  2026/2/7 22:47
 */
public interface KnowledgeService {

    /**
     * 上传文档到知识库
     *
     * @param agentId 代理ID
     * @param kbName  知识库名称
     * @param kbDesc  知识库描述
     * @param file    文件
     * @return 知识库记录
     */
    EaKnowledgeBaseDO uploadDocument(String agentId, String kbName, String kbDesc, MultipartFile file);

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
    List<String> searchKnowledge(String agentId,String query, Integer topK);

    /**
     * 加载PDF文档
     *
     * @param file 文件
     * @return 文档列表
     * @throws Exception 异常
     */
    List<org.springframework.ai.document.Document> loadPdf(MultipartFile file) throws Exception;

    /**
     * 加载文本文档
     *
     * @param file 文件
     * @return 文档列表
     * @throws Exception 异常
     */
    List<org.springframework.ai.document.Document> loadText(MultipartFile file) throws Exception;

    /**
     * 加载图片文档（使用 OCR 识别）
     *
     * @param file 文件
     * @return 文档列表
     * @throws Exception 异常
     */
    List<org.springframework.ai.document.Document> loadImage(MultipartFile file) throws Exception;

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    String getFileExtension(String fileName);
}
