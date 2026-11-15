package com.aaa.easyagent.biz.flow.engine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多 Agent 编排运行期配置，前缀 easy-agent.flow。
 * <p>
 * 默认值写在代码里，不配置也能启动；需要调整时在 application.properties 覆盖对应 key。
 */
@Data
@Component
@ConfigurationProperties(prefix = "easy-agent.flow")
public class FlowOrchestrationProperties {

    /** 节点执行线程池核心线程数（所有 Flow 运行共用一个池） */
    private int corePoolSize = 4;
    /** 节点执行线程池最大线程数 */
    private int maxPoolSize = 32;
    /** 节点执行线程池队列容量，队列满后由调用线程直接执行，避免丢任务 */
    private int queueCapacity = 256;
    /** 单个 Flow 内并发执行的节点数上限，防止一次 FORK 打满共享线程池 */
    private int maxParallelismPerRun = 8;

    /** 单个节点执行超时，单位：秒。超时判定为节点失败 */
    private long nodeTimeoutSeconds = 300L;
    /** 整个 Flow 执行超时，单位：秒。超时判定为整体取消 */
    private long flowTimeoutSeconds = 1800L;
    /** 取消或失败后等待在飞节点退出的时间，单位：秒。超过则不再等待，按取消收口审计 */
    private long cancelDrainSeconds = 10L;
    /** 调度循环检查取消标志与超时的间隔，单位：毫秒 */
    private long pollIntervalMillis = 200L;
}
