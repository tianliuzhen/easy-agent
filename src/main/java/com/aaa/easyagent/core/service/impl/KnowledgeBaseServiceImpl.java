package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.transformer.MyTextReader;
import com.aaa.easyagent.common.transformer.MyTokenTextSplitterV2;
import com.aaa.easyagent.common.util.OcrUtil;
import com.aaa.easyagent.core.domain.DO.EaKnowledgeBaseDO;
import com.aaa.easyagent.core.mapper.EaKnowledgeBaseDAO;
import com.aaa.easyagent.core.service.KnowledgeBaseService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理服务实现类
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseServiceImpl.java  2026/2/1 0:00
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private EaKnowledgeBaseDAO knowledgeBaseDAO;

    @Autowired
    private VectorStore esVectorStore;
    
    @Autowired
    private OcrUtil ocrUtil;

    @Override
    public EaKnowledgeBaseDO uploadDocument(String kbName, String kbDesc, MultipartFile file) {
        try {
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
            }

            // 文档切分
            MyTokenTextSplitterV2 splitter = new MyTokenTextSplitterV2();
            List<Document> chunks = splitter.apply(documents);

            log.info("文档切分完成，共 {} 个片段", chunks.size());

            // 存入向量数据库
            esVectorStore.add(chunks);

            // 收集文档ID
            List<String> docIds = chunks.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            // 保存到数据库
            EaKnowledgeBaseDO kb = new EaKnowledgeBaseDO();
            kb.setKbName(kbName);
            kb.setKbDesc(kbDesc);
            kb.setFileName(fileName);
            kb.setFileType(fileType);
            kb.setFileSize(fileSize);
            kb.setDocCount(chunks.size());
            kb.setDocIds(JSON.toJSONString(docIds));
            kb.setStatus((byte) 1);
            kb.setCreateTime(new Date());
            kb.setUpdateTime(new Date());

            knowledgeBaseDAO.insertSelective(kb);

            log.info("知识库上传成功: {}, 文档数: {}", fileName, chunks.size());
            return kb;

        } catch (Exception e) {
            log.error("上传文档失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("上传文档失败: " + e.getMessage());
        }
    }

    @Override
    public List<EaKnowledgeBaseDO> listKnowledgeBase() {
        try {
            EaKnowledgeBaseDO query = new EaKnowledgeBaseDO();
            query.setStatus((byte) 1);
            return knowledgeBaseDAO.select(query);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            throw new RuntimeException("查询知识库列表失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        try {
            EaKnowledgeBaseDO kb = knowledgeBaseDAO.selectByPrimaryKey(id);
            if (kb == null) {
                throw new RuntimeException("知识库不存在");
            }

            // 删除向量库中的文档
            if (StringUtils.hasText(kb.getDocIds())) {
                List<String> docIds = JSON.parseArray(kb.getDocIds(), String.class);
                log.info("删除知识库，同时删除 {} 个文档片段", docIds.size());
                for (String docId : docIds) {
                    try {
                        esVectorStore.delete(List.of(docId));
                    } catch (Exception e) {
                        log.warn("删除文档片段失败: {}", docId, e);
                    }
                }
            }

            // 逻辑删除数据库记录
            kb.setStatus((byte) 0);
            kb.setUpdateTime(new Date());
            knowledgeBaseDAO.updateByPrimaryKeySelective(kb);

            log.info("知识库删除成功: {}", kb.getFileName());
        } catch (Exception e) {
            log.error("删除知识库失败: {}", id, e);
            throw new RuntimeException("删除知识库失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> searchKnowledge(String query, Integer topK) {
        try {
            if (topK == null || topK <= 0) {
                topK = 5;
            }

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();
            List<Document> documents = esVectorStore.similaritySearch(searchRequest);

            return documents.stream()
                    .map(doc -> {
                        String text = doc.getText();
                        String fileName = (String) doc.getMetadata().get("file_name");
                        String kbName = (String) doc.getMetadata().get("kb_name");
                        return String.format("[%s - %s]\n%s", kbName, fileName, text);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索知识失败: {}", query, e);
            throw new RuntimeException("搜索知识失败: " + e.getMessage());
        }
    }

    /**
     * 加载PDF文档
     */
    private List<Document> loadPdf(MultipartFile file) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
        return pdfReader.get();
    }

    /**
     * 加载文本文档
     */
    private List<Document> loadText(MultipartFile file) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MyTextReader textReader = new MyTextReader(resource);
        return textReader.get();
    }

    /**
     * 加载图片文档（使用 OCR 识别）
     */
    private List<Document> loadImage(MultipartFile file) throws Exception {
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

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
