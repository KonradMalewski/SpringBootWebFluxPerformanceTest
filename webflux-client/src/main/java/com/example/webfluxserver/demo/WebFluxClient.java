package com.example.webfluxserver.demo;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class WebFluxClient {

    public static void main(String[] args) {
        final int NUM_RUNS = 1000000;
        ConnectionProvider connectionProvider = ConnectionProvider.builder("myConnectionPool")
                .maxConnections(65000)
        .pendingAcquireMaxCount(-1)
        .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofSeconds(30).toMillisPart())
                .doOnConnected(conn -> conn
                        .addHandler(new ReadTimeoutHandler(1, TimeUnit.MINUTES))
                        .addHandler(new WriteTimeoutHandler(30)))
                .keepAlive(false);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        WebClient webClient = WebClient.builder().clientConnector(connector).build();
        //WebClient client = WebClient.create("http://localhost:8080");
        log.info("created webclient ");
        AtomicInteger i = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        CountDownLatch counter = new CountDownLatch(NUM_RUNS);
        StopWatch timer = new StopWatch();


        timer.start();

        List<Disposable> tasks = new ArrayList<>(NUM_RUNS);
        for (int unused = 0; unused<NUM_RUNS; unused++) {
            Disposable subscribe = webClient.get()
                    .uri("http://localhost:8080/test-endpoint")
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(u -> {
                        if (i.get()%1000==0) {
                            log.info("Current value of retrieve actions [{}]. Result: [{}]. Thread: [{}]", i.get(), u, Thread.currentThread().getName());
                        }
                    })
                    .doOnNext(u -> i.incrementAndGet())
                    .timeout(Duration.ofSeconds(35))
                    .retry(5)
                    .doOnSuccess(u -> success.incrementAndGet())
                    .doOnError(throwable -> failure.incrementAndGet())
                    .onErrorResume(throwable -> Mono.empty())
                    .doFinally(s -> counter.countDown())
                    .then()
                    .subscribe();
        }

//            Disposable subscribe = webClient.get()
//                    .uri("http://localhost:8080/test-endpoint")
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .doOnNext(unused -> i.incrementAndGet())
//                    .then()
//                    .doFinally(s -> counter.countDown())
//                    .repeat(NUM_RUNS)
//                    .subscribe();

        log.info("Waiting for result !");
        Try.run(() -> counter.await(2, TimeUnit.MINUTES));
        timer.stop();
        long totalTimeMillis = timer.getTotalTimeMillis();
        log.info("Operation of [{}] requests took [{}] ms. Its [{}] RPS. success [{}] failure [{}]", i.get(), totalTimeMillis, NUM_RUNS / timer.getTotalTimeSeconds(), success.get(), failure.get());
    }

}
