# Multi-stage Dockerfile for Kotlin services
# Stage 1: Build
FROM gradle:9.3-jdk21 AS build

ARG SERVICE_NAME
WORKDIR /app

# Copy gradle files
COPY services/build.gradle.kts services/settings.gradle.kts services/gradle.properties ./
COPY services/gradlew services/gradlew.bat ./
COPY services/gradle ./gradle
COPY services/buildSrc ./buildSrc

# Copy common module
COPY services/common ./common

# Copy specific service
COPY services/${SERVICE_NAME} ./${SERVICE_NAME}

# Build the service
RUN ./gradlew :${SERVICE_NAME}:bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE_NAME
WORKDIR /app

# Copy the built jar
COPY --from=build /app/${SERVICE_NAME}/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
