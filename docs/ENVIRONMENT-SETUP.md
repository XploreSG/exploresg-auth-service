# Environment Setup Guide

## Overview

This document explains how to configure the ExploreSG Auth Service for different environments using environment variables.

## Table of Contents

1. [Local Development](#local-development)
2. [Docker Compose](#docker-compose)
3. [Production Deployment](#production-deployment)
4. [Environment Variables Reference](#environment-variables-reference)
5. [Cloud Provider Setup](#cloud-provider-setup)
6. [Security Best Practices](#security-best-practices)

---

## Local Development

### Quick Start

1. **Copy the example environment file:**

   ```bash
   cp .env.example .env
   ```

2. **Update the `.env` file with your local settings** (defaults should work for local development)

3. **Start the PostgreSQL database:**

   ```bash
   docker-compose up db -d
   ```

4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

### Local Configuration

The `.env` file contains all necessary configuration for local development. Default values are pre-configured for a typical local setup.

---

## Docker Compose

### Running with Docker Compose

The `docker-compose.yml` file is configured to read from the `.env` file automatically.

1. **Ensure `.env` file exists:**

   ```bash
   cp .env.example .env
   ```

2. **Start all services:**

   ```bash
   docker-compose up -d
   ```

3. **View logs:**

   ```bash
   docker-compose logs -f backend-auth-dev
   ```

4. **Stop services:**
   ```bash
   docker-compose down
   ```

### Docker Compose Configuration

The `docker-compose.yml` uses environment variables with fallback defaults:

- Database credentials from `.env`
- Application configuration from `.env`
- Network configuration for microservices communication

---

## Production Deployment

### Prerequisites

1. **Secret Management Service:**

   - AWS Secrets Manager
   - Azure Key Vault
   - GCP Secret Manager
   - HashiCorp Vault

2. **Managed Database:**

   - AWS RDS PostgreSQL
   - Azure Database for PostgreSQL
   - GCP Cloud SQL PostgreSQL

3. **Container Registry:**
   - AWS ECR
   - Azure Container Registry
   - GCP Container Registry
   - Docker Hub

### Deployment Steps

#### 1. Generate Secure JWT Secret

```bash
# Generate a secure random key
openssl rand -base64 64
```

Store this in your cloud secret manager, **DO NOT** hardcode in files.

#### 2. Configure Environment Variables

Use `.env.production` as a template. Configure via:

- **AWS ECS/Fargate:** Task Definition environment variables
- **Azure App Service:** Application Settings
- **GCP Cloud Run:** Environment variables in service configuration
- **Kubernetes:** ConfigMaps and Secrets

#### 3. Database Setup

```sql
-- Create production database
CREATE DATABASE exploresg_auth_prod;

-- Create user with minimal privileges
CREATE USER auth_service_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE exploresg_auth_prod TO auth_service_user;
GRANT USAGE ON SCHEMA public TO auth_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO auth_service_user;
```

#### 4. Build Production Image

```bash
# Build the Docker image
docker build -t exploresg-auth-service:latest .

# Tag for your registry
docker tag exploresg-auth-service:latest your-registry/exploresg-auth-service:v1.0.0

# Push to registry
docker push your-registry/exploresg-auth-service:v1.0.0
```

#### 5. Deploy

Refer to your cloud provider's deployment documentation.

---

## Environment Variables Reference

### Required Variables

| Variable                     | Description                | Example                                     |
| ---------------------------- | -------------------------- | ------------------------------------------- |
| `SPRING_DATASOURCE_URL`      | Database JDBC URL          | `jdbc:postgresql://localhost:5432/authdb`   |
| `SPRING_DATASOURCE_USERNAME` | Database username          | `dbuser`                                    |
| `SPRING_DATASOURCE_PASSWORD` | Database password          | `securepassword`                            |
| `JWT_SECRET_KEY`             | Secret key for JWT signing | `generated-secure-key`                      |
| `OAUTH2_JWT_AUDIENCES`       | Google OAuth client ID     | `your-client-id.apps.googleusercontent.com` |

### Optional Variables (with defaults)

| Variable                        | Default     | Description                       |
| ------------------------------- | ----------- | --------------------------------- |
| `SPRING_PROFILES_ACTIVE`        | `dev`       | Active Spring profile             |
| `SERVER_PORT`                   | `8080`      | Application port                  |
| `JWT_EXPIRATION`                | `86400000`  | JWT expiration (24 hours)         |
| `JWT_REFRESH_EXPIRATION`        | `604800000` | Refresh token expiration (7 days) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update`    | Hibernate DDL strategy            |
| `LOGGING_LEVEL_ROOT`            | `INFO`      | Root logging level                |

---

## Cloud Provider Setup

### AWS Deployment

#### Using AWS ECS with Secrets Manager

```json
{
  "containerDefinitions": [
    {
      "name": "exploresg-auth-service",
      "image": "your-registry/exploresg-auth-service:latest",
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        },
        {
          "name": "JWT_SECRET_KEY",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:jwt-secret"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://your-rds-endpoint:5432/authdb"
        }
      ]
    }
  ]
}
```

### Azure Deployment

#### Using Azure App Service

```bash
# Configure App Settings
az webapp config appsettings set --resource-group myResourceGroup \
  --name exploresg-auth-service \
  --settings \
    SPRING_PROFILES_ACTIVE=prod \
    SPRING_DATASOURCE_URL="jdbc:postgresql://your-server.postgres.database.azure.com:5432/authdb"

# Reference Key Vault secrets
az webapp config appsettings set --resource-group myResourceGroup \
  --name exploresg-auth-service \
  --settings \
    JWT_SECRET_KEY="@Microsoft.KeyVault(SecretUri=https://your-vault.vault.azure.net/secrets/jwt-secret/)"
```

### GCP Deployment

#### Using Cloud Run

```bash
# Deploy with environment variables
gcloud run deploy exploresg-auth-service \
  --image gcr.io/your-project/exploresg-auth-service:latest \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod \
  --set-env-vars SPRING_DATASOURCE_URL="jdbc:postgresql://cloudsql-connection-name" \
  --set-secrets SPRING_DATASOURCE_PASSWORD=db-password:latest \
  --set-secrets JWT_SECRET_KEY=jwt-secret:latest
```

### Kubernetes Deployment

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secrets
type: Opaque
data:
  jwt-secret: <base64-encoded-secret>
  db-password: <base64-encoded-password>

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: exploresg-auth-service
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: auth-service
          image: your-registry/exploresg-auth-service:latest
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:postgresql://postgres-service:5432/authdb"
            - name: JWT_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: auth-service-secrets
                  key: jwt-secret
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: auth-service-secrets
                  key: db-password
```

---

## Security Best Practices

### 1. **Never Commit Secrets**

- Add `.env` to `.gitignore`
- Use `.env.example` for documentation
- Keep `.env.production` as template only

### 2. **Use Secret Management Services**

- **AWS:** Secrets Manager or Parameter Store
- **Azure:** Key Vault
- **GCP:** Secret Manager
- **Kubernetes:** Secrets with encryption at rest

### 3. **Rotate Secrets Regularly**

```bash
# Generate new JWT secret
openssl rand -base64 64

# Update in secret manager
aws secretsmanager update-secret --secret-id jwt-secret --secret-string "new-secret"
```

### 4. **Use IAM Roles / Managed Identities**

Avoid hardcoding cloud credentials. Use:

- AWS IAM Roles for ECS/EC2
- Azure Managed Identity
- GCP Service Accounts with Workload Identity

### 5. **Database Security**

- Use SSL/TLS for database connections
- Restrict database access to application VPC/subnet
- Use minimal privilege database users
- Enable database encryption at rest

### 6. **Network Security**

- Use private subnets for applications
- Configure security groups/firewall rules
- Enable VPC/VNet peering for inter-service communication
- Use API Gateway or load balancer with WAF

### 7. **Monitoring & Auditing**

- Enable CloudWatch/Azure Monitor/GCP Logging
- Set up alerts for failed authentication attempts
- Monitor secret access logs
- Implement distributed tracing

---

## Troubleshooting

### Connection Issues

```bash
# Test database connectivity
docker run --rm postgres:15 psql -h host -U user -d database -c "SELECT 1"

# Check environment variables in container
docker exec -it container-name env | grep SPRING
```

### Profile Issues

```bash
# Verify active profile
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name == "Config resource")'
```

### Secret Loading Issues

Check cloud provider logs:

- AWS: CloudWatch Logs
- Azure: Application Insights
- GCP: Cloud Logging

---

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Twelve-Factor App Methodology](https://12factor.net/config)

---

## Support

For issues or questions, please contact the development team or create an issue in the repository.
