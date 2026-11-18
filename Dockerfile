# ==============================================================================
# Multi-stage Dockerfile for Automated Hostel Mess System (Java 21 + Spring Boot 3)
# ==============================================================================

# Stage 1: Build JAR using Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies offline for fast layer caching
RUN ./mvnw dependency:go-offline -B

# Copy application source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal JRE 21 Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for cloud security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built artifact from builder
COPY --from=builder /app/target/hostel-mess-management-system-0.0.1-SNAPSHOT.jar app.jar

# Dynamic port binding for Render ($PORT)
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
