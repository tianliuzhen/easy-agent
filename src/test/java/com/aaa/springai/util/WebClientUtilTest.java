package com.aaa.springai.util;

import org.junit.jupiter.api.Test;

/**
 * @author liuzhen.tian
 * @version 1.0 WebClientUtilTest.java  2025/5/25 21:33
 */
public class WebClientUtilTest {

    @Test
    public void springWebClient() {
        String s = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);
        System.out.println(s);
    }
}
