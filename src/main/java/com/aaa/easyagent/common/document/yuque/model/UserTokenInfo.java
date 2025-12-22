package com.aaa.easyagent.common.document.yuque.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

/**
 * @author liuzhen.tian
 * @version 1.0 UserTokenInfo.java  2025/1/9 21:02
 */
public record UserTokenInfo(@JsonProperty("data") Data data) {

    // 嵌套的record类，用于表示data字段的内容
    public record Data(
            long id,
            String type,
            String login,
            String name,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("books_count") int booksCount,
            @JsonProperty("public_books_count") int publicBooksCount,
            @JsonProperty("followers_count") int followersCount,
            @JsonProperty("following_count") int followingCount,
            @JsonProperty("public") boolean isPublic, // 同样，由于'public'是Java关键字，使用isPublic
            String description,
            @JsonProperty("created_at") ZonedDateTime createdAt,
            @JsonProperty("updated_at") ZonedDateTime updatedAt,
            String workId,
            // 通常不会将'_serializer'这样的内部使用字段包含在record中，
            // 但如果确实需要，可以添加这个字段，并使用@JsonIgnore来忽略它，
            // 或者使用@JsonProperty并将其映射到一个不会与Java关键字冲突的字段名上。
            // 这里我们省略它。
            @JsonProperty("_serializer") String serializer
    ) {
    }

    // 嵌套的record类，用于表示user字段的内容
    public record User(
            long id,
            String type,
            String login,
            String name,
            @JsonProperty("avatar_url") String avatarUrl,
            int followersCount,
            int followingCount,
            @JsonProperty("public") boolean isPublic, // 同样，由于'public'是Java关键字，使用isPublic
            String description,
            @JsonProperty("created_at") ZonedDateTime createdAt,
            @JsonProperty("updated_at") ZonedDateTime updatedAt,
            String workId,
            // 同样省略了'_serializer'字段
            @JsonProperty("_serializer") String serializer
    ) {
    }
}

