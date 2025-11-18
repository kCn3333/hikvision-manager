# ===========================
# BUILD STAGE
# ===========================
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better caching
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw -q dependency:go-offline

# Copy source code and build
COPY src ./src
RUN ./mvnw -q -DskipTests clean package

# ===========================
# RUNTIME STAGE
# ===========================
FROM eclipse-temurin:24-jre-alpine

# Install ffmpeg and curl for healthcheck
RUN apk update && \
    apk add --no-cache ffmpeg curl && \
    rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Switch to non-root user
USER spring

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Health check using curl
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]