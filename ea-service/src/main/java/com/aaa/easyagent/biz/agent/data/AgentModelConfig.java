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


    /**
     * Top P 的取值范围通常是一个浮点数，在 0 到 1 之间。
     * 代表的是概率累计和的阈值，所以它的最小值不能低于 0，最大值不能超过 1
     * 通常的建议范围：0.8 到 0.95
     */
    private Double topP;
    /**
     * Top K 的取值范围通常是一个整数，表示候选词的数量。
     * 它的最小值不能低于 1，最大值没有固定的限制，但通常不会超过 100
     * 通常的建议范围：1 到 50
     */
    private int topK;
}
