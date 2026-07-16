#!/bin/bash

# ---------------- 自动加载 secrets/.env ----------------
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
for cand in "$SCRIPT_DIR/../secrets/.env" "/opt/travel/secrets/.env"; do
    if [ -f "$cand" ]; then
        set -a
        # shellcheck disable=SC1090
        source "$cand"
        set +a
        break
    fi
done
# -------------------------------------------------------
# 重启Gateway
pkill -f "java.*gateway" 2>/dev/null
sleep 3

export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
# export JWT_SECRET=<see secrets/.env>

cd /opt/travel/gateway
nohup java -Xms256m -Xmx512m -jar app.jar > /opt/travel/logs/gateway.log 2>&1 &
echo "Gateway PID: $!"
echo $! > /opt/travel/gateway/app.pid

sleep 18
echo "=== Test ==="
curl -s -o /dev/null -w "Health: %{http_code}\n" http://127.0.0.1:8888/actuator/health
curl -s -o /dev/null -w "Captcha: %{http_code}\n" http://127.0.0.1:8888/api/captcha
