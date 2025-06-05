package com.aaa.springai.web.example;

import com.aaa.springai.util.JacksonUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * @author liuzhen.tian
 * @version 1.0 ExampleController.java  2025/5/25 21:31
 */
@RestController
@RequestMapping("/example")
public class ExampleController {

    @GetMapping(value = "/getCurrentDate")
    public String getCurrentDate() {
        return new Date().toString();
    }


    @PostMapping(value = "/testRequestBody")
    public String testRequestBody(@RequestBody Object map) {
        return JacksonUtil.toStr(map);
    }
}
