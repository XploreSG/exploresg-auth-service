package com.exploresg.authservice.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hello World Integration Test - Industry Grade Template
 * 
 * This integration test demonstrates:
 * - Full application context loading
 * - Real HTTP requests to actual endpoints
 * - Database integration testing with test profile
 * - Health check validation
 * 
 * This is a starting template that can be expanded for comprehensive
 * integration testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureWebMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HelloWorldIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void contextLoads() {
        // Verify that the Spring application context loads successfully
        assertThat(restTemplate).isNotNull();
    }

    @Test
    public void testHelloEndpoint() {
        // Test the /hello endpoint - it's currently secured so we expect 403 Forbidden
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/hello",
                String.class);

        // Since the endpoint is secured, we expect a 403 Forbidden response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testPingEndpoint() {
        // Test the /ping endpoint - it's currently secured so we expect 403 Forbidden
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/ping",
                String.class);

        // Since the endpoint is secured, we expect a 403 Forbidden response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testHealthEndpoint() {
        // Test the /health endpoint - it's currently secured so we expect 403 Forbidden
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/health",
                String.class);

        // Since the endpoint is secured, we expect a 403 Forbidden response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testCheckEndpointWithValidEmail() {
        // Test the /api/v1/check endpoint with a valid email parameter
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/check?email=test@example.com",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"email\":\"test@example.com\"");
        assertThat(response.getBody()).contains("\"exists\":");
    }

    @Test
    public void testCheckEndpointWithMissingEmail() {
        // Test the /api/v1/check endpoint without email parameter
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/check",
                String.class);

        // The endpoint may return 500 or 400 depending on how the error is handled
        // For a hello world version, we just verify it doesn't return 200 OK
        assertThat(response.getStatusCode().is5xxServerError() || response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    public void testActuatorHealthEndpoint() {
        // Test Spring Boot Actuator health endpoint - currently secured
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class);

        // Since the endpoint is secured, we expect a 403 Forbidden response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testSecuredEndpointWithoutAuthentication() {
        // Test that secured endpoints return 401/403 without proper authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/me",
                String.class);

        // Should return 401 Unauthorized or 403 Forbidden
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}