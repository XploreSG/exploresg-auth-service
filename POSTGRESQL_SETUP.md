# 🐘 PostgreSQL Database Setup Guide

This guide explains how to set up PostgreSQL for the ExploreSG Auth Service.

## 🚀 Quick Start

### Option 1: Automated Setup (Recommended)

**Windows:**

```bash
setup-postgres.bat
```

**Linux/MacOS:**

```bash
chmod +x setup-postgres.sh
./setup-postgres.sh
```

### Option 2: Manual Setup

1. **Start PostgreSQL with Docker Compose:**

   ```bash
   cd docker
   docker-compose up -d
   ```

2. **Wait for PostgreSQL to be ready:**

   ```bash
   docker logs -f exploresg-auth-postgres
   ```

3. **Run the application with PostgreSQL:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

## 📊 Database Information

| Property     | Value              |
| ------------ | ------------------ |
| **Host**     | localhost          |
| **Port**     | 5432               |
| **Database** | exploresg_auth     |
| **Username** | exploresg_user     |
| **Password** | exploresg_password |

## 🔧 pgAdmin Access

- **URL:** http://localhost:8081
- **Email:** admin@exploresg.com
- **Password:** admin123

### Adding PostgreSQL Server in pgAdmin:

1. Open pgAdmin at http://localhost:8081
2. Login with the credentials above
3. Right-click "Servers" → "Register" → "Server"
4. **General Tab:**
   - Name: `ExploreSG Auth DB`
5. **Connection Tab:**
   - Host: `postgres` (Docker network name)
   - Port: `5432`
   - Database: `exploresg_auth`
   - Username: `exploresg_user`
   - Password: `exploresg_password`

## 🏗️ Database Schema

The database is automatically created with:

### Tables:

- **users** - User authentication and profile data

### Indexes:

- `idx_users_email` - Fast email lookups
- `idx_users_auth_provider` - OAuth provider filtering
- `idx_users_user_role` - Role-based queries
- `idx_users_user_status` - Status filtering
- `idx_users_created_at` - Time-based queries

### Triggers:

- Auto-update `updated_at` timestamp on row changes

## 🔄 Migration Management

Database migrations are handled by **Flyway**:

- **Location:** `src/main/resources/db/migration/`
- **Pattern:** `V{version}__{description}.sql`
- **Auto-run:** On application startup (prod profile)

### Migration Files:

1. `V1__Create_users_table.sql` - Initial schema
2. `V2__Seed_initial_users.sql` - Sample data

## 🌍 Environment Profiles

### Development (default)

```bash
mvn spring-boot:run
# Uses H2 in-memory database
```

### Production

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
# Uses PostgreSQL database
```

### Testing

```bash
mvn test
# Uses H2 in-memory database (isolated)
```

## 🔒 Environment Variables

Create `.env` file from `.env.example`:

```bash
cp .env.example .env
```

Update production values:

```env
DB_PASSWORD=your_secure_password
JWT_SECRET=your_super_secret_jwt_key
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

## 🛠️ Troubleshooting

### PostgreSQL Won't Start

```bash
# Check if port 5432 is in use
netstat -an | grep 5432

# View PostgreSQL logs
docker logs exploresg-auth-postgres

# Restart containers
docker-compose restart
```

### Connection Issues

```bash
# Test direct connection
docker exec -it exploresg-auth-postgres psql -U exploresg_user -d exploresg_auth

# Check application logs
mvn spring-boot:run -Dspring-boot.run.profiles=prod --debug
```

### Reset Database

```bash
# Stop and remove containers with data
docker-compose down -v

# Restart fresh
docker-compose up -d
```

## 📈 Production Considerations

### Security:

- [ ] Change default passwords
- [ ] Use environment variables for secrets
- [ ] Configure SSL/TLS for database connections
- [ ] Implement database backup strategy

### Performance:

- [ ] Tune PostgreSQL configuration
- [ ] Monitor connection pool settings
- [ ] Set up read replicas if needed
- [ ] Configure proper indexes

### Monitoring:

- [ ] Set up database monitoring
- [ ] Configure alerting for connection issues
- [ ] Monitor query performance
- [ ] Track migration status

## 🔗 Useful Commands

```bash
# View running containers
docker ps

# Access PostgreSQL CLI
docker exec -it exploresg-auth-postgres psql -U exploresg_user -d exploresg_auth

# View database tables
\dt

# View table structure
\d users

# Exit PostgreSQL CLI
\q

# Stop all services
docker-compose down

# Start with fresh data
docker-compose down -v && docker-compose up -d
```
