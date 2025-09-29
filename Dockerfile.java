# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre AS runtime

# Install required packages and create non-root user
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy the jar file from build stage
COPY --from=build /app/target/alert-system-0.1.0.jar app.jar

# Copy frontend files (built frontend should be included in jar)
COPY frontend/dist ./frontend/dist

# SQL files are already included in the jar via src/main/resources
# but we can copy them for debugging purposes
COPY src/main/resources/sql ./sql

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000/api/system-info || exit 1

# Environment variables with defaults
ENV SERVER_PORT=3000
ENV SERVER_HOST=0.0.0.0
ENV DATABASE_URL=jdbc:postgresql://postgres-db:5432/alert_system
ENV DATABASE_USER=admin
ENV DATABASE_PASSWORD=postgres123
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]