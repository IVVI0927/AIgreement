#!/bin/bash

echo "🚀 LegalAI 微服务系统启动脚本"
echo "================================"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 Docker Compose 是否可用
if ! docker-compose version > /dev/null 2>&1; then
    echo "❌ Docker Compose 不可用"
    exit 1
fi

echo "📦 构建微服务镜像..."
docker-compose build

echo "🔧 启动基础设施服务..."
docker-compose up -d postgres redis llama-server

echo "⏳ 等待数据库启动..."
sleep 10

echo "🌐 启动微服务..."
docker-compose up -d discovery-service config-service
sleep 5

docker-compose up -d api-gateway contract-service llm-service file-service user-service notification-service
sleep 5

echo "📊 启动监控服务..."
docker-compose up -d prometheus grafana elasticsearch kibana logstash

echo "✅ 所有服务启动完成！"
echo ""
echo "📋 服务访问地址："
echo "  API Gateway: http://localhost:8080"
echo "  Eureka:      http://localhost:8761"
echo "  Grafana:     http://localhost:3000 (admin/admin)"
echo "  Kibana:      http://localhost:5601"
echo "  Prometheus:  http://localhost:9090"
echo ""
echo "🔍 查看服务状态："
echo "  docker-compose ps"
echo ""
echo "📝 查看日志："
echo "  docker-compose logs -f [service-name]"
echo ""
echo "🛑 停止服务："
echo "  docker-compose down" 