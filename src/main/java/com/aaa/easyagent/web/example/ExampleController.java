package com.aaa.easyagent.web.example;

import com.aaa.easyagent.common.util.JacksonUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

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

    @GetMapping(value = "/queryPreciousMetalsPrice")
    public String queryPreciousMetalsPrice(String type) {
        if ("gold".equals(type)) {
            return "黄金目前1050元每克";
        }
        if ("silver".equals(type)) {
            return "白银目前20元每克";
        }
        return type+"：类型输入有误无法查询" ;
    }


    @PostMapping(value = "/testRequestBody")
    public String testRequestBody(@RequestBody Object map) {
        return JacksonUtil.toStr(map);
    }

    @PostMapping(value = "/getMap")
    public Map getMap() {
        Map<Integer, String> integerStringMap = Map.ofEntries(
                entry(1, "a"),
                entry(2, "b"),
                entry(3, "c"),
                entry(26, "z"));
        return integerStringMap;
    }
}
