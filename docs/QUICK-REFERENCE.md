# 🚀 Quick Reference Card

## Local Development

```bash
# Setup
cp .env.example .env
docker-compose up -d

# Test
curl http://localhost:8080/actuator/health

# Logs
docker-compose logs -f backend-auth-dev

# Stop
docker-compose down
```

## Environment Variables

| Variable                     | Required | Default | Description      |
| ---------------------------- | -------- | ------- | ---------------- |
| `SPRING_DATASOURCE_URL`      | ✅       | -       | Database URL     |
| `SPRING_DATASOURCE_USERNAME` | ✅       | -       | DB username      |
| `SPRING_DATASOURCE_PASSWORD` | ✅       | -       | DB password      |
| `JWT_SECRET_KEY`             | ✅       | -       | JWT secret       |
| `OAUTH2_JWT_AUDIENCES`       | ✅       | -       | Google client ID |
| `SPRING_PROFILES_ACTIVE`     | ❌       | `dev`   | Profile          |
| `SERVER_PORT`                | ❌       | `8080`  | Port             |

## Profiles

| Profile   | Use Case            | Command                          |
| --------- | ------------------- | -------------------------------- |
| `dev`     | Local development   | `SPRING_PROFILES_ACTIVE=dev`     |
| `staging` | Staging environment | `SPRING_PROFILES_ACTIVE=staging` |
| `prod`    | Production          | `SPRING_PROFILES_ACTIVE=prod`    |

## Docker Commands

```bash
# Build image
docker build -t exploresg-auth-service:latest .

# Run container
docker run -d \
  --env-file .env \
  -p 8080:8080 \
  exploresg-auth-service:latest

# View logs
docker logs -f <container-id>

# Stop container
docker stop <container-id>
```

## Kubernetes Quick Deploy

```bash
# Create namespace
kubectl create namespace exploresg

# Create secrets
kubectl create secret generic auth-service-secrets \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-password \
  --from-literal=JWT_SECRET_KEY=your-jwt-secret \
  -n exploresg

# Deploy
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/ingress.yaml

# Check status
kubectl get pods -n exploresg
kubectl logs -f deployment/exploresg-auth-service -n exploresg
```

## Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Generate JWT Secret

```bash
# Generate secure random key
openssl rand -base64 64

# Or using Python
python -c "import secrets; print(secrets.token_urlsafe(64))"

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

## Database Connection

```bash
# Connect to local PostgreSQL
docker exec -it dev-exploresg-auth-db psql -U exploresguser -d exploresg-auth-service-db

# Run SQL query
docker exec -it dev-exploresg-auth-db psql -U exploresguser -d exploresg-auth-service-db -c "SELECT * FROM app_user;"

# Backup database
docker exec -it dev-exploresg-auth-db pg_dump -U exploresguser exploresg-auth-service-db > backup.sql

# Restore database
docker exec -i dev-exploresg-auth-db psql -U exploresguser exploresg-auth-service-db < backup.sql
```

## AWS Deployment

```bash
# Build and tag
docker build -t exploresg-auth-service:latest .
docker tag exploresg-auth-service:latest <account-id>.dkr.ecr.<region>.amazonaws.com/exploresg-auth-service:latest

# Push to ECR
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/exploresg-auth-service:latest

# Update ECS service
aws ecs update-service --cluster <cluster-name> --service <service-name> --force-new-deployment
```

## Azure Deployment

```bash
# Build and tag
docker build -t exploresg-auth-service:latest .
docker tag exploresg-auth-service:latest <registry-name>.azurecr.io/exploresg-auth-service:latest

# Push to ACR
az acr login --name <registry-name>
docker push <registry-name>.azurecr.io/exploresg-auth-service:latest

# Deploy to App Service
az webapp create --resource-group <rg-name> --plan <plan-name> --name <app-name> \
  --deployment-container-image-name <registry-name>.azurecr.io/exploresg-auth-service:latest
```

## GCP Deployment

```bash
# Build and tag
docker build -t exploresg-auth-service:latest .
docker tag exploresg-auth-service:latest gcr.io/<project-id>/exploresg-auth-service:latest

# Push to GCR
gcloud auth configure-docker
docker push gcr.io/<project-id>/exploresg-auth-service:latest

# Deploy to Cloud Run
gcloud run deploy exploresg-auth-service \
  --image gcr.io/<project-id>/exploresg-auth-service:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## Testing

```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw verify -P integration-tests

# Generate coverage report
./mvnw clean test jacoco:report
open target/site/jacoco/index.html

# Run specific test
./mvnw test -Dtest=HelloWorldIntegrationTest
```

## Troubleshooting

```bash
# Check environment variables
docker exec -it <container> env | grep SPRING

# Check application logs
docker logs -f <container>

# Check database connectivity
docker exec -it dev-exploresg-auth-db pg_isready -U exploresguser

# Test JWT secret
echo -n "test" | openssl dgst -sha256 -hmac "your-jwt-secret"

# Check port availability
netstat -an | grep 8080  # Windows
lsof -i :8080            # Mac/Linux
```

## Security Checklist

- [ ] Generate new JWT secret for production
- [ ] Store secrets in cloud secret manager
- [ ] Update OAuth2 client ID for production
- [ ] Enable SSL/TLS
- [ ] Configure CORS for production domain
- [ ] Set up VPC/VNet
- [ ] Enable database encryption
- [ ] Configure security groups
- [ ] Enable monitoring and logging
- [ ] Set up automated backups

## Useful Links

| Resource                 | URL                                                                   |
| ------------------------ | --------------------------------------------------------------------- |
| **Full Documentation**   | [README.md](../README.md)                                             |
| **Deployment Guide**     | [README-DEPLOYMENT.md](../README-DEPLOYMENT.md)                       |
| **Environment Setup**    | [ENVIRONMENT-SETUP.md](ENVIRONMENT-SETUP.md)                          |
| **Cloud Ready Summary**  | [CLOUD-READY-SUMMARY.md](CLOUD-READY-SUMMARY.md)                      |
| **GitHub Actions Setup** | [.github/GITHUB-ACTIONS-SETUP.md](../.github/GITHUB-ACTIONS-SETUP.md) |
| **Local API Docs**       | http://localhost:8080/swagger-ui.html                                 |
| **Actuator**             | http://localhost:8080/actuator                                        |

---

**Keep this card handy for quick reference! 📌**
