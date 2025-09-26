#!/bin/bash
# =============================================================================
# PostgreSQL Initialization Script
# This script runs when the PostgreSQL container starts for the first time
# =============================================================================

set -e

echo "🚀 Initializing ExploreSG Auth Service Database..."

# Create additional databases if needed
# psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
#     CREATE DATABASE exploresg_auth_test;
#     GRANT ALL PRIVILEGES ON DATABASE exploresg_auth_test TO exploresg_user;
# EOSQL

echo "✅ Database initialization completed!"
echo "📊 Database: $POSTGRES_DB"
echo "👤 User: $POSTGRES_USER"
echo "🌐 Host: localhost:5432"