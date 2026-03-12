#!/bin/bash

echo "========================================"
echo "  智教黔行 - API网关服务启动脚本"
echo "  Travel Forecast Gateway"
echo "========================================"
echo ""

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到Java环境，请先安装JDK 17+"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未检测到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

echo "[信息] 开始编译项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "[错误] 项目编译失败"
    exit 1
fi

echo ""
echo "[信息] 启动网关服务..."
echo "[提示] 网关地址: http://localhost:8888"
echo "[提示] 按 Ctrl+C 停止服务"
echo ""

java -jar target/travel-gateway-1.0.0.jar

