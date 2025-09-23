# PostgreSQL Database Connection

## Docker Compose Setup
PostgreSQL database is configured in `docker-compose/postgres-compose.yml`

### Connection Details
- **Database**: alert_system
- **Username**: admin
- **Password**: postgres123
- **Host**: localhost (network_mode: host)
- **Port**: 5432 (default PostgreSQL port)
- **Container Name**: postgres-server

### Docker Connection Commands

#### Start PostgreSQL Container
```bash
cd docker-compose
docker-compose -f postgres-compose.yml up -d
```

#### Connect to PostgreSQL via Docker
只能通过 Docker 命令连接
```bash
# Connect using psql
docker exec -it postgres-server psql -U admin -d alert_system
```

#### Connection String Examples
```
# Standard PostgreSQL connection string
postgresql://admin:postgres123@localhost:5432/alert_system

# For Rust applications (using sqlx)
DATABASE_URL=postgresql://admin:postgres123@localhost:5432/alert_system
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

## Web Server Configuration

### Start Server with Command Line Options
```bash
# Default configuration (127.0.0.1:3000)
cargo run

# Custom port
cargo run -- --port 8080
cargo run -- -P 8080

# Custom host and port
cargo run -- --host 0.0.0.0 --port 8080
cargo run -- -H 0.0.0.0 -P 8080

# Help information
cargo run -- --help

# Version information
cargo run -- --version
```

### Available Options
- `-P, --port <PORT>`: Port to bind the server to (default: 3000)
- `-H, --host <HOST>`: Host address to bind the server to (default: 127.0.0.1)
- `--init-db`: Initialize database (drop and recreate)
- `-h, --help`: Show help information
- `-V, --version`: Show version information

## Database Management

### Initialize Database
```bash
# Drop and recreate the entire database
cargo run -- --init-db
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