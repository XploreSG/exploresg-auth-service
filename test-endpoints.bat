@echo off
REM Test Script for ExploresSG Auth Service Endpoints
echo.
echo ========================================
echo    ExploresSG Auth Service - Test Suite
echo ========================================
echo.

echo Testing public endpoints...
echo.

echo 1. Testing Health Check:
curl -X GET "http://localhost:8080/api/v1/test/hello" -H "Accept: application/json" 2>nul
echo.
echo.

echo 2. Testing API Info:
curl -X GET "http://localhost:8080/api/v1/test/info" -H "Accept: application/json" 2>nul
echo.
echo.

echo 3. Testing OAuth2 endpoints (should redirect):
echo    - Login: http://localhost:8080/oauth2/authorization/google
echo    - Me endpoint (requires JWT): http://localhost:8080/api/v1/auth/me
echo.

echo ========================================
echo    Note: For OAuth2 flow testing:
echo    1. Start the application: mvn spring-boot:run
echo    2. Open: http://localhost:8080/oauth2/authorization/google
echo    3. Complete Google OAuth flow
echo    4. Use returned JWT token in Authorization header
echo ========================================
echo.
pause