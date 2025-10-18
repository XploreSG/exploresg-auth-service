# Quick Setup Guide: Prometheus + Grafana for Your Microservice

Hey team! 👋

I've just finished setting up Prometheus metrics and Grafana monitoring for the auth service. Here's what you need to replicate this in your services.

## 🚀 TL;DR

**The #1 Issue We Hit**: `/actuator/prometheus` returned HTTP 500 because we were missing the `micrometer-registry-prometheus` dependency. Don't forget to add it!

## 📦 What You Need to Add

### 1. Dependencies (`pom.xml`)

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- ⚠️ CRITICAL: Don't forget this one! -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Configuration (`application.properties`)

```properties
# Expose endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true

# K8s health probes
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

### 3. Security Config

Whitelist these endpoints in your `SecurityConfig.java`:

```java
.requestMatchers(
    "/actuator/health",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/info",
    "/actuator/prometheus"
)
.permitAll()
```

### 4. Kubernetes Deployment

Add health probes and Prometheus annotations:

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8080"

spec:
  containers:
    - name: your-service
      startupProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        periodSeconds: 10
        failureThreshold: 30

      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        periodSeconds: 10
        failureThreshold: 3

      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        periodSeconds: 5
        failureThreshold: 3
```

## ✅ How to Test

```bash
# After adding dependencies and rebuilding
./mvnw clean package
./mvnw spring-boot:run

# Test the endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus

# Should see metrics, not an error!
```

## 🐛 Common Issues

**Problem**: `/actuator/prometheus` returns HTTP 500  
**Fix**: Add the `micrometer-registry-prometheus` dependency (see above)

**Problem**: Endpoints return 401/403  
**Fix**: Whitelist them in SecurityConfig (see above)

**Problem**: Prometheus not scraping  
**Fix**: Add the annotations to your K8s deployment

## 📚 Full Documentation

For detailed setup, troubleshooting, and best practices:

**👉 See: `docs/PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`**

This has everything:

- Complete configuration examples
- Troubleshooting guide
- Grafana dashboard setup
- Custom metrics examples
- Testing procedures

## 📁 Reference Implementation

You can check out the working implementation in `exploresg-auth-service`:

- `pom.xml` - Dependencies
- `src/main/resources/application.properties` - Configuration
- `src/main/java/com/exploresg/authservice/config/SecurityConfig.java` - Security setup
- `kubernetes/deployment.yaml` - K8s health probes

## 🎯 Checklist

- [ ] Add both dependencies to pom.xml
- [ ] Add properties to application.properties
- [ ] Whitelist endpoints in SecurityConfig
- [ ] Add health probes to deployment.yaml
- [ ] Test locally before deploying
- [ ] Verify Prometheus scraping after deploy

## Questions?

Reference the full guide or check the auth-service implementation. The setup is straightforward once you have the right dependencies!

Happy monitoring! 📊
