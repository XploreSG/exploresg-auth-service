package com.exploresg.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple controller for basic application checks.
 * For health checks, use Spring Boot Actuator endpoints:
 * - /actuator/health (overall health)
 * - /actuator/health/liveness (K8s liveness probe)
 * - /actuator/health/readiness (K8s readiness probe)
 */
@RequestMapping("/api/v1/check")
@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, ExploreSG!";
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}