package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 表名：ea_knowledge_relation
 * 表注释：知识库关系表
*/
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_knowledge_relation")
public class EaKnowledgeRelationDO {
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
    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * agentId
     */
    @Column(name = "agent_id")
    private Long agentId;

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
}