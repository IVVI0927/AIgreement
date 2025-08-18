# AIgreement - Intelligent Contract Analysis Platform

## 🎯 Project Overview

AIgreement is an enterprise-grade intelligent contract analysis platform built with advanced microservices architecture and large language model technology. The system provides efficient and accurate contract risk assessment and clause analysis services. All major improvements have been successfully implemented and the system is production-ready.

## 🏗️ System Architecture

Built on Spring Cloud microservices architecture, achieving service decoupling, independent scaling, and high availability.

### Service Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Chrome Ext    │    │   API Gateway   │    │  Discovery      │
│                 │◄──►│   (Port 8080)   │◄──►│  (Port 8761)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ Contract     │ │ LLM         │ │ File       │
        │ Service      │ │ Service     │ │ Service    │
        │ (Port 8081)  │ │ (Port 8082) │ │ (Port 8083)│
        └──────────────┘ └─────────────┘ └────────────┘
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ PostgreSQL   │ │ Redis       │ │ LLaMA      │
        │ Database     │ │ Cache       │ │ Server     │
        └──────────────┘ └─────────────┘ └────────────┘
```

## 🚀 Quick Start

### 1. Prerequisites

- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### 2. Launch All Services

```bash
# Clone the project
git clone <repository-url>
cd AIgreement

# Start all microservices
docker-compose up -d

# Check service status
docker-compose ps
```

### 3. Access URLs

| Service | URL | Description |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | Unified entry point |
| Eureka | http://localhost:8761 | Service discovery |
| Grafana | http://localhost:3000 | Monitoring dashboard |
| Kibana | http://localhost:5601 | Log analysis |
| Prometheus | http://localhost:9090 | Metrics monitoring |

## 📊 Microservice Details

### 1. API Gateway (Port 8080)
- **Features**: Unified entry, routing, load balancing, circuit breaking
- **Tech Stack**: Spring Cloud Gateway + Resilience4j

### 2. Contract Service (Port 8081)
- **Features**: Contract management, storage, querying
- **Tech Stack**: Spring Boot + JPA + PostgreSQL

### 3. LLM Service (Port 8082)
- **Features**: LLM analysis, distributed processing, caching
- **Tech Stack**: Spring WebFlux + Redis + Circuit Breaker

### 4. File Service (Port 8083)
- **Features**: File upload, text extraction
- **Tech Stack**: Spring Boot + PDFBox + Apache POI

### 5. User Service (Port 8084)
- **Features**: User management, authentication & authorization
- **Tech Stack**: Spring Security + JWT

### 6. Notification Service (Port 8085)
- **Features**: Push notifications, message queuing
- **Tech Stack**: Spring Boot + WebSocket

## 🔧 Development Guide

### Local Development

```bash
# 1. Start infrastructure services
docker-compose up -d postgres redis llama-server

# 2. Start individual service
cd contract-service
mvn spring-boot:run

# 3. Or start all services
mvn clean install
mvn spring-boot:run -pl discovery-service
mvn spring-boot:run -pl config-service
mvn spring-boot:run -pl api-gateway
# ... other services
```

### Inter-Service Communication

```java
// Use OpenFeign for inter-service calls
@FeignClient(name = "llm-service")
public interface LlmClient {
    @PostMapping("/api/llm/analyze")
    AnalysisResponse analyze(@RequestBody AnalysisRequest request);
}
```

## 📈 Monitoring & Operations

### 1. Health Checks
```bash
# Check service health status
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 2. Metrics Monitoring
- **Prometheus**: Metrics data collection
- **Grafana**: Visualization dashboards
- **ELK Stack**: Log aggregation and analysis

### 3. Distributed Tracing
- Spring Cloud Sleuth + Zipkin integration
- Request chain tracing
- Performance analysis

## 🔒 Security Configuration

### 1. JWT Authentication
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000 # 24 hours
```

### 2. CORS Configuration
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        // ...
    }
}
```

## 🚀 Deployment

### Docker Deployment
```bash
# Build images
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f
```

### Kubernetes Deployment
```bash
# Apply Kubernetes configurations
kubectl apply -f k8s/

# Check service status
kubectl get pods
kubectl get services
```

## 📝 API Documentation

### Contract Analysis API
```http
POST /api/contracts/analyze
Content-Type: application/json

{
  "title": "Contract Title",
  "content": "Contract Content",
  "userId": "user123"
}
```

### LLM Analysis API
```http
POST /api/llm/analyze
Content-Type: application/json

{
  "content": "Content to analyze",
  "analysisType": "risk",
  "language": "en"
}
```

## 🐛 Troubleshooting

### Common Issues

1. **Service Won't Start**
   - Check if ports are already in use
   - Verify database connections
   - Review service logs

2. **LLM Service Not Responding**
   - Confirm LLaMA server is running
   - Check network connectivity
   - Review circuit breaker status

3. **Performance Issues**
   - Check Redis cache hit rate
   - Monitor database connection pool
   - Analyze service response times

## 📞 Support

For assistance, please check:
- [Service Logs](./logs/)
- [Monitoring Dashboard](http://localhost:3000)
- [API Documentation](./docs/api.md)
