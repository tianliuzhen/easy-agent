package com.aaa.springai.document.yuque;

import com.aaa.springai.document.yuque.model.BookCatalogInfo;
import com.aaa.springai.document.yuque.model.BookInfo;
import com.aaa.springai.document.yuque.model.DocDetailInfo;
import com.aaa.springai.document.yuque.model.UserTokenInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


/**
 * @author liuzhen.tian
 * @version 1.0 YuQueApi.java  2025/1/9 21:01
 */
@Component
public class YuQueApi {
    public static final String X_AUTH_TOKEN = "X-Auth-Token";
    // 语雀文档基础域名
    private static String baseUrl = "https://www.yuque.com";

    // 获取当前 Token 的用户详情
    private static String apiV2User = baseUrl + "/api/v2/user";

    // 获取知识库列表
    private static String getBookList = baseUrl + "/api/v2/users/:login/repos?offset=0&limit=100&type=Book";

    // 获取知识库目录
    private static String getBookCatalog = baseUrl + "/api/v2/repos/:group_login/:book_slug/toc";

    // 查询单个文档详情
    private static String getDocDetail = baseUrl + "/api/v2/repos/:group_login/:book_slug/docs/:id?page_size=100&page=1";

    private static RestClient restClient;

    static {
        restClient = RestClient.builder()
                .build();
    }

    /**
     * 获取当前 Token 的用户详情
     *
     * @param token
     * @return
     */
    public static UserTokenInfo getUserInfoByToken(String token) {
        UserTokenInfo info = restClient.get()
                .uri(apiV2User)
                .header(X_AUTH_TOKEN, token)
                .retrieve()
                .body(UserTokenInfo.class);
        return info;
    }

    /**
     * 获取知识库列表
     *
     * @param token
     * @param login UserTokenInfo.login
     * @return
     */
    public static BookInfo getBookList(String token, String login) {
        BookInfo info = restClient.get()
                .uri(getBookList.replace(":login", login))
                .header(X_AUTH_TOKEN, token)
                .retrieve()
                .body(BookInfo.class);
        return info;
    }

    /**
     * 获取知识库目录
     *
     * @param token
     * @param login    UserTokenInfo.login
     * @param bookSlug BookInfo.slug
     */
    public static BookCatalogInfo getBookCatalogList(String token, String login, String bookSlug) {
        BookCatalogInfo info = restClient.get()
                .uri(getBookCatalog.replace(":group_login", login).replace(":book_slug", bookSlug))
                .header(X_AUTH_TOKEN, token)
                .retrieve()
                .body(BookCatalogInfo.class);
        return info;
    }

    /**
     * 查询文档详情
     *
     * @param token
     * @param login    (Required) 团队 Login
     * @param bookSlug (Required) 知识库路径
     * @param docSlug  (Required) 文档 ID or 路径
     * @return
     */
    public static DocDetailInfo getDocDetail(String token, String login, String bookSlug, String docSlug) {
        DocDetailInfo info = restClient.get()
                .uri(getDocDetail.replace(":group_login", login)
                        .replace(":book_slug", bookSlug)
                        .replace(":id", docSlug)
                )
                .header(X_AUTH_TOKEN, token)
                .retrieve()
                .body(DocDetailInfo.class);
        return info;
    }

}
