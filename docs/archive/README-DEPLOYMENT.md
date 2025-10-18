# ExploreSG Auth Service - Deployment Guide

## 🚀 Quick Start

This service is now **cloud-ready** with externalized configuration. All sensitive data is managed through environment variables.

## 📁 New Files Created

### Environment Files

- `.env` - Local development configuration (✅ created, ❌ do not commit)
- `.env.example` - Template for environment variables (✅ commit this)
- `.env.production` - Production template (✅ commit as reference only)

### Configuration Profiles

- `application-dev.properties` - Development profile
- `application-staging.properties` - Staging profile
- `application-prod.properties` - Production profile

### Kubernetes Manifests

- `kubernetes/deployment.yaml` - K8s deployment configuration
- `kubernetes/ingress.yaml` - Ingress configuration

### Documentation

- `docs/ENVIRONMENT-SETUP.md` - Comprehensive environment setup guide

## 🔧 Local Development

### 1. First Time Setup

```bash
# Copy environment template
cp .env.example .env

# Start PostgreSQL database
docker-compose up db -d

# Run the application
./mvnw spring-boot:run
```

### 2. Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend-auth-dev

# Stop services
docker-compose down
```

## ☁️ Cloud Deployment

### Prerequisites

1. **Container Registry Account**

   - Docker Hub, AWS ECR, Azure ACR, or GCP GCR

2. **Cloud Provider Account**

   - AWS, Azure, or GCP

3. **Managed PostgreSQL Database**
   - AWS RDS, Azure Database, or Cloud SQL

### Build & Push Container

```bash
# Build production image
docker build -t exploresg-auth-service:latest .

# Tag for your registry
docker tag exploresg-auth-service:latest your-registry/exploresg-auth-service:v1.0.0

# Push to registry
docker push your-registry/exploresg-auth-service:v1.0.0
```

### Deploy to AWS ECS

```bash
# Create task definition with environment variables
aws ecs register-task-definition --cli-input-json file://aws-task-definition.json

# Create or update service
aws ecs create-service \
  --cluster exploresg-cluster \
  --service-name auth-service \
  --task-definition exploresg-auth-service:1 \
  --desired-count 3
```

### Deploy to Azure App Service

```bash
# Create App Service
az webapp create \
  --resource-group exploresg-rg \
  --plan exploresg-plan \
  --name exploresg-auth-service \
  --deployment-container-image-name your-registry/exploresg-auth-service:v1.0.0

# Configure environment variables
az webapp config appsettings set \
  --resource-group exploresg-rg \
  --name exploresg-auth-service \
  --settings @appsettings.json
```

### Deploy to GCP Cloud Run

```bash
# Deploy with Cloud Run
gcloud run deploy exploresg-auth-service \
  --image gcr.io/your-project/exploresg-auth-service:v1.0.0 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod \
  --set-secrets SPRING_DATASOURCE_PASSWORD=db-password:latest
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f kubernetes/namespace.yaml

# Create secrets (use your values)
kubectl create secret generic auth-service-secrets \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-password \
  --from-literal=JWT_SECRET_KEY=your-jwt-secret \
  -n exploresg

# Deploy application
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/ingress.yaml

# Check status
kubectl get pods -n exploresg
kubectl logs -f deployment/exploresg-auth-service -n exploresg
```

## 🔐 Security Checklist

### Before Production Deployment

- [ ] Generate new JWT secret key: `openssl rand -base64 64`
- [ ] Store secrets in cloud secret manager (not in environment files)
- [ ] Update Google OAuth client ID for production domain
- [ ] Configure production database with SSL/TLS
- [ ] Enable database encryption at rest
- [ ] Set up VPC/VNet for private networking
- [ ] Configure security groups/firewall rules
- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Set up monitoring and alerting
- [ ] Configure log aggregation
- [ ] Enable automated backups
- [ ] Set up disaster recovery plan

## 📊 Environment Variables

### Critical Variables (Must Be Set)

| Variable                     | Description             | Example                              |
| ---------------------------- | ----------------------- | ------------------------------------ |
| `SPRING_DATASOURCE_URL`      | Database connection URL | `jdbc:postgresql://host:5432/db`     |
| `SPRING_DATASOURCE_USERNAME` | Database username       | `dbuser`                             |
| `SPRING_DATASOURCE_PASSWORD` | Database password       | `secure_password`                    |
| `JWT_SECRET_KEY`             | JWT signing key         | Use secret manager                   |
| `OAUTH2_JWT_AUDIENCES`       | Google OAuth client ID  | `123-xyz.apps.googleusercontent.com` |

### Optional Variables (Have Defaults)

See `.env.example` for complete list with defaults.

## 🔍 Health Checks

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check readiness
curl http://localhost:8080/actuator/health/readiness

# Check liveness
curl http://localhost:8080/actuator/health/liveness

# View metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## 📈 Monitoring

The application exposes several actuator endpoints:

- `/actuator/health` - Health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

Configure your monitoring solution to scrape these endpoints.

## 🐛 Troubleshooting

### Database Connection Issues

```bash
# Test database connectivity
docker run --rm postgres:15 psql -h host -U user -d database -c "SELECT 1"
```

### Check Environment Variables

```bash
# In container
docker exec -it container-name env | grep SPRING

# In Kubernetes
kubectl exec -it pod-name -n exploresg -- env | grep SPRING
```

### View Logs

```bash
# Docker Compose
docker-compose logs -f backend-auth-dev

# Kubernetes
kubectl logs -f deployment/exploresg-auth-service -n exploresg

# AWS ECS
aws logs tail /ecs/exploresg-auth-service --follow

# Azure
az webapp log tail --name exploresg-auth-service --resource-group exploresg-rg

# GCP
gcloud logging read "resource.type=cloud_run_revision" --limit 50
```

## 📚 Additional Documentation

- [Environment Setup Guide](docs/ENVIRONMENT-SETUP.md) - Detailed environment configuration
- [Security Best Practices](SECURITY.md) - Security guidelines
- [API Documentation](http://localhost:8080/swagger-ui.html) - API reference

## 🤝 Support

For issues or questions:

1. Check the troubleshooting section
2. Review logs for error messages
3. Consult the environment setup guide
4. Contact the development team

## 📝 Next Steps

1. ✅ Configure your `.env` file for local development
2. ✅ Test locally with Docker Compose
3. ✅ Set up cloud infrastructure (database, networking)
4. ✅ Configure cloud secrets manager
5. ✅ Build and push Docker image
6. ✅ Deploy to your cloud platform
7. ✅ Set up monitoring and alerting
8. ✅ Configure CI/CD pipeline

---

**Last Updated:** October 2025
