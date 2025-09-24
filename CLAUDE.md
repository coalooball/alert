# Alert System - Java Spring Boot Backend

## Technology Stack
- **Backend**: Java 17 + Spring Boot 3.2.0
- **Database**: PostgreSQL
- **Authentication**: JWT
- **Build Tool**: Maven
- **Frontend**: Vue 3 + TypeScript

## PostgreSQL Database Connection

### Docker Compose Setup
PostgreSQL database is configured in `docker-compose/postgres-compose.yml`

#### Connection Details
- **Database**: alert_system
- **Username**: admin
- **Password**: postgres123
- **Host**: localhost
- **Port**: 5432 (default PostgreSQL port)
- **Container Name**: postgres-server

#### Start PostgreSQL Container
```bash
cd docker-compose
docker-compose -f postgres-compose.yml up -d
```

#### Connect to PostgreSQL via Docker
```bash
# Connect using psql
docker exec -it postgres-server psql -U admin -d alert_system
```

#### Connection String
```
# JDBC connection string for Java
jdbc:postgresql://localhost:5432/alert_system
```

### Docker Management Commands
```bash
# Stop the container
docker-compose -f postgres-compose.yml down

# View logs
docker logs postgres-server

# Check container status
docker ps | grep postgres-server
```

## Build and Run Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL (via Docker)

### Build the Project
```bash
# Clean and build
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### Run the Application

#### Using Maven
```bash
# Default configuration (127.0.0.1:3000)
mvn spring-boot:run

# Custom port
mvn spring-boot:run -Dspring-boot.run.arguments="--port=8080"
```

#### Using JAR file
```bash
# Default configuration (127.0.0.1:3000)
java -jar target/alert-system-0.1.0.jar

# Custom port
java -jar target/alert-system-0.1.0.jar --port=8080

# Custom host and port
java -jar target/alert-system-0.1.0.jar --host=0.0.0.0 --port=8080

# Initialize database (drop and recreate)
java -jar target/alert-system-0.1.0.jar --init-db

# Help information
java -jar target/alert-system-0.1.0.jar --help
```

### Available Command Line Options
- `--port=<PORT>`: Port to bind the server to (default: 3000)
- `--host=<HOST>`: Host address to bind the server to (default: 127.0.0.1)
- `--init-db`: Initialize database (drop and recreate)
- `-h, --help`: Show help information
- `-V, --version`: Show version information

## Database Management

### Initialize Database
```bash
# Drop and recreate the entire database with default admin user
java -jar target/alert-system-0.1.0.jar --init-db
```

This command will:
1. 🗑️ Drop the 'alert_system' database (if exists)
2. 🔨 Create a new 'alert_system' database
3. 📄 Execute all SQL files in order
4. ✅ Create default admin user (admin/admin123)
5. 🎉 Complete initialization and exit

### Database Structure
- SQL files located in `/sql/` directory
- Executed in numerical order: `001_*.sql`, `002_*.sql`, etc.
- Current files:
  - `001_create_users_table.sql` - User table structure
  - `002_create_users_indexes.sql` - Database indexes

## API Endpoints

### Authentication
- `POST /api/login` - User login
- `POST /api/logout` - User logout
- `GET /api/me` - Get current user info

### User Management (Admin only)
- `GET /api/users` - Get all users
- `POST /api/users` - Create new user
- `DELETE /api/users/{id}` - Delete user

### System
- `GET /api/system-info` - Get system information

## Default Credentials
- **Username**: admin
- **Password**: admin123

## Frontend Development
Frontend files are served from `frontend/dist/` directory. Make sure to build the frontend before running the backend:

```bash
cd frontend
npm install
npm run build
```

## Configuration
Main configuration file: `src/main/resources/application.yml`

Key configuration options:
- Server port and host
- Database connection
- JWT secret and expiration
- CORS settings
- Static resource locations

## Project Structure
```
├── src/main/java/com/alert/system/
│   ├── config/          # Spring configuration classes
│   ├── controller/      # REST API controllers
│   ├── dto/            # Data Transfer Objects
│   ├── entity/         # JPA entities
│   ├── enums/          # Enum definitions
│   ├── repository/     # JPA repositories
│   ├── security/       # Security and JWT components
│   ├── service/        # Business logic services
│   └── AlertSystemApplication.java  # Main application class
├── src/main/resources/
│   └── application.yml  # Spring Boot configuration
├── frontend/           # Vue.js frontend application
├── sql/               # Database SQL scripts
├── docker-compose/    # Docker configuration files
└── pom.xml           # Maven project configuration
```

## Environment Variables
- `SERVER_PORT` - Server port (default: 3000)
- `SERVER_HOST` - Server host (default: 127.0.0.1)
- `DATABASE_URL` - JDBC database URL
- `DATABASE_USER` - Database username (default: admin)
- `DATABASE_PASSWORD` - Database password (default: postgres123)
- `JWT_SECRET` - JWT secret key