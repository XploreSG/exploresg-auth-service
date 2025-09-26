# 🏗️ Database Schema Management - Auto Schema Generation

## Overview

The ExploresSG Auth Service now uses **JPA automatic schema generation** instead of Flyway migrations for simplified development and deployment.

## How It Works

### 🔄 Development Environment (`dev` profile)

- **Database**: H2 in-memory
- **Schema Strategy**: `hibernate.ddl-auto=update`
- **Behavior**: Automatically creates and updates tables based on JPA entities
- **Data Persistence**: Data is lost when application restarts (in-memory)

### 🐘 Production Environment (`prod` profile)

- **Database**: PostgreSQL (Docker)
- **Schema Strategy**: `hibernate.ddl-auto=update`
- **Behavior**: Automatically creates and updates tables on first run
- **Data Persistence**: Data persists in PostgreSQL database

### 🧪 Test Environment (`test` profile)

- **Database**: H2 in-memory
- **Schema Strategy**: `hibernate.ddl-auto=create-drop`
- **Behavior**: Creates fresh schema for each test run, drops after tests

## Benefits of Auto Schema Generation

✅ **Simplified Setup** - No migration files to maintain  
✅ **Automatic Updates** - Schema changes automatically applied  
✅ **Development Speed** - Faster iteration during development  
✅ **Cross-Database** - Works with H2, PostgreSQL, MySQL, etc.

## Current Database Schema

The schema is automatically generated from the `UserEntity` JPA entity:

```sql
CREATE TABLE users (
    id UUID NOT NULL,
    auth_provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    picture_url VARCHAR(500),
    updated_at TIMESTAMP(6) NOT NULL,
    user_role ENUM ('ADMIN','FLEET','USER') NOT NULL,
    user_status ENUM ('ACTIVE','PENDING_VERIFICATION','SUSPENDED'),
    PRIMARY KEY (id),
    UNIQUE KEY UK_email (email)
);
```

## Development Workflow

### 1. Make Entity Changes

```java
@Entity
@Table(name = "users")
public class UserEntity {
    // Add new fields here
    @Column(name = "new_field")
    private String newField;
}
```

### 2. Restart Application

- Hibernate automatically detects changes
- Updates database schema accordingly
- No manual migration scripts needed

### 3. Test Changes

- Schema changes applied automatically
- Application ready to use new fields

## Environment Switching

### Start with H2 (Development)

```bash
mvn spring-boot:run
# or
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Start with PostgreSQL (Production)

```bash
# Start PostgreSQL container first
cd docker
docker-compose up -d postgres

# Then start application
mvn spring-boot:run -Dspring.profiles.active=prod
```

## Database Access

### H2 Console (Development)

- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: `password`

### pgAdmin (Production)

- **URL**: http://localhost:8081
- **Email**: `admin@exploresg.com`
- **Password**: `admin123`

## Configuration Details

### Development (`application-dev.properties`)

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.h2.console.enabled=true
```

### Production (`application-prod.properties`)

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.h2.console.enabled=false
```

### Test (`application-test.properties`)

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

## Migration from Flyway (Completed)

✅ Removed Flyway dependencies from `pom.xml`  
✅ Deleted migration files from `src/main/resources/db/migration/`  
✅ Updated all environment configurations  
✅ Simplified Docker Compose setup  
✅ Updated documentation

## Future Considerations

### When to Consider Migrations Again

If you need:

- **Version Control** of schema changes
- **Production Rollbacks** capability
- **Team Collaboration** on database changes
- **Complex Data Transformations**

You can always add Flyway back later by:

1. Adding Flyway dependencies
2. Creating baseline migration
3. Switching to `hibernate.ddl-auto=validate`

---

## ✅ Current Status: **Fully Functional**

Your application now uses automatic schema generation and is ready for development and production use!
