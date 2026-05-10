package com.aaa.easyagent.biz.agent.service.impl;

import com.aaa.easyagent.biz.agent.service.KnowledgeService;
import com.aaa.easyagent.common.config.vectorstore.VectorStoreRegister;
import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.common.transformer.MyTextReader;
import com.aaa.easyagent.common.transformer.MyTokenTextSplitterV2;
import com.aaa.easyagent.common.util.OcrUtil;
import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseSearchRequest;
import com.aaa.easyagent.core.domain.request.KnowledgeBaseUploadRequest;
import com.aaa.easyagent.core.domain.result.KnowledgeSearchResult;
import com.aaa.easyagent.core.domain.result.EaAgentResult;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.service.KnowledgeBaseService;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库业务服务实现类 - 业务逻辑层
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeServiceImpl.java  2026/2/7 22:47
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final AgentManagerService agentManagerService;
    private final OcrUtil ocrUtil;

    @Autowired
    private VectorStoreRegister vectorStoreRegister;

    @Override
    public EaKnowledgeBaseDO uploadDocument(KnowledgeBaseUploadRequest request, MultipartFile file) {
        try {
            String agentId = request.getAgentId();
            String kbName = request.getKbName();
            String kbDesc = request.getKbDesc();
            String catalog = request.getCatalog();

            String fileName = file.getOriginalFilename();
            String fileType = getFileExtension(fileName);

            // 如果没有扩展名，默认为 txt
            if (!StringUtils.hasText(fileType)) {
                fileType = "txt";
                log.info("文件 {} 没有扩展名，默认作为 txt 处理", fileName);
            }

            long fileSize = file.getSize();

            log.info("开始上传文档: {}, 类型: {}, 大小: {}", fileName, fileType, fileSize);

            List<Document> documents;
            if ("pdf".equalsIgnoreCase(fileType)) {
                documents = loadPdf(file);
            } else if ("txt".equalsIgnoreCase(fileType)) {
                documents = loadText(file);
            } else if (ocrUtil.isSupportedImageFormat(fileName)) {
                // 图片格式，使用 OCR 识别
                documents = loadImage(file);
            } else {
                throw new IllegalArgumentException("不支持的文件类型: " + fileType + "，支持 txt、pdf 和图片格式（jpg、png等）");
            }

            // 添加元数据
            for (Document doc : documents) {
                doc.getMetadata().put("kb_name", kbName);
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("file_type", fileType);
                if (StringUtils.hasText(catalog)) {
                    doc.getMetadata().put("catalog", catalog);
                }
            }

            // 文档切分
            MyTokenTextSplitterV2 splitter = new MyTokenTextSplitterV2();
            List<Document> chunks = splitter.apply(documents);

            log.info("文档切分完成，共 {} 个片段", chunks.size());

            // 存入向量数据库
            VectorStore vectorStore = getLoginCurrentUserVectorStore();
            vectorStore.add(chunks);

            // 收集文档ID
            List<String> docIds = chunks.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            // 保存到数据库
            EaKnowledgeBaseDO kb = new EaKnowledgeBaseDO();
            kb.setKbName(kbName);
            kb.setKbDesc(kbDesc);
            kb.setCatalog(catalog);
            kb.setFileName(fileName);
            kb.setFileType(fileType);
            kb.setFileSize(fileSize);
            kb.setDocCount(chunks.size());
            kb.setDocIds(JSON.toJSONString(docIds));
            kb.setAgentId(StringUtils.hasText(agentId) ? Long.valueOf(agentId) : null);
            kb.setStatus((byte) 1);
            kb.setCreateTime(new Date());
            kb.setUpdateTime(new Date());

            knowledgeBaseService.saveKnowledgeBase(kb);

            log.info("知识库上传成功: {}, 文档数: {}", fileName, chunks.size());
            return kb;

        } catch (Exception e) {
            log.error("上传文档失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("上传文档失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        try {
            EaKnowledgeBaseDO kb = knowledgeBaseService.getKnowledgeBaseById(id);
            if (kb == null) {
                throw new RuntimeException("知识库不存在");
            }

            // 删除向量库中的文档
            if (StringUtils.hasText(kb.getDocIds())) {
                List<String> docIds = JSON.parseArray(kb.getDocIds(), String.class);
                log.info("删除知识库，同时删除 {} 个文档片段", docIds.size());
                VectorStore vectorStore = getLoginCurrentUserVectorStore();
                for (String docId : docIds) {
                    try {
                        vectorStore.delete(List.of(docId));
                    } catch (Exception e) {
                        log.warn("删除文档片段失败: {}", docId, e);
                    }
                }
            }

            // 逻辑删除数据库记录
            kb.setStatus((byte) 0);
            kb.setUpdateTime(new Date());
            knowledgeBaseService.updateKnowledgeBase(kb);

            log.info("知识库删除成功: {}", kb.getFileName());
        } catch (Exception e) {
            log.error("删除知识库失败: {}", id, e);
            throw new RuntimeException("删除知识库失败: " + e.getMessage());
        }
    }

    @Override
    public List<KnowledgeSearchResult> searchKnowledge(KnowledgeBaseSearchRequest request) {
        return searchKnowledgeWithFilter(request);
    }

    @Override
    public List<KnowledgeSearchResult> searchKnowledgeWithFilter(KnowledgeBaseSearchRequest request) {
        try {
            String query = request.getQuery();
            Integer topK = request.getTopK();
            String catalog = request.getCatalog();
            Double threshold = request.getThreshold();
            
            if (topK == null || topK <= 0) {
                topK = 5;
            }

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            // 如果指定了catalog,添加过滤条件
            if (StringUtils.hasText(catalog)) {
                builder.filterExpression("catalog == '" + catalog + "'");
            }

            // 如果指定了threshold,设置相似度阈值
            if (threshold != null) {
                builder.similarityThreshold(threshold);
            }

            SearchRequest searchRequest = builder.build();

            VectorStore vectorStore = getLoginCurrentUserVectorStore();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            return documents.stream()
                    .map(doc -> {
                        KnowledgeSearchResult result = new KnowledgeSearchResult();
                        result.setScore(doc.getScore());
                        result.setKbName((String) doc.getMetadata().get("kb_name"));
                        result.setFileName((String) doc.getMetadata().get("file_name"));
                        result.setCatalog((String) doc.getMetadata().get("catalog"));
                        result.setText(doc.getText());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索知识失败: {}", request.getQuery(), e);
            throw new RuntimeException("搜索知识失败: " + e.getMessage());
        }
    }

    /**
     * 根据agentId 生成向量数据库（带缓存）
     *
     * @param agentManagerService
     * @return
     */
    private VectorStore getVectorStore(EaAgentResult agentManagerService) {
        EaAgentResult agent = agentManagerService;
        VectorStore vectorStore = vectorStoreRegister.register(agent.getAgentKey());
        return vectorStore;
    }

    private VectorStore getLoginCurrentUserVectorStore() {
        String agentKey = "ea_user_common_" + UserContextHolder.getUserId();
        VectorStore vectorStore = vectorStoreRegister.register(agentKey);
        return vectorStore;
    }

    @Override
    public List<Document> loadPdf(MultipartFile file) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
        return pdfReader.get();
    }

    @Override
    public List<Document> loadText(MultipartFile file) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MyTextReader textReader = new MyTextReader(resource);
        return textReader.get();
    }

    @Override
    public List<Document> loadImage(MultipartFile file) throws Exception {
        log.info("开始 OCR 识别图片: {}", file.getOriginalFilename());

        // 使用 OCR 识别图片中的文字
        String text = ocrUtil.recognizeText(file);

        if (!StringUtils.hasText(text)) {
            log.warn("图片 {} 未识别到任何文字", file.getOriginalFilename());
            throw new RuntimeException("图片中未识别到任何文字，请确保图片清晰且包含文字内容");
        }

        log.info("OCR 识别成功，文本长度: {}", text.length());

        // 创建文档对象
        Document document = new Document(text);
        document.getMetadata().put("source", file.getOriginalFilename());
        document.getMetadata().put("ocr_processed", "true");

        return Collections.singletonList(document);
    }

    @Override
    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
