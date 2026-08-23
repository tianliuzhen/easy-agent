package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@Table(name = "ea_agent_flow_run")
public class EaAgentFlowRunDO {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;
    @Column(name = "run_id")
    private String runId;
    @Column(name = "flow_id")
    private Long flowId;
    @Column(name = "revision_id")
    private Long revisionId;
    @Column(name = "session_id")
    private String sessionId;
    @Column(name = "run_type")
    private String runType;
    @Column(name = "status")
    private String status;
    @Column(name = "input_json")
    private String inputJson;
    @Column(name = "output_json")
    private String outputJson;
    @Column(name = "error_message")
    private String errorMessage;
    @Column(name = "input_tokens")
    private Long inputTokens;
    @Column(name = "output_tokens")
    private Long outputTokens;
    @Column(name = "started_at")
    private Date startedAt;
    @Column(name = "finished_at")
    private Date finishedAt;
}
