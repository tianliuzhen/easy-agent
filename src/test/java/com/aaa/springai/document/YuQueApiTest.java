package com.aaa.springai.document;

import com.aaa.springai.document.yuque.YuQueApi;
import com.aaa.springai.document.yuque.model.BookCatalogInfo;
import com.aaa.springai.document.yuque.model.BookInfo;
import com.aaa.springai.document.yuque.model.UserTokenInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author liuzhen.tian
 * @version 1.0 YuqueApiTest.java  2025/1/9 21:19
 */
@SpringBootTest
public class YuQueApiTest {

    private static String xAuthTokenTemp = "81GISWeSQ3HpvoBAaLJQUW56c8wyWS1gePyRHeKQ";


    @Test
    public void testGetUserInfoByToken() {
        // 获取当前 Token 的用户详情
        UserTokenInfo userInfoByToken = YuQueApi.getUserInfoByToken(xAuthTokenTemp);

        // 获取当前Token的知识库列表
        String login = userInfoByToken.data().login();
        BookInfo bookList = YuQueApi.getBookList(xAuthTokenTemp, login);

        String bookSlug = bookList.data().get(0).slug();
        // 查询知识库的目录
        BookCatalogInfo bookCatalogList = YuQueApi.getBookCatalogList(xAuthTokenTemp, login, bookSlug);

        String docSlug = bookCatalogList.data().get(0).slug();
        // 查询文档详情
        YuQueApi.getDocDetail(xAuthTokenTemp, login, bookSlug, docSlug);
    }
}
