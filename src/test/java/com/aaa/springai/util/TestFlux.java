package com.aaa.springai.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * @author liuzhen.tian
 * @version 1.0 TestFlux.java  2025/4/22 22:41
 */
public class TestFlux {

    @Test
    public  void sseEmitter() {
        WebClient client = WebClient.create();

        Flux<String> eventStream = client.get()
                .uri("http://localhost:8080/ollama/ai/sseEmitter")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class);

        eventStream.subscribe(
                data -> {
                    System.out.println("Received: " + data);
                },
                error -> {
                    System.err.println("Error: " + error);
                },
                () -> {
                    System.out.println("Completed");
                }
        );

        // 保持程序运行
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
