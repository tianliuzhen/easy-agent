package com.aaa.springai.document.yuque.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 BookInfo.java  2025/1/9 21:35
 */
public record BookInfo(@JsonProperty("data") List<BookInfo.Data> data) {
    // 嵌套的record类，用于表示data数组中的每个元素
    public record Data(
            long id,
            String type,
            String slug,
            String name,
            long userId,
            String description,
            long creatorId,
            @JsonProperty("public") boolean isPublic, // 同样，由于'public'是Java关键字，使用isPublic
            int itemsCount,
            int likesCount,
            int watchesCount,
            @JsonProperty("content_updated_at") ZonedDateTime contentUpdatedAt,
            @JsonProperty("created_at") ZonedDateTime createdAt,
            @JsonProperty("updated_at") ZonedDateTime updatedAt,
            UserTokenInfo.User user, // 嵌套的User record
            String namespace,
            @JsonProperty("_serializer") String serializer
    ) {
    }


}
