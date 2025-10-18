# 🚨 Fix: Actuator Health Endpoint 403 Forbidden

## Problem

The Docker health check and Kubernetes probes are failing because Spring Security is blocking access to `/actuator/health`:

```
o.s.s.w.a.Http403ForbiddenEntryPoint - Pre-authenticated entry point called. Rejecting access
```

## Root Cause

The `SecurityConfig.java` doesn't have `/actuator/health/**` in the `permitAll()` list, causing Spring Security to block anonymous access to the health endpoint.

## Solution

### 1. Update SecurityConfig.java

Add actuator endpoints to the permitAll list:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/v1/check/**",
                "/api/v1/auth/**",
                "/actuator/health/**",      // ✅ ADD THIS
                "/actuator/health",          // ✅ ADD THIS
                "/swagger-ui/**",
                "/v3-api-docs/**")
            .permitAll()
            .anyRequest()
            .authenticated())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### 2. Rebuild and Test

```powershell
# Rebuild the application
mvn clean package -DskipTests

# Rebuild Docker image
docker build -t sreerajrone/exploresg-auth-service:latest .

# Test locally
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/exploresg-auth-service-db \
  -e SPRING_DATASOURCE_USERNAME=exploresguser \
  -e SPRING_DATASOURCE_PASSWORD=exploresgpass \
  -e JWT_SECRET_KEY=your-secret-key \
  -e OAUTH2_JWT_AUDIENCES=your-google-client-id \
  --name auth-test \
  sreerajrone/exploresg-auth-service:latest

# Wait for startup
Start-Sleep -Seconds 30

# Test health endpoint (should return 200 OK)
curl http://localhost:8080/actuator/health

# Clean up
docker stop auth-test
docker rm auth-test
```

## Why This Matters

### Docker Health Check

The Dockerfile has a built-in health check:

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

If this fails, Docker marks the container as **unhealthy**.

### Kubernetes Probes

Kubernetes uses health endpoints for:

1. **Liveness Probe** - Restarts container if failing

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
```

2. **Readiness Probe** - Removes from load balancer if not ready

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

3. **Startup Probe** - Delays other probes during slow startup

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 30 # 5 minutes max
```

## Security Considerations

**Q: Is it safe to expose /actuator/health publicly?**  
**A: Yes**, it's standard practice:

✅ **Safe to expose:**

- `/actuator/health` - Basic UP/DOWN status
- `/actuator/health/liveness` - For Kubernetes liveness
- `/actuator/health/readiness` - For Kubernetes readiness
- `/actuator/info` - Application metadata

❌ **Should be protected:**

- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment variables (secrets!)
- `/actuator/configprops` - Configuration properties
- `/actuator/beans` - Spring beans
- `/actuator/threaddump` - Thread dumps
- `/actuator/heapdump` - Heap dumps (large files!)

### Recommended application.properties

```properties
# Expose only health endpoints publicly
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=when-authorized

# Enable liveness and readiness probes
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true

# For production, hide details from unauthenticated users
management.endpoint.health.show-components=when-authorized
```

## Testing the Fix

### 1. Check Health Status

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 2. Check Liveness

```bash
curl http://localhost:8080/actuator/health/liveness
# Expected: {"status":"UP"}
```

### 3. Check Readiness

```bash
curl http://localhost:8080/actuator/health/readiness
# Expected: {"status":"UP"}
```

### 4. Verify Docker Health Check

```powershell
# Check container health status
docker inspect --format='{{.State.Health.Status}}' auth-test
# Expected: healthy

# View health check logs
docker inspect --format='{{range .State.Health.Log}}{{.Output}}{{end}}' auth-test
```

## Alternative: Management Port

For even better security, run actuator on a separate port:

```properties
# application.properties
management.server.port=8081
management.endpoints.web.exposure.include=health,info,metrics
```

Then in `SecurityConfig.java`, you don't need to expose actuator endpoints because they're on a different port that's not accessible externally.

**Dockerfile health check:**

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1
```

**Kubernetes probes:**

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081 # Management port
```

This way:

- Port 8080 is for API (secured)
- Port 8081 is for management (internal only, not exposed via Ingress)

## Quick Fix for Your Running Container

If you can't rebuild right now, you can disable security temporarily:

```properties
# application-prod.properties (TEMPORARY - NOT RECOMMENDED)
management.security.enabled=false
```

But **DO NOT** use this in production! Fix the SecurityConfig instead.

---

## Status

- [x] ✅ Identified the issue (actuator endpoints not in permitAll)
- [x] ✅ Documented the fix in SecurityConfig.java
- [ ] ⏳ Rebuild application with fix
- [ ] ⏳ Rebuild Docker image
- [ ] ⏳ Test health endpoint access
- [ ] ⏳ Deploy to staging/production

## Next Steps

1. Fix the compilation errors in the codebase
2. Apply the SecurityConfig fix above
3. Rebuild: `mvn clean package -DskipTests`
4. Rebuild Docker image: `docker build -t sreerajrone/exploresg-auth-service:latest .`
5. Test locally before deploying
6. Push to registry: `docker push sreerajrone/exploresg-auth-service:latest`
7. Update Kubernetes deployment

---

**Priority**: 🔴 **CRITICAL** - Blocks health checks and Kubernetes probes
