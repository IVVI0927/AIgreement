#!/bin/bash

echo "🚀 LegalAI 简化版启动脚本"
echo "=========================="

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

echo "🔧 启动基础服务..."
docker-compose -f docker-compose-simple.yml up -d

echo "⏳ 等待服务启动..."
sleep 10

echo "✅ 基础服务启动完成！"
echo ""
echo "📋 服务访问地址："
echo "  PostgreSQL: localhost:5432"
echo "  Redis:      localhost:6379"
echo "  LLaMA:      localhost:11434"
echo ""
echo "🔍 查看服务状态："
echo "  docker-compose -f docker-compose-simple.yml ps"
echo ""
echo "📝 查看日志："
echo "  docker-compose -f docker-compose-simple.yml logs -f"
echo ""
echo "🛑 停止服务："
echo "  docker-compose -f docker-compose-simple.yml down" 