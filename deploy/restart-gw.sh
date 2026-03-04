#!/bin/bash
# 重启Gateway
pkill -f "java.*gateway" 2>/dev/null
sleep 3

export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=TravelPredictionSecretKey2025LiupanshuiTourismSystem

cd /opt/travel/gateway
nohup java -Xms256m -Xmx512m -jar app.jar > /opt/travel/logs/gateway.log 2>&1 &
echo "Gateway PID: $!"
echo $! > /opt/travel/gateway/app.pid

sleep 18
echo "=== Test ==="
curl -s -o /dev/null -w "Health: %{http_code}\n" http://127.0.0.1:8888/actuator/health
curl -s -o /dev/null -w "Captcha: %{http_code}\n" http://127.0.0.1:8888/api/auth/captcha
