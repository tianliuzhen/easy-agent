package com.aaa.easyagent.biz.flow.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Flow 节点执行线程池。
 * <p>
 * 全局共享一个有界池：原先每次 execute() 都新建一个固定线程池（串行图也要起 2 个线程），
 * Controller 还另有一个调试专用池，并发会话下线程数不受控。
 * 队列满后使用 CallerRunsPolicy，由调度线程直接执行节点，保证不丢任务。
 */
@Configuration
@RequiredArgsConstructor
public class FlowExecutorConfig {

    public static final String FLOW_NODE_EXECUTOR = "flowNodeExecutor";

    private final FlowOrchestrationProperties properties;

    @Bean(name = FLOW_NODE_EXECUTOR, destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor flowNodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("flow-node-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
