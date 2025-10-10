# GitHub Actions Secrets and Variables Setup Guide

This document outlines all the secrets and variables you need to configure in your GitHub repository for the CI/CD pipeline.

## Repository Settings Location

Navigate to: **Repository → Settings → Secrets and variables → Actions**

---

## 🔐 Required Secrets

### Common Secrets (Required for All Providers)

| Secret Name    | Description                      | How to Get It   |
| -------------- | -------------------------------- | --------------- |
| `GITHUB_TOKEN` | Automatically provided by GitHub | No setup needed |

### AWS Secrets (If deploying to AWS)

| Secret Name             | Description        | How to Get It                        |
| ----------------------- | ------------------ | ------------------------------------ |
| `AWS_ACCESS_KEY_ID`     | AWS IAM access key | Create IAM user with ECS permissions |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | From IAM user creation               |

**AWS IAM Policy Example:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
```

### Azure Secrets (If deploying to Azure)

| Secret Name         | Description                   | How to Get It         |
| ------------------- | ----------------------------- | --------------------- |
| `AZURE_CREDENTIALS` | Service Principal credentials | See Azure setup below |

**Get Azure Credentials:**

```bash
az ad sp create-for-rbac \
  --name "exploresg-auth-github-actions" \
  --role contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/{resource-group} \
  --sdk-auth
```

This outputs JSON - use the entire output as the secret value.

### GCP Secrets (If deploying to GCP)

| Secret Name  | Description              | How to Get It       |
| ------------ | ------------------------ | ------------------- |
| `GCP_SA_KEY` | Service Account JSON key | See GCP setup below |

**Get GCP Service Account Key:**

```bash
# Create service account
gcloud iam service-accounts create github-actions \
  --display-name "GitHub Actions"

# Grant permissions
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:github-actions@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.admin"

# Create key
gcloud iam service-accounts keys create key.json \
  --iam-account github-actions@PROJECT_ID.iam.gserviceaccount.com

# Copy the content of key.json to GCP_SA_KEY secret
```

---

## 📋 Required Variables

### Common Variables

| Variable Name    | Description         | Example Value            |
| ---------------- | ------------------- | ------------------------ |
| `CLOUD_PROVIDER` | Your cloud provider | `aws`, `azure`, or `gcp` |

### AWS Variables

| Variable Name      | Description                   | Example Value               |
| ------------------ | ----------------------------- | --------------------------- |
| `AWS_REGION`       | AWS region                    | `us-east-1`                 |
| `ECS_CLUSTER`      | ECS cluster name (staging)    | `exploresg-staging-cluster` |
| `ECS_SERVICE`      | ECS service name (staging)    | `auth-service-staging`      |
| `ECS_CLUSTER_PROD` | ECS cluster name (production) | `exploresg-prod-cluster`    |
| `ECS_SERVICE_PROD` | ECS service name (production) | `auth-service-prod`         |

### Azure Variables

| Variable Name            | Description                   | Example Value            |
| ------------------------ | ----------------------------- | ------------------------ |
| `AZURE_WEBAPP_NAME`      | App Service name (staging)    | `exploresg-auth-staging` |
| `AZURE_WEBAPP_NAME_PROD` | App Service name (production) | `exploresg-auth-prod`    |

### GCP Variables

| Variable Name            | Description                    | Example Value          |
| ------------------------ | ------------------------------ | ---------------------- |
| `GCP_REGION`             | GCP region                     | `us-central1`          |
| `CLOUD_RUN_SERVICE`      | Cloud Run service (staging)    | `auth-service-staging` |
| `CLOUD_RUN_SERVICE_PROD` | Cloud Run service (production) | `auth-service-prod`    |

---

## 🔧 Environment-Specific Secrets

### Staging Environment Secrets

Navigate to: **Repository → Settings → Environments → staging**

Set these secrets in the staging environment:

| Secret Name            | Description                 |
| ---------------------- | --------------------------- |
| `DB_PASSWORD`          | Staging database password   |
| `JWT_SECRET_KEY`       | Staging JWT secret          |
| `OAUTH2_CLIENT_SECRET` | Staging Google OAuth secret |

### Production Environment Secrets

Navigate to: **Repository → Settings → Environments → production**

Set these secrets in the production environment:

| Secret Name            | Description                    |
| ---------------------- | ------------------------------ |
| `DB_PASSWORD`          | Production database password   |
| `JWT_SECRET_KEY`       | Production JWT secret          |
| `OAUTH2_CLIENT_SECRET` | Production Google OAuth secret |

---

## 📝 Setup Checklist

### Step 1: Repository Secrets

- [ ] Add cloud provider secrets (AWS/Azure/GCP)
- [ ] Verify `GITHUB_TOKEN` is available (automatic)

### Step 2: Repository Variables

- [ ] Set `CLOUD_PROVIDER` variable
- [ ] Set cloud-specific variables for your provider
- [ ] Verify all resource names match your infrastructure

### Step 3: Environment Setup

- [ ] Create `staging` environment in GitHub
- [ ] Create `production` environment in GitHub
- [ ] Add protection rules for production (optional but recommended)

### Step 4: Environment Secrets

- [ ] Add staging-specific secrets
- [ ] Add production-specific secrets
- [ ] Generate secure values for JWT secrets

### Step 5: Test the Pipeline

- [ ] Push to `develop` branch to test staging deployment
- [ ] Push to `main` branch to test production deployment
- [ ] Monitor GitHub Actions logs for any errors

---

## 🛡️ Security Best Practices

1. **Rotate Secrets Regularly**

   - Change JWT secrets every 90 days
   - Rotate cloud credentials every 6 months

2. **Use Least Privilege**

   - Grant only necessary permissions to service accounts
   - Review IAM policies regularly

3. **Enable Branch Protection**

   - Require pull request reviews
   - Require status checks to pass
   - Prevent force pushes to main

4. **Environment Protection Rules**

   - Require approval for production deployments
   - Restrict deployments to specific branches
   - Set deployment timeout limits

5. **Audit and Monitor**
   - Review GitHub Actions logs regularly
   - Set up alerts for failed deployments
   - Monitor cloud provider access logs

---

## 🔍 Troubleshooting

### Common Issues

**Issue: Deployment fails with "credentials not found"**

- Solution: Verify secrets are set in the correct location (repository or environment)
- Check secret names match exactly (case-sensitive)

**Issue: "Resource not found" errors**

- Solution: Verify variable names match your actual cloud resources
- Check cloud resource names are correct

**Issue: Insufficient permissions**

- Solution: Review IAM policies/roles for service accounts
- Ensure service principal has correct permissions

### Testing Secrets

You can test if secrets are accessible by adding a debug step:

```yaml
- name: Debug Secrets
  run: |
    echo "AWS Region: ${{ vars.AWS_REGION }}"
    echo "Cloud Provider: ${{ vars.CLOUD_PROVIDER }}"
    # Never echo actual secret values!
```

---

## 📞 Support

For issues with GitHub Actions setup:

1. Check the Actions logs in your repository
2. Review this guide for missing configuration
3. Consult your cloud provider's documentation
4. Contact the DevOps team

---

**Last Updated:** October 2025
