# Multi-stage build for optimized image
FROM maven:3.9-openjdk-17-slim AS build
WORKDIR /app

# Copy parent POM and service POM
COPY ../../pom.xml ../pom.xml
COPY pom.xml .

# Download dependencies for caching
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage - use distroless for enhanced security
FROM gcr.io/distroless/java17-debian11:nonroot AS runtime

# Security labels for container scanning
LABEL org.opencontainers.image.title="Contract Service"
LABEL org.opencontainers.image.description="Legal AI Contract Analysis Service" 
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Legal AI Inc"
LABEL org.opencontainers.image.licenses="MIT"
LABEL maintainer="security@legal-ai.com"
LABEL org.opencontainers.image.source="https://github.com/legal-ai/contract-service"

# Copy JAR from build stage with proper ownership
COPY --from=build --chown=nonroot:nonroot /app/target/*.jar app.jar

# Use nonroot user (already set in distroless base image)
USER nonroot:nonroot

# Expose port
EXPOSE 8081

# Security: Use exec form to avoid shell and set security flags
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dfile.encoding=UTF-8", \
    "-Djava.net.preferIPv4Stack=true", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", \
    "app.jar"]