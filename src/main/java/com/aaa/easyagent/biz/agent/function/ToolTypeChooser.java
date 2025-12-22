package com.aaa.easyagent.biz.agent.function;

import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;

import java.lang.annotation.*;

/**
 * @author liuzhen.tian
 * @version 1.0 ToolTypeChooser.java  2025/6/1 19:54
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolTypeChooser {
    ToolTypeEnum value() ;
}
