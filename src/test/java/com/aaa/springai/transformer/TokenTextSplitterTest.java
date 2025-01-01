package com.aaa.springai.transformer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 TokenTextSplitterTest.java  2025/1/1 18:26
 */
public class TokenTextSplitterTest {

    @Test
    public  void tokenTextSplitter() {
        MyTextReader textReader = new MyTextReader(new ClassPathResource("docs/MetalPrice.txt"));
        textReader.getCustomMetadata().put("金属价格", "MetalPrice.txt");
        List<Document> documents = textReader.get();

        // 把文章分为小段
        MyTokenTextSplitter tokenTextSplitter = new MyTokenTextSplitter(
                400,
                175,
                5,
                1000,
                true,
                100);
        List<Document> list = tokenTextSplitter.apply(documents);
    }

    @Test
    public  void tokenTextSplitterV2() {
        MyTextReader textReader = new MyTextReader(new ClassPathResource("docs/MetalPrice.txt"));
        textReader.getCustomMetadata().put("金属价格", "MetalPrice.txt");
        List<Document> documents = textReader.get();

        // 把文章分为小段
        MyTokenTextSplitterV2 tokenTextSplitter = new MyTokenTextSplitterV2(
                400,
                175,
                5,
                1000,
                true,
                100);
        List<Document> list = tokenTextSplitter.apply(documents);
    }
}
