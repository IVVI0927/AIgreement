# LegalAI 微服务架构

## 🏗️ 架构概述

这是一个简化的微服务架构，包含3个核心服务：

```
Chrome Extension
       ↓
   API Gateway (8080)
       ↓
   ┌─────────┬─────────┐
   ↓         ↓         ↓
Contract  LLM      File Upload
Service   Service  (Contract Service)
(8081)    (8082)   (8081)
   ↓         ↓
PostgreSQL LLaMA
Database   Server
```

## 🚀 快速启动

### 1. 启动微服务
```bash
./start-microservices.sh
```

### 2. 验证服务
```bash
# 检查 API Gateway
curl http://localhost:8080/actuator/health

# 检查 Contract Service
curl http://localhost:8081/actuator/health

# 检查 LLM Service
curl http://localhost:8082/actuator/health
```

## 📊 服务详情

### API Gateway (端口 8080)
- **功能**: 统一入口、路由
- **技术**: Spring Cloud Gateway
- **路由规则**:
  - `/api/contracts/**` → Contract Service (8081)
  - `/api/llm/**` → LLM Service (8082)

### Contract Service (端口 8081)
- **功能**: 合同管理、文件上传、数据库操作
- **技术**: Spring Boot + JPA + PostgreSQL
- **API**:
  - `POST /api/contracts/analyze` - 合同分析
  - `POST /api/contracts/upload` - 文件上传
  - `GET /api/contracts` - 获取合同列表

### LLM Service (端口 8082)
- **功能**: LLM 分析处理
- **技术**: Spring Boot + WebFlux
- **API**:
  - `POST /api/llm/analyze` - LLM 分析

## 🔧 开发指南

### 单独启动服务
```bash
# 启动 Contract Service
cd microservices/contract-service
mvn spring-boot:run

# 启动 LLM Service
cd microservices/llm-service
mvn spring-boot:run

# 启动 API Gateway
cd microservices/api-gateway
mvn spring-boot:run
```

### 查看日志
```bash
# Contract Service 日志
tail -f microservices/contract-service/contract-service.log

# LLM Service 日志
tail -f microservices/llm-service/llm-service.log

# API Gateway 日志
tail -f microservices/api-gateway/api-gateway.log
```

## 📝 API 使用示例

### 1. 上传并分析合同
```bash
# 上传文件
curl -X POST http://localhost:8080/api/contracts/upload \
  -F "file=@contract.pdf"

# 分析合同
curl -X POST http://localhost:8080/api/contracts/analyze \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Contract","content":"Contract content..."}'
```

### 2. LLM 分析
```bash
curl -X POST http://localhost:8080/api/llm/analyze \
  -H "Content-Type: application/json" \
  -d '{"content":"Analyze this contract clause..."}'
```

## 🐛 故障排除

### 1. 服务无法启动
- 检查端口是否被占用
- 确认数据库连接正常
- 查看服务日志

### 2. API Gateway 路由失败
- 确认目标服务已启动
- 检查路由配置
- 验证服务端口

### 3. LLM 服务无响应
- 确认 LLaMA 服务器运行在 11434 端口
- 检查网络连接
- 查看 LLM 服务日志

## 🔄 与单体应用的区别

| 特性 | 单体应用 | 微服务 |
|------|----------|--------|
| 部署 | 单个 JAR | 多个独立服务 |
| 扩展 | 整体扩展 | 独立扩展 |
| 故障隔离 | 单点故障 | 服务隔离 |
| 技术栈 | 统一 | 灵活选择 |
| 复杂度 | 简单 | 中等 |

## 🎯 优势

1. **服务解耦**: 各服务独立开发、部署、扩展
2. **技术灵活**: 不同服务可使用不同技术栈
3. **故障隔离**: 单个服务故障不影响整体
4. **独立扩展**: 可根据负载独立扩展服务

## 📈 下一步优化

1. **服务发现**: 添加 Eureka 服务注册
2. **配置中心**: 添加 Config Server
3. **熔断器**: 添加 Resilience4j
4. **监控**: 添加 Prometheus + Grafana
5. **容器化**: Docker 部署 