package com.aaa.easyagent.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;

class WebClientUtilTest {

    @Test
    void exchange() {
        WebClientUtil.exchange(HttpMethod.GET, "http://localhost:8080/example/getCurrentDate", null, null, String.class);
    }
}
