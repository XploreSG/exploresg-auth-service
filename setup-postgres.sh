#!/bin/bash
# =============================================================================
# PostgreSQL Setup Script for ExploreSG Auth Service
# =============================================================================

echo "🐘 Setting up PostgreSQL for ExploreSG Auth Service..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not available. Please install Docker Compose."
    exit 1
fi

# Navigate to docker directory
cd "$(dirname "$0")/docker"

echo "🚀 Starting PostgreSQL and pgAdmin containers..."

# Start the services
if command -v docker-compose &> /dev/null; then
    docker-compose up -d
else
    docker compose up -d
fi

echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 10

# Check if PostgreSQL is ready
if docker exec exploresg-auth-postgres pg_isready -U exploresg_user -d exploresg_auth; then
    echo "✅ PostgreSQL is ready!"
    echo ""
    echo "📊 Database Information:"
    echo "   Host: localhost"
    echo "   Port: 5432"
    echo "   Database: exploresg_auth"
    echo "   Username: exploresg_user"
    echo "   Password: exploresg_password"
    echo ""
    echo "🔧 pgAdmin Information:"
    echo "   URL: http://localhost:8081"
    echo "   Email: admin@exploresg.com"
    echo "   Password: admin123"
    echo ""
    echo "🏃‍♂️ To run your application with PostgreSQL:"
    echo "   mvn spring-boot:run -Dspring-boot.run.profiles=prod"
else
    echo "❌ PostgreSQL failed to start. Please check Docker logs:"
    echo "   docker logs exploresg-auth-postgres"
fi