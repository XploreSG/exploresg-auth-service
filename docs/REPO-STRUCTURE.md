# 📦 Repository Structure Update - October 19, 2025

## 🎯 Architecture Decision

This repository follows a **separation of concerns** pattern:

- **Application Repository** (this repo): Contains application code, Dockerfile, and application configuration
- **Infrastructure Repository** (separate repo): Contains Kubernetes manifests, Helm charts, and deployment configurations

## ✅ Changes Made

### Removed

- ❌ `kubernetes/` folder - Moved to central infrastructure repository

### Updated

1. ✅ `README.md` - Updated deployment section to reference central infra repo
2. ✅ `docs/MEMORY-OPTIMIZATION-SUMMARY.md` - Updated deployment steps for centralized architecture
3. ✅ `docs/MEMORY-OPTIMIZATION-QUICK-REF.md` - Clarified infrastructure changes location

### Added

4. ✅ `docs/KUBERNETES-CONFIG-CHANGES.md` - **NEW** - Comprehensive guide for infrastructure repository changes
5. ✅ `docs/REPO-STRUCTURE.md` - **NEW** - This document

## 📂 Current Repository Structure

```
exploresg-auth-service/
├── .github/workflows/          # CI/CD pipelines (build, test, docker)
├── docs/                       # Documentation
│   ├── ENVIRONMENT-SETUP.md   # Environment configuration guide
│   ├── KUBERNETES-CONFIG-CHANGES.md  # 🆕 For infra repo
│   ├── MEMORY-OPTIMIZATION.md
│   ├── MEMORY-OPTIMIZATION-SUMMARY.md
│   ├── MEMORY-OPTIMIZATION-QUICK-REF.md
│   ├── REPO-STRUCTURE.md      # 🆕 This file
│   └── ...
├── scripts/                    # Utility scripts
│   ├── validate-memory-optimization.ps1
│   ├── validate-memory-optimization.sh
│   └── ...
├── src/                        # Application source code
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-staging.properties
│   │       ├── application-prod.properties  # ✨ Updated with optimizations
│   │       └── logback-spring.xml           # ✨ Updated with async logging
│   └── test/
├── target/                     # Build output (gitignored)
├── .dockerignore
├── .env.example
├── .gitignore
├── docker-compose.yml          # Local development only
├── Dockerfile                  # Application container definition
├── pom.xml                     # Maven dependencies
└── README.md                   # ✨ Updated with architecture notes
```

## 🏗️ Infrastructure Repository Structure (Expected)

Your central infrastructure repository should contain:

```
infra-repo/
├── kubernetes/
│   ├── auth-service/
│   │   ├── deployment.yaml      # ⚠️ Needs JVM tuning updates
│   │   ├── configmap.yaml       # ⚠️ Needs Hibernate cache limits
│   │   ├── secret.yaml
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   └── hpa.yaml
│   ├── fleet-service/
│   ├── booking-service/
│   └── ...
├── helm/
│   └── auth-service/
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-staging.yaml
│       └── values-prod.yaml
├── argocd/
│   └── applications/
│       └── auth-service.yaml
└── terraform/
    └── ...
```

## 📋 Next Steps for Infrastructure Repository

Apply these changes to your central infrastructure repository:

1. **Read the Guide**: `docs/KUBERNETES-CONFIG-CHANGES.md`
2. **Update ConfigMap**: Add Hibernate cache limits
3. **Update Deployment**: Add JVM environment variables + memory limit adjustment
4. **Deploy to Staging**: Test with validation script
5. **Monitor**: Validate memory optimization
6. **Deploy to Production**: After successful staging validation

## 🔗 References

### This Repository (Application Code)

- Application configuration: `src/main/resources/application-prod.properties`
- Logging configuration: `src/main/resources/logback-spring.xml`
- Docker image: Built from `Dockerfile`

### Infrastructure Repository (Deployment)

- Kubernetes manifests: Apply changes from `docs/KUBERNETES-CONFIG-CHANGES.md`
- Helm charts: Update values files
- GitOps: ArgoCD/Flux application definitions

## 🎓 Benefits of This Architecture

| Aspect                     | Benefit                                                |
| -------------------------- | ------------------------------------------------------ |
| **Separation of Concerns** | App developers focus on code, DevOps on infrastructure |
| **Security**               | Sensitive configs (secrets) stay in separate repo      |
| **Deployment**             | Infrastructure changes don't require app rebuild       |
| **Versioning**             | Independent versioning of app vs infrastructure        |
| **Access Control**         | Different permissions for app vs infra repo            |
| **GitOps**                 | Infrastructure repo as single source of truth          |
| **Scalability**            | Easier to manage multiple services                     |

## 📝 Workflow Example

### Making Application Changes (This Repo)

```bash
# 1. Make code changes
git checkout -b feature/new-endpoint
# ... make changes ...

# 2. Test locally
./mvnw test
docker build -t auth-service:test .

# 3. Commit and push
git commit -am "feat: add new endpoint"
git push origin feature/new-endpoint

# 4. CI/CD builds and pushes Docker image
# GitHub Actions → Docker Hub → your-registry/auth-service:v1.2.3
```

### Making Infrastructure Changes (Infra Repo)

```bash
# 1. Update Kubernetes manifests
cd /path/to/infra-repo
git checkout -b feature/auth-service-memory-opt

# 2. Apply changes from docs/KUBERNETES-CONFIG-CHANGES.md
vim kubernetes/auth-service/deployment.yaml
vim kubernetes/auth-service/configmap.yaml

# 3. Commit and push
git commit -am "feat(auth-service): memory optimization"
git push origin feature/auth-service-memory-opt

# 4. GitOps deploys changes
# ArgoCD/Flux syncs → Kubernetes applies manifests
```

## 🔍 Validation

After infrastructure changes are deployed:

```bash
# From this repository (auth-service)
cd /path/to/exploresg-auth-service

# Run validation script
./scripts/validate-memory-optimization.sh exploresg-staging

# Or PowerShell
.\scripts\validate-memory-optimization.ps1 -Namespace exploresg-staging
```

## 📞 Support

**Questions about:**

- **Application code**: Check this repository's README.md
- **Kubernetes configs**: See `docs/KUBERNETES-CONFIG-CHANGES.md`
- **Memory optimization**: See `docs/MEMORY-OPTIMIZATION-SUMMARY.md`
- **Deployment**: Contact DevOps team for infra repo access

---

**Last Updated:** October 19, 2025  
**Architecture:** Centralized Infrastructure Repository  
**Status:** ✅ Active
