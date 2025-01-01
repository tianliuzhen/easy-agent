package com.aaa.springai.web.docs;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 LocalDocumentService.java  2024/12/28 21:31
 */
@Component
public class LocalDocumentService {

    @Value("classpath:docs/MetalPrice.txt") // This is the text document to load
    private Resource resource;

    @Autowired
    private VectorStore simpleVectorStore;

    @PostConstruct
    public void init() {
        loadText();
    }

    public List<Document> loadText() {
        // 查询文档
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("金属价格", "MetalPrice.txt");
        List<Document> documents = textReader.get();

        // 把文章分为小段
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(400, 175, 5, 1000, true);
        // TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> list = tokenTextSplitter.apply(documents);

        // 存入向量数据库
        simpleVectorStore.add(list);

        return list;
    }

    public List<Document> search(String message) {
        List<Document> documents = simpleVectorStore.similaritySearch(message);
        return documents;
    }

}
