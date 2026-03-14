FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY microservices/contract-service/pom.xml /app/microservices/contract-service/pom.xml

RUN mvn -B -q -f /app/pom.xml -pl microservices/contract-service -am dependency:go-offline

COPY microservices/contract-service/src /app/microservices/contract-service/src

RUN mvn -B -q -f /app/pom.xml -pl microservices/contract-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/microservices/contract-service/target/*.jar /app/app.jar

USER spring:spring

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx768m -Xms384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -jar /app/app.jar"]
