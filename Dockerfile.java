# Build stage
FROM maven:3.9-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the jar file
COPY --from=build /app/target/alert-system-0.1.0.jar app.jar

# Copy frontend files
COPY frontend/dist ./frontend/dist

# Copy SQL files
COPY sql ./sql

# Expose port
EXPOSE 3000

# Environment variables
ENV SERVER_PORT=3000
ENV SERVER_HOST=0.0.0.0
ENV DATABASE_URL=jdbc:postgresql://localhost:5432/alert_system
ENV DATABASE_USER=admin
ENV DATABASE_PASSWORD=postgres123

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]