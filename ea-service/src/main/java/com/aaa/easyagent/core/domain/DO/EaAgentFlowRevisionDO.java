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

@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_agent_flow_revision")
public class EaAgentFlowRevisionDO {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "revision_no")
    private Integer revisionNo;

    @Column(name = "status")
    private String status;

    @Column(name = "schema_version")
    private String schemaVersion;

    @Column(name = "definition_json")
    private String definitionJson;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "validation_json")
    private String validationJson;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "published_at")
    private Date publishedAt;
}
