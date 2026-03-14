# Multi-stage build for Eureka discovery-service (build from repo root)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy ONLY the parent POM first (assumes docker build context is repo root)
COPY pom.xml /app/pom.xml

# Pre-fetch parent-level dependencies (quiet mode)
RUN mvn -B -q -f /app/pom.xml dependency:go-offline

# Now copy the module's pom and sources
COPY microservices/discovery-service/pom.xml /app/microservices/discovery-service/pom.xml
COPY microservices/discovery-service/src /app/microservices/discovery-service/src

# Build only this module, also make required modules from parent
RUN mvn -B -q -f /app/pom.xml -pl microservices/discovery-service -am clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Optional: curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy the built jar from build stage
COPY --from=build /app/microservices/discovery-service/target/*.jar /app/app.jar

# Use non-root
USER spring:spring

# Healthcheck (Eureka default port 8761; adjust if you changed it)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

EXPOSE 8761

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]