# Observability (Logging, Metrics, Tracing)

This document consolidates the project's logging, metrics, and observability guidance into one reference. It merges content from the previous logging and Prometheus docs.

## Overview

- Structured JSON logging for production, human-friendly console logs for dev.
- Request correlation IDs propagated via `X-Correlation-ID` and stored in MDC.
- User context (userId, userEmail) added to MDC after authentication for audit trails.
- Metrics exposed via Micrometer and Prometheus (`/actuator/prometheus`).
- Health checks via Spring Boot Actuator (`/actuator/health`) with readiness and liveness checks.

---

## Files & Components

- `src/main/resources/logback-spring.xml` — logging configuration for dev/staging/prod
- `src/main/java/.../RequestCorrelationFilter.java` — sets correlation ID
- `src/main/java/.../UserContextLoggingFilter.java` — adds user context to MDC
- `src/main/java/.../RequestLoggingInterceptor.java` — logs HTTP requests and timings
- `src/main/java/.../WebMvcConfig.java` — registers filters/interceptors
- Micrometer Prometheus registry dependency for `/actuator/prometheus`

---

## Logging behavior

### Formats by environment

- Dev: human-readable, colored console logs
- Staging: pretty-printed JSON
- Prod: compact single-line JSON for log aggregation

### Required fields in logs

- timestamp, level, logger, message
- application, environment
- correlationId, requestMethod, requestPath
- userId, userEmail (when authenticated)
- clientIp

### Best practices

- Do not log secrets or sensitive data
- Use structured fields instead of string concatenation
- Use log sampling or async appenders for high-volume endpoints

---

## Metrics & Prometheus

- Micrometer registry added; `/actuator/prometheus` exposes metrics
- Recommended metrics:
  - http.server.requests
  - jvm.memory.used
  - process.cpu.usage
  - custom business counters (e.g., `user.signup.count`)

Prometheus scrape configuration example (Kubernetes):

```yaml
scrape_configs:
  - job_name: "exploresg-auth-service"
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        target_label: __metrics_path__
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod
```

---

## Tracing (recommended)

- Add OpenTelemetry instrumentation for distributed tracing.
- Ensure correlation IDs map to trace IDs where possible.

---

## Cloud Integration

### AWS CloudWatch

- JSON logs to stdout are captured by the EKS log collector (fluentd/FluentBit) and forwarded to CloudWatch Logs.
- Use CloudWatch Insights to query by `correlationId` or `userId`.

### ELK

- Send logs to Logstash/ElasticSearch with `logstash-logback-encoder`.
- Recommended pipeline: logstash -> Elasticsearch -> Kibana dashboards.

---

## Troubleshooting & Tests

- Test correlation ID propagation with a custom header; verify response includes `X-Correlation-ID`.
- Start app with `prod` profile to verify JSON logs.
- Use CloudWatch Insights/Elasticsearch queries to verify logs ingestion.

---

## Archive

The original files were archived in `docs/archive/2025-10-15/` for traceability.

---

**Last Updated:** 2025-10-15
