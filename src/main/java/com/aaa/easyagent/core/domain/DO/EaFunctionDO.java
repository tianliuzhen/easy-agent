package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_function
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_function")
public class EaFunctionDO {
    /**
     * 主键
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 工具名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 工具类型
     */
    @Column(name = "type")
    private String type;

    /**
     * 工具描述
     */
    @Column(name = "desc")
    private String desc;

    /**
     * 元数据
     */
    @Column(name = "metadata")
    private String metadata;

    /**
     * 结构化入参模板
     */
    @Column(name = "input_template")
    private String inputTemplate;

    /**
     * 结构化出参模板
     */
    @Column(name = "output_template")
    private String outputTemplate;
}
