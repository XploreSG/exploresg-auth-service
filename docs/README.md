# Monitoring & Observability Documentation

This directory contains comprehensive documentation for setting up Prometheus metrics, Grafana monitoring, and health checks in Spring Boot microservices.

## 📚 Documentation Index

### For Your Team (Other Services)

**Start here** → [`TEAM-SETUP-GUIDE.md`](TEAM-SETUP-GUIDE.md)  
Quick copy-paste guide with the essentials. Perfect for sharing in Slack/Teams.

**Full implementation guide** → [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md)  
Complete step-by-step setup with troubleshooting, testing, and Grafana dashboards.

### Troubleshooting & Fixes

**Prometheus HTTP 500 fix** → [`PROMETHEUS-FIX.md`](PROMETHEUS-FIX.md)  
Detailed explanation and fix for the missing `micrometer-registry-prometheus` dependency issue.

**Health check setup** → [`HEALTH-CHECK-GUIDE.md`](HEALTH-CHECK-GUIDE.md)  
Best practices for Kubernetes liveness, readiness, and startup probes.

### Reference

**What we learned** → [`LESSONS-LEARNED.md`](LESSONS-LEARNED.md)  
Key learnings, best practices, and time-saving tips from our implementation.

## 🎯 Quick Links by Role

### For Developers (Adding Monitoring to New Service)

1. Read [`TEAM-SETUP-GUIDE.md`](TEAM-SETUP-GUIDE.md)
2. Follow [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md)
3. Reference this service's code as working example

### For DevOps/Platform Team

1. Review [`HEALTH-CHECK-GUIDE.md`](HEALTH-CHECK-GUIDE.md) for K8s probe configuration
2. Check [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md) for ServiceMonitor setup
3. See [`PROMETHEUS-FIX.md`](PROMETHEUS-FIX.md) for common scraping issues

### For Troubleshooting

1. Start with [`PROMETHEUS-FIX.md`](PROMETHEUS-FIX.md) if metrics endpoint fails
2. Check [`HEALTH-CHECK-GUIDE.md`](HEALTH-CHECK-GUIDE.md) if probes are failing
3. Review [`LESSONS-LEARNED.md`](LESSONS-LEARNED.md) for common pitfalls

## ⚠️ Critical Points

### The #1 Issue (Save Hours of Debugging)

**Problem**: `/actuator/prometheus` returns HTTP 500  
**Solution**: Add this dependency to `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

See [`PROMETHEUS-FIX.md`](PROMETHEUS-FIX.md) for details.

### Security Best Practice

Don't use `/actuator/**` wildcard. Whitelist specific endpoints:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`
- `/actuator/info`

## 🧪 Testing Your Setup

```bash
# 1. Rebuild with dependencies
./mvnw clean package

# 2. Run locally
./mvnw spring-boot:run

# 3. Test endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus

# 4. Deploy and verify
kubectl port-forward svc/your-service 8080:8080
curl http://localhost:8080/actuator/prometheus
```

## 📊 What You Get

After implementing this setup:

- ✅ Prometheus metrics scraping
- ✅ Grafana dashboards
- ✅ Kubernetes health probes (liveness, readiness, startup)
- ✅ JVM, HTTP, and database metrics
- ✅ Production-ready monitoring

## 🔗 Related Files in This Repo

**Configuration:**

- `src/main/resources/application.properties` - Actuator settings
- `src/main/java/com/exploresg/authservice/config/SecurityConfig.java` - Endpoint security
- `kubernetes/deployment.yaml` - Health probes and annotations
- `docker-compose.yml` - Docker health checks
- `pom.xml` - Required dependencies

**Controllers:**

- `src/main/java/com/exploresg/authservice/controller/HelloWorldController.java` - Custom health endpoint for integration tests

## 💬 Sharing with Your Team

### For Chat/Slack/Teams

Share [`TEAM-SETUP-GUIDE.md`](TEAM-SETUP-GUIDE.md) - it's concise and has everything they need.

### For Confluence/Wiki

Copy [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md) - it's comprehensive and well-structured.

### For Email

```
Subject: Prometheus/Grafana Setup Guide for Microservices

Hey team,

I've documented the Prometheus and Grafana setup from auth-service.

Quick setup: [Link to TEAM-SETUP-GUIDE.md]
Full guide: [Link to PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md]

Key point: Don't forget the micrometer-registry-prometheus dependency!

Reference implementation: exploresg-auth-service repo

Let me know if you have questions!
```

### For Copilot/AI Assistant

Share [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md) with your AI coding assistant. It has:

- Complete dependency list
- Configuration examples
- Code snippets
- Troubleshooting guide
- Testing procedures

## 📈 Metrics & Dashboards

### Grafana Dashboards

Import these pre-built dashboards:

- **Spring Boot 2.1 System Monitor** (ID: 4701)
- **JVM (Micrometer)** (ID: 12856)

### Custom Queries

See [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md) for example Prometheus queries.

## 🆘 Getting Help

1. **Check troubleshooting section** in [`PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md`](PROMETHEUS-GRAFANA-INTEGRATION-GUIDE.md)
2. **Review common issues** in [`LESSONS-LEARNED.md`](LESSONS-LEARNED.md)
3. **Compare with working code** in this repository
4. **Test locally first** before deploying to K8s

## ✅ Implementation Checklist

- [ ] Added dependencies to `pom.xml`
- [ ] Configured `application.properties`
- [ ] Updated `SecurityConfig.java`
- [ ] Added health probes to `kubernetes/deployment.yaml`
- [ ] Added Prometheus annotations
- [ ] Created `ServiceMonitor` (if using Prometheus Operator)
- [ ] Tested locally
- [ ] Deployed to K8s
- [ ] Verified Prometheus scraping
- [ ] Set up Grafana dashboards

## 📅 Last Updated

October 14, 2025

## 🤝 Contributing

Found an issue or have improvements? Update the relevant documentation file and share with the team!

---

**Status**: ✅ Production-ready  
**Service**: exploresg-auth-service  
**Prometheus**: Scraping successfully  
**Grafana**: Dashboards available
