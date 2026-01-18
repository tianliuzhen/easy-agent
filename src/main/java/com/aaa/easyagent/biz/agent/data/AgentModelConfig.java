package com.aaa.easyagent.biz.agent.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM配置模型类
 * 用于封装大型语言模型的基本配置信息，包括API密钥、基础URL、模型版本和补全路径等参数
 *
 * @author liuzhen.tian
 * @version 1.0 AgentModelConfig.java  2026/1/18 17:21
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentModelConfig {
    /**
     * API密钥，用于认证访问LLM服务的身份
     */
    private String apiKey;

    /**
     * LLM服务的基础URL地址
     */
    private String baseUrl;

    /**
     * LLM模型的版本号或标识符
     */
    private String modelVersion;

    /**
     * 补全请求的API路径
     */
    private String completionsPath;
}
