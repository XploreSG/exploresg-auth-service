# Prometheus & Grafana Integration Guide for Microservices

> **Quick Start Guide for Adding Prometheus Metrics to Spring Boot Microservices**  
> Copied from `exploresg-auth-service` - Proven working setup

## Overview

This guide shows you how to add Prometheus metrics and Grafana monitoring to your Spring Boot microservice. This setup is based on the working implementation from `exploresg-auth-service`.

---

## Step 1: Add Required Dependencies

### Add to `pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Actuator - Provides health checks and metrics endpoints -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- ⚠️ CRITICAL: Micrometer Prometheus Registry -->
    <!-- Without this, /actuator/prometheus will return HTTP 500 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### Common Issue ⚠️

**Problem**: `/actuator/prometheus` returns HTTP 500 error  
**Cause**: Missing `micrometer-registry-prometheus` dependency  
**Solution**: Add the dependency above (it's not included in `spring-boot-starter-actuator`)

---

## Step 2: Configure Application Properties

### `src/main/resources/application.properties`

```properties
# ============================================
# Actuator & Monitoring Configuration
# ============================================

# Expose health, info, metrics, and prometheus endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true

# ============================================
# Kubernetes Health Probes
# ============================================

# Enable Kubernetes-specific health probes
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true

# Configure health check components
management.endpoint.health.show-components=always
management.endpoint.health.group.liveness.include=livenessState
management.endpoint.health.group.readiness.include=readinessState,db
```

### Environment Variables (for `.env` or `.env.production`)

```bash
# Monitoring & Observability
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized
```

---

## Step 3: Configure Security (Spring Security)

### Update `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/error",
                    "/api/v1/check/**",              // Your custom health checks
                    "/api/v1/auth/**",               // Auth endpoints
                    "/actuator/health",              // ✅ Overall health
                    "/actuator/health/liveness",     // ✅ K8s liveness probe
                    "/actuator/health/readiness",    // ✅ K8s readiness probe
                    "/actuator/info",                // ✅ App info
                    "/actuator/prometheus",          // ✅ Prometheus metrics
                    "/swagger-ui/**",
                    "/v3-api-docs/**"
                )
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            // ... rest of your security config

        return http.build();
    }
}
```

### ⚠️ Security Best Practices

- ✅ **DO**: Whitelist specific endpoints (`/actuator/health`, `/actuator/prometheus`)
- ❌ **DON'T**: Use wildcard `/actuator/**` (exposes sensitive endpoints like `/actuator/env`)
- ⚠️ **Production**: Consider adding authentication for `/actuator/prometheus` if it contains sensitive metrics

---

## Step 4: Kubernetes Configuration

### Update `kubernetes/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: your-service-name
  namespace: exploresg
  labels:
    app: your-service-name
spec:
  replicas: 3
  selector:
    matchLabels:
      app: your-service-name
  template:
    metadata:
      labels:
        app: your-service-name
      # ✅ Add Prometheus annotations
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
        - name: your-service
          image: your-registry/your-service:latest
          ports:
            - containerPort: 8080
              name: http

          # ============================================
          # Health Probes for Kubernetes
          # ============================================

          # Startup Probe - Protects slow-starting containers
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 30 # 30 * 10s = 5 minutes max startup

          # Liveness Probe - Restarts pod if failing
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          # Readiness Probe - Removes from service if not ready
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3

          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

---

## Step 5: Docker Compose Configuration

### Update `docker-compose.yml`

```yaml
services:
  your-service:
    build: .
    image: your-service:dev
    ports:
      - "8080:8080"
    env_file:
      - .env
    # ✅ Add health check
    healthcheck:
      test:
        ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    depends_on:
      db:
        condition: service_healthy
```

---

## Step 6: Prometheus ServiceMonitor (if using Prometheus Operator)

### Create `kubernetes/servicemonitor.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: your-service-monitor
  namespace: exploresg
  labels:
    app: your-service
spec:
  selector:
    matchLabels:
      app: your-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
      scrapeTimeout: 10s
```

---

## Testing & Verification

### Local Testing

```bash
# 1. Rebuild with new dependencies
./mvnw clean package -DskipTests

# 2. Start the application
./mvnw spring-boot:run

# 3. Test endpoints (in another terminal)
# Health check
curl http://localhost:8080/actuator/health

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Expected Responses

#### `/actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

#### `/actuator/health/liveness`

```json
{ "status": "UP" }
```

#### `/actuator/prometheus`

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Eden Space",} 1.234567E7
...
```

### Docker Testing

```bash
# Build and start
docker-compose up --build -d

# Check health status
docker-compose ps

# Test endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### Kubernetes Testing

```bash
# Apply configuration
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/servicemonitor.yaml

# Check pod status
kubectl get pods -n exploresg -l app=your-service

# View pod events (shows probe results)
kubectl describe pod -n exploresg your-service-xxxxx

# Port forward and test
kubectl port-forward -n exploresg svc/your-service 8080:8080
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets
# Go to Prometheus UI → Status → Targets
# Your service should show as "UP"
```

---

## Troubleshooting

### Issue 1: `/actuator/prometheus` Returns HTTP 500

**Symptoms:**

- Prometheus shows "Error scraping target: server returned HTTP status 500"
- Service logs show errors about missing Prometheus registry

**Cause:**
Missing `micrometer-registry-prometheus` dependency

**Solution:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

Then rebuild:

```bash
./mvnw clean package
```

---

### Issue 2: Endpoints Return 401/403 Unauthorized

**Symptoms:**

- Health checks return 401 or 403
- Prometheus can't scrape metrics

**Cause:**
Endpoints not whitelisted in Spring Security

**Solution:**
Update `SecurityConfig.java` to permit actuator endpoints (see Step 3 above)

---

### Issue 3: Prometheus Not Scraping Service

**Symptoms:**

- Prometheus targets show service as "DOWN"
- No metrics appearing in Prometheus

**Checklist:**

1. **Verify endpoint works:**

   ```bash
   kubectl exec -n exploresg your-service-xxxxx -- curl http://localhost:8080/actuator/prometheus
   ```

2. **Check service selector:**

   ```bash
   kubectl get svc -n exploresg your-service -o yaml
   # Verify selector matches pod labels
   ```

3. **Verify ServiceMonitor:**

   ```bash
   kubectl get servicemonitor -n exploresg
   kubectl describe servicemonitor -n exploresg your-service-monitor
   ```

4. **Check Prometheus can reach service:**
   ```bash
   # From Prometheus pod
   kubectl exec -n monitoring prometheus-xxxxx -- wget -O- http://your-service.exploresg.svc.cluster.local:8080/actuator/prometheus
   ```

---

### Issue 4: Database Health Check Failing

**Symptoms:**

- `/actuator/health` shows `"status": "DOWN"`
- `"db": {"status": "DOWN"}`

**Solution:**

1. **Check database connection:**

   ```bash
   kubectl logs -n exploresg your-service-xxxxx | grep -i database
   ```

2. **Verify database credentials:**

   ```bash
   kubectl get secret -n exploresg your-service-secrets -o yaml
   ```

3. **Test database connectivity:**
   ```bash
   kubectl exec -n exploresg your-service-xxxxx -- env | grep DATASOURCE
   ```

---

## Available Metrics

After setup, your service will expose these metrics categories:

### JVM Metrics

- `jvm_memory_used_bytes` - Memory usage
- `jvm_gc_pause_seconds` - Garbage collection
- `jvm_threads_live_threads` - Thread counts

### HTTP Metrics

- `http_server_requests_seconds_count` - Request counts
- `http_server_requests_seconds_sum` - Response times
- Status code breakdowns (2xx, 4xx, 5xx)

### Database Metrics (HikariCP)

- `hikaricp_connections_active` - Active connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_pending` - Pending connections

### System Metrics

- `system_cpu_usage` - CPU usage
- `process_uptime_seconds` - Service uptime
- `system_load_average_1m` - Load average

### Custom Metrics

You can add custom metrics using `MeterRegistry`:

```java
@Component
public class CustomMetrics {
    private final Counter orderCounter;

    public CustomMetrics(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .register(registry);
    }

    public void incrementOrderCount() {
        orderCounter.increment();
    }
}
```

---

## Grafana Dashboard Setup

### Import Pre-built Dashboards

1. **Spring Boot Dashboard** (ID: 4701)

   - Go to Grafana → Dashboards → Import
   - Enter dashboard ID: `4701`
   - Select your Prometheus datasource
   - Click Import

2. **JVM Dashboard** (ID: 12856)

   - Dashboard ID: `12856`
   - Shows detailed JVM metrics

3. **Custom Dashboard Query Examples**

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Database connection pool
hikaricp_connections_active
```

---

## Checklist for New Services

- [ ] Add `spring-boot-starter-actuator` dependency
- [ ] Add `micrometer-registry-prometheus` dependency
- [ ] Configure `application.properties` with actuator settings
- [ ] Update `SecurityConfig` to whitelist actuator endpoints
- [ ] Add health probes to `kubernetes/deployment.yaml`
- [ ] Add Prometheus annotations to pod metadata
- [ ] Create `ServiceMonitor` (if using Prometheus Operator)
- [ ] Add healthcheck to `docker-compose.yml`
- [ ] Test locally: `curl http://localhost:8080/actuator/prometheus`
- [ ] Deploy to K8s and verify Prometheus scraping
- [ ] Create Grafana dashboards

---

## Quick Copy-Paste Checklist

```bash
# 1. Add dependencies to pom.xml (see Step 1)
# 2. Add properties to application.properties (see Step 2)
# 3. Update SecurityConfig.java (see Step 3)
# 4. Update deployment.yaml (see Step 4)
# 5. Test locally
./mvnw clean package
./mvnw spring-boot:run
curl http://localhost:8080/actuator/prometheus

# 6. Build and push Docker image
docker build -t your-registry/your-service:latest .
docker push your-registry/your-service:latest

# 7. Deploy to Kubernetes
kubectl apply -f kubernetes/deployment.yaml
kubectl rollout status deployment/your-service -n exploresg

# 8. Verify in Prometheus
# Check: Prometheus UI → Status → Targets
# Your service should be UP and scraping successfully
```

---

## Additional Resources

- **Source Service**: `exploresg-auth-service` (proven working setup)
- **Spring Boot Actuator Docs**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer Prometheus**: https://micrometer.io/docs/registry/prometheus
- **Kubernetes Health Probes**: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/

---

## Contact & Support

If you encounter issues:

1. Check this guide's troubleshooting section
2. Review `exploresg-auth-service` implementation as reference
3. Check service logs: `kubectl logs -n exploresg your-service-xxxxx`
4. Verify Prometheus targets: Prometheus UI → Status → Targets

**Questions?** Reference the working implementation in `exploresg-auth-service` repository.
