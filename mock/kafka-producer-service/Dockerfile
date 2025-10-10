# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and frontend
COPY src ./src
COPY frontend ./frontend

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="Alert System Team"
LABEL description="Kafka Producer Service for Alert System"

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the jar file from build stage
COPY --from=build /build/target/kafka-producer-service.jar app.jar

# Create data and upload directories
RUN mkdir -p /app/data /app/uploads && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8888/api/kafka/health || exit 1

# Environment variables with defaults
ENV SERVER_PORT=8888 \
    SERVER_HOST=0.0.0.0 \
    KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
    DATA_DIR=/app/data \
    UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

