package com.aaa.springai.web.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
