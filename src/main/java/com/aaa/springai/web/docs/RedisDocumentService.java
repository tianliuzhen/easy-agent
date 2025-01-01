package com.aaa.springai.web.docs;

import com.aaa.springai.transformer.MyTextReader;
import com.aaa.springai.transformer.MyTokenTextSplitterV2;
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
public class RedisDocumentService {

    @Value("classpath:docs/MetalPrice.txt") // This is the text document to load
    private Resource resource;

    @Autowired
    private VectorStore redisVectorStore;

    @PostConstruct
    public void init(){
        loadText();
    }

    public List<Document> loadText() {
        // 查询文档
        MyTextReader textReader = new MyTextReader(resource);
        textReader.getCustomMetadata().put("金属价格", "MetalPrice.txt");
        List<Document> documents = textReader.get();

        // 把文章分为小段
        MyTokenTextSplitterV2 tokenTextSplitter = new MyTokenTextSplitterV2();
        List<Document> list = tokenTextSplitter.apply(documents);

        // todo 重写documentId 否则上传会重复

        // 存入向量数据库
        redisVectorStore.add(list);

        return list;
    }

    public List<Document> search(String message){
        List<Document> documents = redisVectorStore.similaritySearch(message);
        return documents;
    }

}
