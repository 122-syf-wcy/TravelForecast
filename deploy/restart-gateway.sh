#!/bin/bash
# 重启Gateway服务
source /opt/travel/start-all.sh 2>/dev/null

# 只停Gateway
PID=$(cat /opt/travel/gateway/app.pid 2>/dev/null)
if [ -n "$PID" ]; then
    kill $PID 2>/dev/null
    echo "Stopped old gateway PID: $PID"
fi
sleep 3

# 导入环境变量
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=travel_prediction
export DB_USER=root
export DB_PASSWORD=TravelDB2025!
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=TravelPredictionSecretKey2025LiupanshuiTourismSystem

cd /opt/travel/gateway
nohup java -Xms256m -Xmx512m -jar app.jar > /opt/travel/logs/gateway.log 2>&1 &
echo $! > /opt/travel/gateway/app.pid
echo "Gateway started, PID: $!"

sleep 15
echo "=== Test ==="
curl -s -o /dev/null -w "Gateway health: HTTP %{http_code}\n" http://127.0.0.1:8888/actuator/health
curl -s -o /dev/null -w "Captcha via GW: HTTP %{http_code}\n" http://127.0.0.1:8888/api/auth/captcha
curl -s -o /dev/null -w "Captcha via Nginx: HTTP %{http_code}\n" http://127.0.0.1/api/auth/captcha
