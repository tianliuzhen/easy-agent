package com.aaa.easyagent.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * @author liuzhen.tian
 * @version 1.0 TestFlux.java  2025/4/22 22:41
 */
public class TestFlux {


    @Test
    public void jdkHttpClient() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/ollama/ai/sseEmitter"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    try (var inputStream = response.body()) {
                        var reader = new BufferedReader(
                                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // 保持程序运行
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void springWebClient() {
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
