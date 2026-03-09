package com.aaa.easyagent.flux;

import com.aaa.easyagent.biz.agent.context.AgentHolder;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author liuzhen.tian
 * @version 1.0 FluxExample.java  2025/5/10 18:45
 */
public class FluxExample {
    public static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {
        // 创建一个 Flux 数据流
        Flux<String> stringFlux = Flux.just("Hello", "World", "from", "Spring", "WebFlux")
                .delayElements(Duration.ofSeconds(1));

        CountDownLatch latch = new CountDownLatch(1);

        List<String> agentThink = AgentHolder.getAgentThink();
        AgentHolder.writeThink("begin:", "开始执行");

        // 如果先激活线程,并且使用完销毁    AgentHolder.clearThink();
        executorService.execute(() -> {
            List<String> think = AgentHolder.getAgentThink();
            System.err.println(think == agentThink);
            // AgentHolder.clearThink();
        });
        AgentHolder.writeThink("begin:", "开始执行2");

        // 再次使用线程，  AgentHolder.getAgentThink(); 会是空
        executorService.execute(() -> {
            System.out.println("Thread.currentThread().getName() = " + Thread.currentThread().getName());
            List<String> think = AgentHolder.getAgentThink();
            System.err.println(think == agentThink);
            // 订阅并处理数据
            stringFlux.subscribe(
                    value -> {
                        System.out.println("Thread.currentThread().getName()1 = " + Thread.currentThread().getName());
                        // onNext
                        List<String> think2 = AgentHolder.getAgentThink();
                        System.out.println(think2 == agentThink);
                        AgentHolder.writeThink("data:", value);
                        System.err.println("Received: " + value);
                    },
                    error -> {
                        // onError
                        List<String> agentThink3 = AgentHolder.getAgentThink();
                        AgentHolder.writeThink("err:", error.getLocalizedMessage());
                        latch.countDown();
                        System.err.println("Error: " + error);
                    },
                    () -> {
                        System.out.println("Thread.currentThread().getName()2 = " + Thread.currentThread().getName());
                        // onComplete
                        AgentHolder.writeThink("end:", "执行结束");
                        List<String> agentThink4 = AgentHolder.getAgentThink();

                        System.out.println(AgentHolder.getThink());
                        System.out.println("Completed!");
                        latch.countDown();
                    }
            );
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
