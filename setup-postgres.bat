@echo off
REM =============================================================================
REM PostgreSQL Setup Script for ExploreSG Auth Service (Windows)
REM =============================================================================

echo 🐘 Setting up PostgreSQL for ExploreSG Auth Service...

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed. Please install Docker first.
    pause
    exit /b 1
)

REM Navigate to docker directory
cd /d "%~dp0docker"

echo 🚀 Starting PostgreSQL and pgAdmin containers...

REM Start the services
docker-compose up -d

echo ⏳ Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

REM Check if PostgreSQL is ready
docker exec exploresg-auth-postgres pg_isready -U exploresg_user -d exploresg_auth >nul 2>&1
if errorlevel 1 (
    echo ❌ PostgreSQL failed to start. Please check Docker logs:
    echo    docker logs exploresg-auth-postgres
) else (
    echo ✅ PostgreSQL is ready!
    echo.
    echo 📊 Database Information:
    echo    Host: localhost
    echo    Port: 5432
    echo    Database: exploresg_auth
    echo    Username: exploresg_user
    echo    Password: exploresg_password
    echo.
    echo 🔧 pgAdmin Information:
    echo    URL: http://localhost:8081
    echo    Email: admin@exploresg.com
    echo    Password: admin123
    echo.
    echo 🏃‍♂️ To run your application with PostgreSQL:
    echo    mvn spring-boot:run -Dspring-boot.run.profiles=prod
)

pause