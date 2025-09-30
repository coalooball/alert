# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Alert System - Java Spring Boot Backend

## Technology Stack
- **Backend**: Java 17 + Spring Boot 3.2.0
- **Database**: PostgreSQL (main) + ClickHouse (alert storage)
- **Authentication**: JWT
- **Build Tool**: Maven
- **Frontend**: Vue 3 + TypeScript
- **Message Queue**: Apache Kafka
- **Stream Processing**: Apache Flink
- **Orchestration**: Docker Compose

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

# Help information
java -jar target/alert-system-0.1.0.jar --help
```

### Available Command Line Options
- `--port=<PORT>`: Port to bind the server to (default: 3000)
- `--host=<HOST>`: Host address to bind the server to (default: 127.0.0.1)
- `-h, --help`: Show help information
- `-V, --version`: Show version information

## Database Management

### Automatic Database Initialization
The application automatically checks if the `alert_system` database exists on startup. If not, it will:

1. 🔨 Create the `alert_system` database
2. 📊 Generate all tables using JPA (Hibernate with `ddl-auto=update`)
3. 📝 Insert default data from SQL files in `/sql/` directory
4. ✅ Create default admin user (admin/admin123)

**No manual initialization is required!** Just start the application and it will handle everything automatically.

### Database Structure
- Tables are managed by JPA entities with `ddl-auto=update`
- Default data is inserted from SQL files in `/sql/` directory
- SQL files are executed in numerical order: `001_*.sql`, `002_*.sql`, etc.
- Current SQL files:
  - `001_insert_default_clickhouse_config.sql` - Default ClickHouse configuration
  - `002_insert_alert_metadata.sql` - Alert types, subtypes, and fields
  - `003_insert_alert_storage_mapping.sql` - Storage mapping configuration
  - `004_insert_default_tags.sql` - Default tags
  - `005_insert_default_datasource_config.sql` - Kafka and REST data source configurations

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

## Additional Infrastructure Components

### ClickHouse Database
ClickHouse is used for high-performance alert data storage and analysis.

#### Start ClickHouse Container
```bash
cd docker-compose
docker-compose -f clickhouse-compose.yml up -d
```

#### Connection Details
- **Database**: alert_system
- **Username**: admin
- **Password**: clickhouse123
- **Host**: localhost (uses network_mode: host)
- **HTTP Port**: 8123
- **Native Port**: 9000

#### Access ClickHouse CLI
```bash
docker exec clickhouse-server clickhouse-client --database alert_system
```

### Apache Kafka Message Queue
Kafka is used for real-time alert data ingestion from various sources.

#### Start Kafka Container
```bash
cd docker-compose
docker-compose -f kafka-compose.yml up -d
```

#### Connection Details
- **Bootstrap Server**: localhost:9092
- **Zookeeper**: localhost:2181
- **Topics**:
  - `network-attack-alerts` - Network attack alerts
  - `malicious-sample-alerts` - Malware/malicious sample alerts
  - `host-behavior-alerts` - Host behavior anomaly alerts

## Testing and Mock Data

### Mock Data Generation
The `mock/` directory contains utilities for testing the alert processing pipeline:

#### Generate Mock Alert Data
```bash
# Generate new mock data files
cd mock
python3 generate_mock_data.py
```

#### Send Mock Data to Kafka
```bash
# Send network attack alerts
python3 kafka_producer.py --type network_attack --limit 10

# Send malicious sample alerts
python3 kafka_producer.py --type malicious_sample --limit 10

# Send host behavior alerts
python3 kafka_producer.py --type host_behavior --limit 10

# Send all types with custom delay
python3 kafka_producer.py --all --delay 0.5

# Send to custom Kafka broker
python3 kafka_producer.py --type network_attack --broker kafka.example.com:9092
```

#### Verify Data in ClickHouse
```bash
# Check record counts
docker exec clickhouse-server clickhouse-client --database alert_system --query "SELECT COUNT(*) FROM network_attack_alerts"

# View latest alerts
docker exec clickhouse-server clickhouse-client --database alert_system --query "SELECT * FROM network_attack_alerts ORDER BY created_at DESC LIMIT 5 FORMAT Vertical"
```

## High-Level Architecture

### Alert Processing Pipeline
1. **Data Ingestion**: Multiple data sources send alerts to Kafka topics
2. **Kafka Consumption**: `KafkaConsumerManager` service consumes messages from configured topics
3. **Alert Processing**: `AlertProcessingService` processes and validates incoming alerts
4. **Storage**: Processed alerts are stored in ClickHouse tables via `ClickHouseStorageService`
5. **Correlation**: `AlertCorrelationService` performs cross-alert analysis and correlation
6. **Observable Extraction**: `ObservableService` extracts IOCs and observables from alerts
7. **Stream Processing**: Flink jobs can be submitted via `FlinkJobSubmitService` for complex event processing

### Key Services
- **KafkaConsumerManager**: Manages Kafka consumer lifecycle and configuration
- **AlertProcessingService**: Core alert processing and validation logic
- **ClickHouseStorageService**: Handles ClickHouse table creation and data insertion
- **AlertCorrelationService**: Implements alert correlation algorithms
- **FlinkComputeService**: Manages Flink job execution for stream processing
- **ObservableService**: Extracts and manages security observables from alerts

### Database Schema Management
- JPA entities automatically create/update PostgreSQL tables on startup
- ClickHouse tables are dynamically created based on alert type configurations
- Alert metadata and storage mappings are configured in PostgreSQL
- Actual alert data is stored in ClickHouse for performance

## Development Workflow

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AlertProcessingServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Debugging
- Application runs on port 3000 by default
- Enable debug logging: Add `--debug` flag when running
- View Kafka consumer logs: Check logs for `KafkaConsumerManager`
- Monitor ClickHouse queries: Check ClickHouse server logs

### Common Development Tasks

#### Add New Alert Type
1. Define alert type in `alert_type` table
2. Configure fields in `alert_field` table
3. Create storage mapping in `alert_storage_mapping` table
4. Restart application to create ClickHouse table automatically

#### Modify Kafka Configuration
1. Update `kafka_data_source_config` table in PostgreSQL
2. Configuration changes are picked up automatically (30-second refresh interval)

#### Check Alert Processing Status
```bash
# View Spring Boot logs for processing details
tail -f logs/alert-system.log | grep "Processing alert"

# Check ClickHouse for stored alerts
docker exec clickhouse-server clickhouse-client --database alert_system --query "SELECT table, count() FROM system.tables WHERE database='alert_system' GROUP BY table"
```