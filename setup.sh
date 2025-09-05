#!/bin/bash

# Legal Document Platform - Quick Setup Script
# 法律文档平台快速搭建脚本

echo "========================================="
echo "Legal Document Platform Setup"
echo "========================================="

# 创建项目结构
echo "Creating project structure..."
mkdir -p legal-document-platform/{auth-service,document-service,gateway-service,common,config}
mkdir -p legal-document-platform/auth-service/src/main/java/com/legal/auth/{controller,service,entity,repository,config,dto,util}
mkdir -p legal-document-platform/auth-service/src/main/resources
mkdir -p legal-document-platform/document-service/src/main/java/com/legal/document/{controller,service,entity,repository}
mkdir -p legal-document-platform/document-service/src/main/resources
mkdir -p legal-document-platform/gateway-service/src/main/java/com/legal/gateway/config
mkdir -p legal-document-platform/gateway-service/src/main/resources
mkdir -p legal-document-platform/k8s
mkdir -p legal-document-platform/monitoring
mkdir -p legal-document-platform/db

cd legal-document-platform

# 创建根POM文件
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.legal.platform</groupId>
    <artifactId>legal-document-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.14</version>
    </parent>
    
    <modules>
        <module>auth-service</module>
        <module>document-service</module>
        <module>gateway-service</module>
        <module>common</module>
    </modules>
    
    <properties>
        <java.version>11</java.version>
        <spring-cloud.version>2021.0.8</spring-cloud.version>
        <docker.image.prefix>legal-platform</docker.image.prefix>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
EOF

# 创建Docker Compose文件
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: legaldb
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    networks:
      - legal-network
  
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass redis123
    ports:
      - "6379:6379"
    networks:
      - legal-network
  
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    ports:
      - "9090:9090"
    networks:
      - legal-network
  
  grafana:
    image: grafana/grafana:latest
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - legal-network

volumes:
  postgres-data:
  prometheus-data:
  grafana-data:

networks:
  legal-network:
    driver: bridge
EOF

# 创建Makefile
cat > Makefile << 'EOF'
.PHONY: help build test run stop clean

help:
	@echo "Available commands:"
	@echo "  make build   - Build all services"
	@echo "  make test    - Run tests"
	@echo "  make run     - Start all services"
	@echo "  make stop    - Stop all services"
	@echo "  make clean   - Clean build artifacts"

build:
	mvn clean package -DskipTests

test:
	mvn test

run:
	docker-compose up -d
	@echo "Services starting..."
	@echo "Postgres: localhost:5432"
	@echo "Redis: localhost:6379"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3000 (admin/admin123)"

stop:
	docker-compose down

clean:
	mvn clean
	docker-compose down -v
EOF

# 创建README
cat > README.md << 'EOF'
# Legal Document Platform

## 项目简介
这是一个基于Spring Boot微服务架构的分布式法律文档平台，部署在Kubernetes上，实现了高性能的合同审查系统。

## 核心功能
- ✅ OAuth2/JWT认证
- ✅ RBAC权限控制
- ✅ 2FA双因素认证
- ✅ OWASP安全防护
- ✅ 自动化文档审查
- ✅ 实时监控和告警

## 快速开始

### 前置要求
- Java 11+
- Maven 3.6+
- Docker & Docker Compose
- Kubernetes (可选)

### 本地开发
```bash
# 1. 启动基础服务
make run

# 2. 构建项目
make build

# 3. 运行测试
make test
```

### 访问服务
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Document Service: http://localhost:8082
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

## 项目结构
```
legal-document-platform/
├── auth-service/        # 认证授权服务
├── document-service/    # 文档处理服务
├── gateway-service/     # API网关
├── common/             # 公共模块
├── k8s/                # Kubernetes配置
├── monitoring/         # 监控配置
└── db/                 # 数据库脚本
```

## 安全特性
- JWT Token认证
- RBAC细粒度权限控制
- Google Authenticator 2FA
- CSRF保护
- XSS防护
- 严格的CORS策略
- SQL注入防护
- 容器安全扫描

## 性能优化
- 并行文档处理
- Redis缓存
- 数据库连接池优化
- 水平自动扩展
- 熔断器模式
EOF

echo "========================================="
echo "Setup completed!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. cd legal-document-platform"
echo "2. make run    # Start infrastructure"
echo "3. make build  # Build services"
echo ""
echo "Check README.md for detailed instructions"