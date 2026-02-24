package com.aaa.easyagent.common.document.yuque;

import com.aaa.easyagent.common.document.yuque.model.DocDetailInfo;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 YuQueDocumentReader.java  2025/1/9 20:46
 */

public class YuQueDocumentReader implements DocumentReader {
    private String token;
    private String login;
    private String bookSlug;
    private String docSlug;

    public YuQueDocumentReader(String token, String login, String bookSlug, String docSlug) {
        this.token = token;
        this.login = login;
        this.bookSlug = bookSlug;
        this.docSlug = docSlug;
    }

    private YuQueDocumentReader() {
    }

    @Override
    public List<Document> get() {
        DocDetailInfo docDetail = YuQueApi.getDocDetail(token, login, bookSlug, docSlug);

        if (docDetail == null) {
            return null;
        }
        DocDetailInfo.Data docDetailInfo = docDetail.data();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", docDetailInfo.type());
        String urlPath = login + "/" + bookSlug + "/" + docDetailInfo.slug() + "/" + docDetailInfo.id();
        metadata.put("urlPath", urlPath);
        Document document = new Document(urlPath,
                docDetailInfo.body(),
                metadata
        );

        List<Document> list = new ArrayList<>();
        list.add(document);
        return list;
    }
}
