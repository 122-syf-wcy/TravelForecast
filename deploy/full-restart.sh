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

# ---------------- 自动加载 secrets/.env ----------------
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
if [ -f "$REPO_ROOT/secrets/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$REPO_ROOT/secrets/.env"
    set +a
elif [ -f "/opt/travel/secrets/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "/opt/travel/secrets/.env"
    set +a
else
    echo "❌ 未找到 secrets/.env"
    echo "   请复制 secrets/.env.example 为 secrets/.env 并填入真实密钥"
    exit 1
fi
# -------------------------------------------------------

# 全部停止再重启
echo "=== 杀掉所有Java进程 ==="
for pid in $(ps aux | grep 'java.*app.jar' | grep -v grep | awk '{print $2}'); do
    echo "  kill $pid"
    kill -9 $pid
done

echo "=== 杀掉所有Python/uvicorn ==="
for pid in $(ps aux | grep uvicorn | grep -v grep | awk '{print $2}'); do
    echo "  kill $pid"
    kill -9 $pid
done

sleep 3
echo "=== 验证端口已清空 ==="
ss -tlnp | grep -E '8888|8080|8081|8082|8001|8083' || echo "  全部清空"

echo ""
echo "=== 重启所有服务 ==="
bash /opt/travel/start-all.sh start

echo ""
sleep 20
echo "=== 最终验证 ==="
echo "Gateway health:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1:8888/actuator/health
echo "Captcha via gateway:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1:8888/api/captcha
echo "Captcha via nginx:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1/api/captcha
echo "Backend direct:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1:8080/api/captcha
echo "Digital Human:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1:8083/health
echo "Prediction:"
curl -s -o /dev/null -w "  HTTP %{http_code}\n" http://127.0.0.1:8001/api/prediction/models/info
