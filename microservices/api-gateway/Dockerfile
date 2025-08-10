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

# Runtime stage
FROM openjdk:17-jre-slim AS runtime

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership and switch to non-root user
RUN chown spring:spring app.jar
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM optimization for containers
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]