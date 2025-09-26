# 🔐 **Environment Setup Guide**

## Overview

This guide helps you set up the required environment variables and Google OAuth2 credentials for the ExploresSG Auth Service.

## 🚨 **Security Notice**

**NEVER commit actual credentials to Git!** Always use environment variables or local configuration files that are excluded from version control.

## 📋 **Prerequisites**

1. **Google Cloud Console Account**
2. **Google OAuth2 Application** configured
3. **Environment Variables** set up locally

---

## 🔧 **Step 1: Google OAuth2 Setup**

### 1.1 Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google+ API** or **Google Identity API**

### 1.2 Create OAuth2 Credentials
1. Navigate to **APIs & Services > Credentials**
2. Click **Create Credentials > OAuth 2.0 Client IDs**
3. Choose **Web application**
4. Add authorized redirect URIs:
   ```
   http://localhost:8080/login/oauth2/code/google
   http://localhost:3000/auth/callback (if using frontend)
   ```
5. Save and note down:
   - **Client ID** (safe to expose)
   - **Client Secret** (keep private!)

---

## 🔧 **Step 2: Environment Configuration**

### 2.1 Create Local Environment File
```bash
# Copy the example file
cp .env.example .env

# Edit with your actual values
notepad .env  # Windows
# or
nano .env     # Linux/Mac
```

### 2.2 Set Environment Variables

**Option A: Using .env file** (Recommended for development)
```bash
# .env file (DO NOT commit this file!)
GOOGLE_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-your-actual-client-secret
JWT_SECRET=your-super-secure-jwt-key-at-least-32-characters
```

**Option B: System Environment Variables**
```bash
# Windows (PowerShell)
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:JWT_SECRET="your-jwt-secret"

# Linux/Mac
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export JWT_SECRET="your-jwt-secret"
```

---

## 🚀 **Step 3: Run the Application**

### 3.1 Development Mode
```bash
# Load environment variables (if using .env file)
source .env  # Linux/Mac
# or use dotenv tools for Windows

# Start application
mvn spring-boot:run
```

### 3.2 Production Mode
```bash
# Set production environment variables
export SPRING_PROFILES_ACTIVE=prod
export GOOGLE_CLIENT_ID="prod-client-id"
export GOOGLE_CLIENT_SECRET="prod-client-secret"
export JWT_SECRET="production-jwt-secret"

# Start with production profile
mvn spring-boot:run -Dspring.profiles.active=prod
```

---

## ✅ **Step 4: Verify Setup**

### 4.1 Test OAuth2 Flow
1. Start the application
2. Open browser: `http://localhost:8080/oauth2/authorization/google`
3. Should redirect to Google login
4. After login, should redirect back with JWT token

### 4.2 Test API Endpoints
```bash
# Health check (should work without auth)
curl http://localhost:8080/health

# Protected endpoint (requires JWT token)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8080/api/v1/auth/me
```

---

## 🔒 **Security Best Practices**

### ✅ **DO:**
- Use environment variables for all secrets
- Use different credentials for dev/staging/prod
- Rotate secrets regularly
- Use strong, random JWT secrets (32+ characters)
- Keep `.env` file in `.gitignore`

### ❌ **DON'T:**
- Commit credentials to version control
- Share secrets in plain text
- Use weak or predictable secrets
- Reuse production secrets in development
- Expose client secrets in frontend code

---

## 🆘 **Troubleshooting**

### Issue: OAuth2 Redirect Mismatch
**Solution:** Ensure redirect URIs in Google Console match your application URLs

### Issue: JWT Token Invalid
**Solution:** Check that `JWT_SECRET` environment variable is set correctly

### Issue: Application Won't Start
**Solution:** Verify all required environment variables are set

### Issue: GitHub Blocks Push (Secrets Detected)
**Solution:** Remove hardcoded secrets, use environment variables instead

---

## 📞 **Support**

If you encounter issues:
1. Check the application logs
2. Verify environment variables are loaded
3. Confirm Google OAuth2 configuration
4. Review the troubleshooting section above

---

## 🎯 **Quick Start Summary**

```bash
# 1. Set up Google OAuth2 credentials
# 2. Copy and configure environment
cp .env.example .env
# Edit .env with your actual values

# 3. Start the application
mvn spring-boot:run

# 4. Test OAuth2 flow
# Open: http://localhost:8080/oauth2/authorization/google
```

Your ExploresSG Auth Service is now secure and ready for development! 🚀