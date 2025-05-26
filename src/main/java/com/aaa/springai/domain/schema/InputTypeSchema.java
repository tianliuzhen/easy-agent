package com.aaa.springai.domain.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liuzhen.tian
 * @version 1.0 InputTypeSchema.java  2025/5/26 20:37
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InputTypeSchema {
    /**
     * 参数名
     */
    private String name;
    /**
     * 参数描述
     */
    private String desc;
    /**
     * 参数类型
     */
    private String type;
}
