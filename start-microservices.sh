#!/bin/bash

echo "🚀 LegalAI 微服务启动脚本"
echo "========================"

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 需要 Java 17 或更高版本，当前版本：$JAVA_VERSION"
    exit 1
else
    echo "✅ Java 版本检查通过: $JAVA_VERSION"
fi

# 检查 Maven
if ! mvn -version > /dev/null 2>&1; then
    echo "❌ Maven 不可用"
    exit 1
fi

echo "📦 编译微服务..."
cd microservices/contract-service && mvn clean compile -q && cd ../..
cd microservices/llm-service && mvn clean compile -q && cd ../..
cd microservices/api-gateway && mvn clean compile -q && cd ../..

echo "🔧 启动微服务..."
echo ""

# 启动 Contract Service (端口 8081)
echo "启动 Contract Service (端口 8081)..."
cd microservices/contract-service
mvn spring-boot:run > contract-service.log 2>&1 &
CONTRACT_PID=$!
cd ../..

# 等待 Contract Service 启动
sleep 10

# 启动 LLM Service (端口 8082)
echo "启动 LLM Service (端口 8082)..."
cd microservices/llm-service
mvn spring-boot:run > llm-service.log 2>&1 &
LLM_PID=$!
cd ../..

# 等待 LLM Service 启动
sleep 10

# 启动 API Gateway (端口 8080)
echo "启动 API Gateway (端口 8080)..."
cd microservices/api-gateway
mvn spring-boot:run > api-gateway.log 2>&1 &
GATEWAY_PID=$!
cd ../..

echo ""
echo "✅ 微服务启动完成！"
echo ""
echo "📋 服务访问地址："
echo "  API Gateway: http://localhost:8080"
echo "  Contract Service: http://localhost:8081"
echo "  LLM Service: http://localhost:8082"
echo ""
echo "🔍 查看服务状态："
echo "  curl http://localhost:8080/actuator/health"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo ""
echo "📝 查看日志："
echo "  tail -f microservices/contract-service/contract-service.log"
echo "  tail -f microservices/llm-service/llm-service.log"
echo "  tail -f microservices/api-gateway/api-gateway.log"
echo ""
echo "🛑 停止服务："
echo "  kill $CONTRACT_PID $LLM_PID $GATEWAY_PID"
echo "  或者按 Ctrl+C" 