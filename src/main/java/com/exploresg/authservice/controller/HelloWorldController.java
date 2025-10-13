package com.exploresg.authservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple controller for basic application checks and integration tests.
 * 
 * For production health checks, prefer Spring Boot Actuator endpoints:
 * - /actuator/health (overall health)
 * - /actuator/health/liveness (K8s liveness probe)
 * - /actuator/health/readiness (K8s readiness probe)
 * 
 * The /health endpoint here is kept for integration tests and backward
 * compatibility.
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

    /**
     * Simple health check endpoint for integration tests.
     * For production use, prefer /actuator/health endpoints.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "exploresg-auth-service"));
    }
}