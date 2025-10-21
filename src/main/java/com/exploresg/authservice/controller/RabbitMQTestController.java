package com.exploresg.authservice.controller;

import com.exploresg.authservice.event.UserCreatedEvent;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.service.UserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Test controller for verifying RabbitMQ integration.
 * 
 * ⚠️ SECURITY NOTE:
 * These endpoints are currently PUBLIC (no authentication required) for
 * development/testing.
 * This is configured in SecurityConfig: "/api/v1/test/**" is in permitAll()
 * 
 * ⚠️ BEFORE PRODUCTION:
 * 1. Remove this controller entirely, OR
 * 2. Secure with @PreAuthorize("hasRole('ADMIN')"), OR
 * 3. Use @Profile("!prod") to disable in production
 * 
 * Current configuration allows testing without JWT tokens.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class RabbitMQTestController {

    private final UserEventPublisher userEventPublisher;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Test endpoint to verify RabbitMQ connection.
     * Sends a test message to the user created queue.
     * 
     * Usage: GET http://localhost:8080/api/v1/test/rabbitmq/hello
     */
    @GetMapping("/rabbitmq/hello")
    public ResponseEntity<Map<String, Object>> testRabbitMQConnection() {
        try {
            log.info("Testing RabbitMQ connection...");

            // Create a test event
            UserCreatedEvent testEvent = UserCreatedEvent.builder()
                    .userId(999L)
                    .userUuid(UUID.randomUUID())
                    .email("test@exploresg.com")
                    .name("Test User")
                    .givenName("Test")
                    .familyName("User")
                    .identityProvider("GOOGLE")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .eventType("USER_CREATED_TEST")
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            // Send test message
            rabbitTemplate.convertAndSend(
                    "exploresg.user.events",
                    "user.created",
                    testEvent);

            log.info("✅ Test message sent successfully to RabbitMQ");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test message sent to RabbitMQ",
                    "exchange", "exploresg.user.events",
                    "routingKey", "user.created",
                    "testEmail", "test@exploresg.com",
                    "timestamp", LocalDateTime.now().toString(),
                    "instructions",
                    "Check RabbitMQ UI at http://localhost:15672 (guest/guest) or notification service logs"));

        } catch (Exception e) {
            log.error("❌ Failed to send test message to RabbitMQ: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to send test message",
                    "error", e.getMessage(),
                    "troubleshooting", Map.of(
                            "step1", "Check if RabbitMQ is running: docker ps | grep rabbitmq",
                            "step2", "Verify network: docker network ls | grep exploresg-net",
                            "step3", "Check logs: docker logs dev-exploresg-auth-service")));
        }
    }

    /**
     * Test endpoint using the UserEventPublisher service.
     * 
     * Usage: GET http://localhost:8080/api/v1/test/rabbitmq/publish
     */
    @GetMapping("/rabbitmq/publish")
    public ResponseEntity<Map<String, Object>> testPublisher() {
        try {
            log.info("Testing UserEventPublisher...");

            // Create a mock user for testing
            User testUser = User.builder()
                    .id(888L)
                    .userId(UUID.randomUUID())
                    .email("publisher-test@exploresg.com")
                    .name("Publisher Test")
                    .givenName("Publisher")
                    .familyName("Test")
                    .build();

            // Publish using the service
            userEventPublisher.publishUserCreatedEvent(testUser);

            log.info("✅ Published test event via UserEventPublisher");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test event published via UserEventPublisher",
                    "service", "UserEventPublisher",
                    "testEmail", "publisher-test@exploresg.com",
                    "timestamp", LocalDateTime.now().toString()));

        } catch (Exception e) {
            log.error("❌ Failed to publish test event: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to publish test event",
                    "error", e.getMessage()));
        }
    }

    /**
     * Check RabbitMQ connection status.
     * 
     * Usage: GET http://localhost:8080/api/v1/test/rabbitmq/status
     */
    @GetMapping("/rabbitmq/status")
    public ResponseEntity<Map<String, Object>> checkRabbitMQStatus() {
        try {
            // Try to get connection info
            var connectionFactory = rabbitTemplate.getConnectionFactory();

            return ResponseEntity.ok(Map.of(
                    "status", "connected",
                    "message", "RabbitMQ connection is active",
                    "host", connectionFactory.getHost(),
                    "port", connectionFactory.getPort(),
                    "virtualHost", connectionFactory.getVirtualHost(),
                    "timestamp", LocalDateTime.now().toString()));

        } catch (Exception e) {
            log.error("❌ RabbitMQ connection check failed: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "status", "disconnected",
                    "message", "RabbitMQ connection is not available",
                    "error", e.getMessage()));
        }
    }

    /**
     * Send a custom test message.
     * 
     * Usage: POST http://localhost:8080/api/v1/test/rabbitmq/custom
     * Body: { "email": "custom@test.com", "name": "Custom User" }
     */
    @PostMapping("/rabbitmq/custom")
    public ResponseEntity<Map<String, Object>> sendCustomMessage(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.getOrDefault("email", "default@test.com");
            String name = payload.getOrDefault("name", "Default User");

            UserCreatedEvent customEvent = UserCreatedEvent.builder()
                    .userId(777L)
                    .userUuid(UUID.randomUUID())
                    .email(email)
                    .name(name)
                    .givenName(name.split(" ")[0])
                    .familyName(name.contains(" ") ? name.substring(name.indexOf(" ") + 1) : "")
                    .identityProvider("TEST")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .eventType("USER_CREATED_CUSTOM_TEST")
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    "exploresg.user.events",
                    "user.created",
                    customEvent);

            log.info("✅ Custom test message sent for: {}", email);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Custom test message sent",
                    "email", email,
                    "name", name));

        } catch (Exception e) {
            log.error("❌ Failed to send custom message: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to send custom message",
                    "error", e.getMessage()));
        }
    }
}
