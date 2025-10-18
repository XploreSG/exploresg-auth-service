# Prometheus Integration - Lessons Learned

## What We Did

Integrated Prometheus metrics and Grafana monitoring into `exploresg-auth-service` with proper health checks for Kubernetes.

## Key Learnings

### 1. The Critical Dependency ⚠️

**Problem**: `/actuator/prometheus` endpoint returned HTTP 500  
**Root Cause**: Missing `micrometer-registry-prometheus` dependency  
**Lesson**: `spring-boot-starter-actuator` alone is NOT enough for Prometheus

```xml
<!-- This is required! -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Security Configuration

**Lesson**: Be explicit about which actuator endpoints to expose

❌ **Bad** (security risk):

```java
.requestMatchers("/actuator/**").permitAll()
```

✅ **Good** (secure):

```java
.requestMatchers(
    "/actuator/health",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/info",
    "/actuator/prometheus"
).permitAll()
```

### 3. Health Checks vs Metrics

**Lesson**: Separate concerns - don't use custom health endpoints for production

- **Integration Tests**: Use custom endpoint like `/api/v1/check/health`
- **Production K8s**: Use Actuator endpoints `/actuator/health/liveness` and `/actuator/health/readiness`
- **Monitoring**: Use `/actuator/prometheus` for metrics

### 4. Kubernetes Health Probes

**Lesson**: Use all three probe types for production

```yaml
startupProbe: # Protects slow-starting apps
  failureThreshold: 30 # 5 minutes max

livenessProbe: # Restarts if failing
  periodSeconds: 10

readinessProbe: # Removes from service if not ready
  periodSeconds: 5
```

### 5. Property Names Matter

**Lesson**: Spring Boot is case-sensitive with property names

❌ Wrong:

```properties
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

✅ Correct:

```properties
management.health.livenessstate.enabled=true  # lowercase 'state'
management.health.readinessstate.enabled=true
```

## Best Practices Established

1. ✅ Always include `micrometer-registry-prometheus` when using Actuator
2. ✅ Whitelist specific endpoints, not wildcards
3. ✅ Use Actuator's built-in health probes for K8s
4. ✅ Keep custom health endpoints for integration tests only
5. ✅ Add Prometheus annotations to K8s deployments
6. ✅ Test locally before deploying to K8s
7. ✅ Document the setup for other teams

## Testing Workflow

```bash
# 1. Local test first
./mvnw spring-boot:run
curl http://localhost:8080/actuator/prometheus

# 2. Docker test
docker-compose up --build
curl http://localhost:8080/actuator/prometheus

# 3. K8s deployment
kubectl apply -f kubernetes/deployment.yaml
kubectl port-forward svc/exploresg-auth-service 8080:8080
curl http://localhost:8080/actuator/prometheus

# 4. Verify Prometheus scraping
# Check: Prometheus UI → Status → Targets
```

## Troubleshooting Shortcuts

| Issue                              | Quick Check                                   | Fix                           |
| ---------------------------------- | --------------------------------------------- | ----------------------------- |
| HTTP 500 on `/actuator/prometheus` | `grep micrometer-registry-prometheus pom.xml` | Add the dependency            |
| 401/403 errors                     | Check SecurityConfig                          | Whitelist the endpoint        |
| Prometheus not scraping            | Check pod annotations                         | Add prometheus.io annotations |
| Health check failing               | `curl /actuator/health`                       | Check DB connection           |

## Metrics We're Now Collecting

- JVM memory and GC
- HTTP request rates and latencies
- Database connection pool stats
- System CPU and load
- Custom business metrics (if needed)

## Documentation Created

1. `PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md` - Complete setup guide for other services
2. `TEAM-SETUP-GUIDE.md` - Quick reference for team
3. `PROMETHEUS-FIX.md` - Detailed fix for the HTTP 500 issue
4. `HEALTH-CHECK-GUIDE.md` - Health check best practices

## Next Steps for Other Services

1. Share `TEAM-SETUP-GUIDE.md` with other teams
2. Use `PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md` as implementation guide
3. Reference this service as working example
4. Set up Grafana dashboards using shared queries

## Time Saved for Others

By documenting this setup:

- ⏱️ Avoid the HTTP 500 debugging (2+ hours)
- ⏱️ Skip security configuration trial-and-error (1+ hour)
- ⏱️ Copy-paste working K8s health probe config (30 min)
- ⏱️ Use proven property configurations (30 min)

**Total time saved per service: ~4 hours** 🎉

## Contact

Questions about this setup? Reference:

- This service's implementation
- The documentation files listed above
- Test the endpoints locally to verify

---

**Date**: October 14, 2025  
**Service**: exploresg-auth-service  
**Status**: ✅ Working in Production
