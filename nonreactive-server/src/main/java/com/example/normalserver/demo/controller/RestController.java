package com.example.normalserver.demo.controller;

import ch.qos.logback.core.util.Duration;
import io.vavr.control.Try;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.TimeUnit;


@org.springframework.web.bind.annotation.RestController
public class RestController {

    @GetMapping("/test-endpoint")
    ResponseEntity<String> getResponse() {
        Try.run( () -> TimeUnit.MILLISECONDS.sleep(10));
        return ResponseEntity.ok("ThisIsAResponse");
    }
}
