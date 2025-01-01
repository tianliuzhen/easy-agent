package com.aaa.springai.transformer;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 TokenTextSplitterTest.java  2025/1/1 18:26
 */
public class TokenTextSplitterTest {

    @Test
    public void tokenTextSplitter() {
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
    public void tokenTextSplitterV2() {
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

    @Test
    public void EncodedTokens() {
        String text = "你好,day day up";
        int length = text.length();
        List<Integer> tokens = getEncodedTokens(text);
        //  需要注意的是这里的 tokens != text.length()
        String text2 = decodeTokens(tokens);
        List<Integer> tokens2 = tokens.subList(2, 4);
        String token3 = decodeTokens(tokens2);
        System.out.println();
    }

    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");

        Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        return encoding.encode(text).boxed();
    }


    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");
        var tokensIntArray = new IntArrayList(tokens.size());
        tokens.forEach(tokensIntArray::add);

        Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        return encoding.decode(tokensIntArray);
    }
}
