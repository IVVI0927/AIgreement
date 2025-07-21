# LegalAI 微服务架构设计文档

## 🎯 升级目标

将原有的单体 Spring Boot 应用升级为**微服务 + LLM 分布式处理系统**，实现：

- ✅ **高可用性**: 服务解耦，单点故障不影响整体
- ✅ **可扩展性**: 独立扩展不同服务
- ✅ **分布式处理**: LLM 任务分布式执行
- ✅ **监控运维**: 完整的监控和日志系统
- ✅ **安全性**: 统一的认证和授权

## 🏗️ 架构设计

### 1. 服务拆分策略

| 服务 | 端口 | 职责 | 技术栈 |
|------|------|------|--------|
| **API Gateway** | 8080 | 统一入口、路由、熔断 | Spring Cloud Gateway |
| **Discovery Service** | 8761 | 服务注册发现 | Eureka Server |
| **Config Service** | 8888 | 配置中心 | Spring Cloud Config |
| **Contract Service** | 8081 | 合同管理 | Spring Boot + JPA |
| **LLM Service** | 8082 | LLM 处理 | Spring WebFlux + Redis |
| **File Service** | 8083 | 文件处理 | Spring Boot + PDFBox |
| **User Service** | 8084 | 用户管理 | Spring Security + JWT |
| **Notification Service** | 8085 | 通知推送 | Spring Boot + WebSocket |

### 2. 数据流设计

```
Chrome Extension
       ↓
   API Gateway (8080)
       ↓
   ┌─────────┬─────────┬─────────┐
   ↓         ↓         ↓         ↓
Contract  LLM      File      User
Service   Service  Service   Service
(8081)    (8082)   (8083)    (8084)
   ↓         ↓         ↓         ↓
PostgreSQL Redis   Local    PostgreSQL
Database   Cache   Storage  Database
```

### 3. 分布式 LLM 处理

#### 3.1 处理流程
1. **任务接收**: API Gateway 接收分析请求
2. **任务分发**: 路由到 LLM Service
3. **缓存检查**: Redis 检查是否有缓存结果
4. **LLM 调用**: 调用本地 LLaMA 模型
5. **结果缓存**: 将结果存储到 Redis
6. **响应返回**: 返回分析结果

#### 3.2 异步处理
- 支持长时间运行的 LLM 任务
- 任务状态跟踪
- 结果通知机制

#### 3.3 负载均衡
- 多个 LLM 服务实例
- 智能任务分发
- 健康检查

## 🔧 技术实现

### 1. 服务发现 (Eureka)

```yaml
# application.yml
eureka:
  client:
    service-url:
      defaultZone: http://discovery-service:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 2. 配置中心 (Config Server)

```yaml
# config-service/application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-repo/config-repo
          default-label: main
```

### 3. API 网关路由

```yaml
# api-gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: contract-service
          uri: lb://contract-service
          predicates:
            - Path=/api/contracts/**
          filters:
            - name: CircuitBreaker
              args:
                name: contractCircuitBreaker
```

### 4. 熔断器配置

```java
@CircuitBreaker(name = "llmAnalysis", fallbackMethod = "analyzeFallback")
public Mono<AnalysisResponse> analyzeContract(AnalysisRequest request) {
    // LLM 分析逻辑
}
```

### 5. 缓存策略

```java
@Cacheable(value = "analysisCache", key = "#cacheKey")
public AnalysisResponse getCachedResponse(String cacheKey) {
    // 缓存逻辑
}
```

## 📊 监控体系

### 1. 指标监控 (Prometheus + Grafana)

- **应用指标**: 请求量、响应时间、错误率
- **系统指标**: CPU、内存、磁盘、网络
- **业务指标**: 分析任务数、成功率、缓存命中率

### 2. 日志聚合 (ELK Stack)

- **Elasticsearch**: 日志存储和检索
- **Logstash**: 日志收集和过滤
- **Kibana**: 日志可视化

### 3. 分布式追踪

- **Spring Cloud Sleuth**: 请求链路追踪
- **Zipkin**: 性能分析

## 🔒 安全设计

### 1. 认证授权

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer()
            .jwt()
            .and()
            .authorizeRequests()
            .antMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .build();
    }
}
```

### 2. API 网关安全

- JWT Token 验证
- Rate Limiting
- CORS 配置
- 请求过滤

### 3. 数据安全

- 数据库加密
- 敏感信息脱敏
- 审计日志

## 🚀 部署策略

### 1. Docker 容器化

```dockerfile
# Dockerfile
FROM openjdk:17-jre-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 2. Kubernetes 部署

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: llm-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: llm-service
  template:
    metadata:
      labels:
        app: llm-service
    spec:
      containers:
      - name: llm-service
        image: legalai/llm-service:latest
        ports:
        - containerPort: 8082
```

### 3. CI/CD 流水线

```yaml
# .github/workflows/deploy.yml
name: Deploy to Kubernetes
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Build and push Docker images
    - name: Deploy to Kubernetes
```

## 📈 性能优化

### 1. 缓存策略

- **Redis 缓存**: 分析结果缓存
- **本地缓存**: 配置信息缓存
- **CDN 缓存**: 静态资源缓存

### 2. 数据库优化

- **连接池**: HikariCP 连接池
- **索引优化**: 查询性能优化
- **读写分离**: 主从数据库

### 3. 异步处理

- **消息队列**: RabbitMQ/Kafka
- **异步任务**: @Async 注解
- **响应式编程**: WebFlux

## 🔄 迁移计划

### 阶段 1: 基础设施搭建
- [x] 创建微服务项目结构
- [x] 配置 Docker Compose
- [x] 设置监控系统

### 阶段 2: 核心服务迁移
- [ ] 迁移合同管理服务
- [ ] 迁移 LLM 处理服务
- [ ] 迁移文件处理服务

### 阶段 3: 网关和配置
- [ ] 部署 API Gateway
- [ ] 配置服务发现
- [ ] 设置配置中心

### 阶段 4: 监控和优化
- [ ] 部署监控系统
- [ ] 性能调优
- [ ] 安全加固

### 阶段 5: 生产部署
- [ ] Kubernetes 部署
- [ ] CI/CD 流水线
- [ ] 生产环境测试

## 🎯 预期收益

### 1. 性能提升
- **响应时间**: 减少 50% (缓存 + 异步)
- **并发能力**: 提升 300% (水平扩展)
- **可用性**: 99.9% (熔断 + 降级)

### 2. 运维效率
- **部署时间**: 从小时级到分钟级
- **故障恢复**: 自动故障转移
- **监控能力**: 全链路监控

### 3. 开发效率
- **独立开发**: 团队并行开发
- **技术选型**: 灵活选择技术栈
- **测试效率**: 独立测试和部署

## 📞 技术支持

- **文档**: [README.md](./README.md)
- **监控**: http://localhost:3000 (Grafana)
- **日志**: http://localhost:5601 (Kibana)
- **健康检查**: http://localhost:8080/actuator/health 