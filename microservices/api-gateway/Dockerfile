FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY microservices/api-gateway/pom.xml /app/microservices/api-gateway/pom.xml

RUN mvn -B -q -f /app/pom.xml -pl microservices/api-gateway -am dependency:go-offline

COPY microservices/api-gateway/src /app/microservices/api-gateway/src

RUN mvn -B -q -f /app/pom.xml -pl microservices/api-gateway -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/microservices/api-gateway/target/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
