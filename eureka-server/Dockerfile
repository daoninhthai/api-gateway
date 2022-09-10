# =============================================================
# Multi-stage Dockerfile for Eureka Server
# Stage 1: Build with Maven
# Stage 2: Runtime with lightweight JRE
# =============================================================

# ---- Build Stage ----
FROM maven:3.8-openjdk-11 AS build

WORKDIR /app

# Copy parent POM first (for dependency caching)
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl eureka-server -am -B

# Copy source code
COPY eureka-server/src eureka-server/src

# Build the application
RUN mvn clean package -pl eureka-server -am -DskipTests -B

# ---- Runtime Stage ----
FROM openjdk:11-jre-slim

LABEL maintainer="daoninhthai <thaimeo1131@gmail.com>"
LABEL description="Eureka Service Discovery Server"
LABEL version="1.0.0"

WORKDIR /app

# Create a non-root user for security
RUN groupadd -r eureka && useradd -r -g eureka eureka

# Copy the built JAR from the build stage
COPY --from=build /app/eureka-server/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R eureka:eureka /app

USER eureka

# Expose Eureka port
EXPOSE 8761

# JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
