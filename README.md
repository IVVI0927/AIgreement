# LegalAI 微服务架构系统

## 🏗️ 架构概述

LegalAI 已升级为基于 Spring Cloud 的微服务架构，支持分布式 LLM 处理和水平扩展。

### 服务架构

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

## 🚀 快速启动

### 1. 环境要求

- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### 2. 启动所有服务

```bash
# 克隆项目
git clone <repository-url>
cd legalAI

# 启动所有微服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 3. 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| API Gateway | http://localhost:8080 | 统一入口 |
| Eureka | http://localhost:8761 | 服务发现 |
| Grafana | http://localhost:3000 | 监控面板 |
| Kibana | http://localhost:5601 | 日志分析 |
| Prometheus | http://localhost:9090 | 指标监控 |

## 📊 微服务详情

### 1. API Gateway (端口 8080)
- **功能**: 统一入口、路由、负载均衡、熔断
- **技术**: Spring Cloud Gateway + Resilience4j

### 2. Contract Service (端口 8081)
- **功能**: 合同管理、存储、查询
- **技术**: Spring Boot + JPA + PostgreSQL

### 3. LLM Service (端口 8082)
- **功能**: LLM 分析、分布式处理、缓存
- **技术**: Spring WebFlux + Redis + Circuit Breaker

### 4. File Service (端口 8083)
- **功能**: 文件上传、文本提取
- **技术**: Spring Boot + PDFBox + Apache POI

### 5. User Service (端口 8084)
- **功能**: 用户管理、认证授权
- **技术**: Spring Security + JWT

### 6. Notification Service (端口 8085)
- **功能**: 通知推送、消息队列
- **技术**: Spring Boot + WebSocket

## 🔧 开发指南

### 本地开发

```bash
# 1. 启动基础设施
docker-compose up -d postgres redis llama-server

# 2. 启动单个服务
cd contract-service
mvn spring-boot:run

# 3. 或者启动所有服务
mvn clean install
mvn spring-boot:run -pl discovery-service
mvn spring-boot:run -pl config-service
mvn spring-boot:run -pl api-gateway
# ... 其他服务
```

### 服务间通信

```java
// 使用 OpenFeign 进行服务间调用
@FeignClient(name = "llm-service")
public interface LlmClient {
    @PostMapping("/api/llm/analyze")
    AnalysisResponse analyze(@RequestBody AnalysisRequest request);
}
```

## 📈 监控与运维

### 1. 健康检查
```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 2. 指标监控
- **Prometheus**: 收集指标数据
- **Grafana**: 可视化监控面板
- **ELK Stack**: 日志聚合分析

### 3. 分布式追踪
- 使用 Spring Cloud Sleuth + Zipkin
- 追踪请求链路
- 性能分析

## 🔒 安全配置

### 1. JWT 认证
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000 # 24小时
```

### 2. CORS 配置
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

## 🚀 部署

### Docker 部署
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### Kubernetes 部署
```bash
# 应用 Kubernetes 配置
kubectl apply -f k8s/

# 查看服务状态
kubectl get pods
kubectl get services
```

## 📝 API 文档

### 合同分析 API
```http
POST /api/contracts/analyze
Content-Type: application/json

{
  "title": "合同标题",
  "content": "合同内容",
  "userId": "user123"
}
```

### LLM 分析 API
```http
POST /api/llm/analyze
Content-Type: application/json

{
  "content": "分析内容",
  "analysisType": "risk",
  "language": "zh"
}
```

## 🐛 故障排除

### 常见问题

1. **服务无法启动**
   - 检查端口是否被占用
   - 确认数据库连接正常
   - 查看服务日志

2. **LLM 服务无响应**
   - 确认 LLaMA 服务器运行正常
   - 检查网络连接
   - 查看熔断器状态

3. **性能问题**
   - 检查 Redis 缓存命中率
   - 监控数据库连接池
   - 分析服务响应时间

## 📞 支持

如有问题，请查看：
- [服务日志](./logs/)
- [监控面板](http://localhost:3000)
- [API 文档](./docs/api.md)
