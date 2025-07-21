#!/bin/bash

echo "🚀 LegalAI 本地开发启动脚本"
echo "============================"

# 检查 Java 版本
if ! java -version 2>&1 | grep -q "version \"17"; then
    echo "❌ 需要 Java 17，当前版本："
    java -version
    exit 1
fi

# 检查 Maven
if ! mvn -version > /dev/null 2>&1; then
    echo "❌ Maven 不可用"
    exit 1
fi

echo "📦 编译项目..."
mvn clean compile -q

echo "🔧 启动原有单体应用..."
echo "  这将启动原有的 LegalAI 应用在端口 8080"
echo "  包含所有功能：合同分析、文件上传、LLM 处理"
echo ""

# 启动原有的单体应用
cd src/main/java/com/example/legalAI
java -cp "../../../../../target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.example.legalAI.LegalAiApplication &

echo "✅ 应用启动完成！"
echo ""
echo "📋 访问地址："
echo "  LegalAI API: http://localhost:8080"
echo "  Health Check: http://localhost:8080/actuator/health"
echo ""
echo "🔍 查看日志："
echo "  查看控制台输出"
echo ""
echo "🛑 停止服务："
echo "  按 Ctrl+C 停止" 