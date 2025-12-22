package com.aaa.easyagent.common.document.yuque.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 DocDetailResp.java  2025/1/9 22:13
 */
public record DocDetailInfo(@JsonProperty("data") Data data) {
    public record Data(int id,
                       @JsonProperty("type") String type,
                       @JsonProperty("slug") String slug,
                       @JsonProperty("title") String title,
                       @JsonProperty("description") String description,
                       @JsonProperty("cover") String cover,
                       @JsonProperty("user_id") int userId,
                       @JsonProperty("book_id") int bookId,
                       @JsonProperty("last_editor_id") int lastEditorId,
                       @JsonProperty("format") String format,
                       @JsonProperty("body_draft") String bodyDraft,
                       @JsonProperty("body") String body,
                       @JsonProperty("body_html") String bodyHtml,
                       @JsonProperty("body_lake") String bodyLake,
                       @JsonProperty("public") boolean isPublic, // 将整数 0/1 映射为布尔值 false/true
                       @JsonProperty("status") int status,
                       @JsonProperty("likes_count") int likesCount,
                       @JsonProperty("read_count") int readCount,
                       @JsonProperty("hits") int hits,
                       @JsonProperty("comments_count") int commentsCount,
                       @JsonProperty("word_count") int wordCount,
                       @JsonProperty("created_at") ZonedDateTime createdAt,
                       @JsonProperty("updated_at") ZonedDateTime updatedAt,
                       @JsonProperty("content_updated_at") ZonedDateTime contentUpdatedAt,
                       @JsonProperty("published_at") ZonedDateTime publishedAt,
                       @JsonProperty("first_published_at") ZonedDateTime firstPublishedAt,
                       @JsonProperty("book") BookInfo.Data book,
                       @JsonProperty("user") UserTokenInfo.User user,
                       @JsonProperty("tags") List<String> tags, // 假设标签是字符串列表，如果不是请根据实际情况调整
                       @JsonProperty("latest_version_id") Long latestVersionId,
                       @JsonProperty("creator") UserTokenInfo.User creator,
                       @JsonProperty("_serializer") String serializer) {

    }
}
