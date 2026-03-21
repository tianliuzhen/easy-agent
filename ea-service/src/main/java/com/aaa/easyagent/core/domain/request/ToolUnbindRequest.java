package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 工具解绑请求
 *
 * @author liuzhen.tian
 * @version 1.0 ToolUnbindRequest.java  2026/3/22
 */
@Data
public class ToolUnbindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 工具配置ID
     */
    private Long toolConfigId;
}