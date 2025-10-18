# Prometheus Endpoint Fix

## Problem

Prometheus was returning **HTTP 500** when scraping `/actuator/prometheus`:

```
Error scraping target: server returned HTTP status 500
```

## Root Cause

The `/actuator/prometheus` endpoint requires the **Micrometer Prometheus Registry** dependency, which was missing from the project.

## Solution

Added the following dependency to `pom.xml`:

```xml
<!-- Micrometer Prometheus Registry for /actuator/prometheus endpoint -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Why This Fixes It

1. **Spring Boot Actuator** provides the actuator framework
2. **Micrometer Registry Prometheus** provides the actual Prometheus metrics exporter
3. Without this dependency, the `/actuator/prometheus` endpoint exists but throws an error when accessed

## Steps to Apply the Fix

### 1. Rebuild the Application

```bash
# Clean and rebuild
./mvnw clean package -DskipTests

# Or with tests
./mvnw clean install
```

### 2. Rebuild Docker Image

```bash
# Build new image
docker build -t exploresg-auth-service:latest .

# Or with docker-compose
docker-compose build
```

### 3. Redeploy to Kubernetes

```bash
# Update the image in your cluster
kubectl rollout restart deployment/exploresg-auth-service -n exploresg

# Or reapply the deployment
kubectl apply -f kubernetes/deployment.yaml

# Check rollout status
kubectl rollout status deployment/exploresg-auth-service -n exploresg
```

### 4. Verify the Fix

```bash
# Port forward to test locally
kubectl port-forward -n exploresg svc/exploresg-auth-service 8080:8080

# Test the prometheus endpoint
curl http://localhost:8080/actuator/prometheus
```

You should now see Prometheus metrics output like:

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Eden Space",} 1.234567E7
...
```

## Verify in Prometheus

After redeploying, Prometheus should successfully scrape the endpoint:

1. Go to Prometheus UI: `http://your-prometheus-url`
2. Navigate to **Status → Targets**
3. Find `serviceMonitor/exploresg/exploresg-auth-service/0`
4. Status should show **UP** instead of **DOWN**
5. Last scrape should show **no errors**

## Configuration

The prometheus endpoint is already properly configured in:

### `application.properties`

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

### `SecurityConfig.java`

```java
.requestMatchers(
    "/actuator/health",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/info",
    "/actuator/prometheus"  // ✅ Whitelisted
)
.permitAll()
```

### `kubernetes/deployment.yaml`

```yaml
metadata:
  labels:
    app: exploresg-auth-service
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8080"
```

## Testing

### Local Testing

```bash
# Start the application
./mvnw spring-boot:run

# Test prometheus endpoint
curl http://localhost:8080/actuator/prometheus

# You should see metrics output (not an error)
```

### Docker Testing

```bash
# Start with docker-compose
docker-compose up -d

# Test from host
curl http://localhost:8080/actuator/prometheus
```

### Kubernetes Testing

```bash
# Get pod name
kubectl get pods -n exploresg -l app=exploresg-auth-service

# Execute curl inside the pod
kubectl exec -n exploresg exploresg-auth-service-xxxxx -- curl -s http://localhost:8080/actuator/prometheus | head -20

# Or port-forward and test from your machine
kubectl port-forward -n exploresg svc/exploresg-auth-service 8080:8080
curl http://localhost:8080/actuator/prometheus
```

## Common Metrics Exposed

After the fix, you'll see metrics like:

- **JVM Metrics**: Memory usage, garbage collection, thread counts
- **HTTP Metrics**: Request counts, response times, error rates
- **Database Metrics**: Connection pool stats, query times
- **Application Metrics**: Custom business metrics (if any)

Example metrics:

```
# JVM Memory
jvm_memory_used_bytes
jvm_memory_max_bytes

# HTTP Requests
http_server_requests_seconds_count
http_server_requests_seconds_sum

# Database Connection Pool
hikaricp_connections_active
hikaricp_connections_idle

# System Metrics
system_cpu_usage
process_uptime_seconds
```

## Troubleshooting

### Still Getting 500 Error

1. **Check logs**:

   ```bash
   kubectl logs -n exploresg exploresg-auth-service-xxxxx
   ```

2. **Verify dependency is included**:

   ```bash
   # Check if micrometer-registry-prometheus is in the jar
   unzip -l target/auth-service-0.0.1-SNAPSHOT.jar | grep micrometer-registry-prometheus
   ```

3. **Check actuator configuration**:
   ```bash
   curl http://localhost:8080/actuator
   ```
   Should list `prometheus` as an available endpoint.

### Prometheus Still Shows DOWN

1. **Network issue**: Verify Prometheus can reach the service
2. **Service selector**: Check Kubernetes ServiceMonitor configuration
3. **Firewall**: Ensure port 8080 is accessible from Prometheus pods

### Missing Metrics

1. **Check management properties**: Verify `management.metrics.export.prometheus.enabled=true`
2. **Custom metrics**: Ensure `@Timed` or `MeterRegistry` is properly configured
3. **Dependencies**: Some metrics require additional dependencies (e.g., `micrometer-registry-prometheus`)

## References

- [Micrometer Prometheus Documentation](https://micrometer.io/docs/registry/prometheus)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Prometheus Spring Boot Integration](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#scrape_config)
