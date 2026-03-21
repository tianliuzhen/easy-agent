package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 工具绑定请求
 *
 * @author liuzhen.tian
 * @version 1.0 ToolBindRequest.java  2026/3/22
 */
@Data
public class ToolBindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 工具配置ID
     */
    private Long toolConfigId;

    /**
     * 工具名称（冗余存储，方便查询）
     */
    private String toolName;

    /**
     * 创建者
     */
    private String creator;
}