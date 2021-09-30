package com.example.webfluxserver.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    AtomicInteger counter = new AtomicInteger(0);

    @GetMapping("/test-endpoint")
    Mono<String> getResponse() {
        counter.incrementAndGet();
        return Mono.delay(Duration.ofMillis(1000)).then(Mono.just("ThisIsAResponse"));
    }

    @GetMapping("/info")
    Mono<String> getInfo() {
        return Mono.just(String.format("Number of requests:%d", counter.get()));
    }
}
