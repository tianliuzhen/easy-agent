package com.aaa.easyagent.common.document.yuque.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 BookCataglogInfo.java  2025/1/9 21:48
 */
public record BookCatalogInfo(@JsonProperty("data") List<Data> data) {
    // BookRecord 类表示 JSON 中的根对象
    public record Data(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("slug") String slug,
            @JsonProperty("id") int id,
            @JsonProperty("doc_id") int docId,
            @JsonProperty("level") int level,
            @JsonProperty("depth") int depth,
            @JsonProperty("open_window") boolean isOpenWindow, // 将整数 1/0 映射为布尔值 true/false
            @JsonProperty("visible") boolean isVisible,        // 将整数 1/0 映射为布尔值 true/false
            @JsonProperty("prev_uuid") String prevUuid,
            @JsonProperty("sibling_uuid") String siblingUuid,
            @JsonProperty("child_uuid") String childUuid,
            @JsonProperty("parent_uuid") String parentUuid,
            @JsonProperty("_serializer") String serializer
    ) {
    }
}
