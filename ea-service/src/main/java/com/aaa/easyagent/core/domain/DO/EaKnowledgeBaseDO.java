package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 表名：ea_knowledge_base
 * 表注释：知识库管理表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_knowledge_base")
public class EaKnowledgeBaseDO {
    /**
     * 主键ID
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * agentId
     */
    @Column(name = "agent_id")
    private Long agentId;

    /**
     * 知识库名称
     */
    @Column(name = "kb_name")
    private String kbName;

    /**
     * 知识库描述
     */
    @Column(name = "kb_desc")
    private String kbDesc;

    /**
     * 知识库类型
     */
    @Column(name = "kb_type")
    private String kbType;

    /**
     * 文件名
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * 文件类型（txt/pdf）
     */
    @Column(name = "file_type")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 切分后的文档数量
     */
    @Column(name = "doc_count")
    private Integer docCount;

    /**
     * 状态：1-正常，0-已删除
     */
    @Column(name = "status")
    private Byte status;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * creator
     */
    @Column(name = "creator")
    private String creator;


    /**
     * 分类
     */
    @Column(name = "catalog")
    private String catalog;
    /**
     * 文档分片ID列表，JSON数组格式，如：["doc_id_1","doc_id_2"]
     */
    @Column(name = "doc_ids")
    private String docIds;
}
