#!/bin/bash
# ============================================================
# 游韵华章 · 全服务一键启停脚本
# 用法: bash /opt/travel/start-all.sh [start|stop|restart|status]
# ============================================================

BASE="/opt/travel"
LOG="$BASE/logs"
mkdir -p "$LOG"

# 环境变量
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=travel_prediction
export DB_USER=root
export DB_PASSWORD=TravelDB2025!
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
export JWT_SECRET=TravelPredictionSecretKey2025LiupanshuiTourismSystem
export OSS_ACCESS_KEY_ID=LTAI5t7AvM8aEFV9rxT7wVY6
export OSS_ACCESS_KEY_SECRET=SCNTM1T0KYdcqaggeOW0g4uY396OJF
export WECHAT_APPID=wx9569d09c12f8de06
export WECHAT_APPSECRET=7dc2a1fea1d738a3c233f3a4628f1d01
export DASHSCOPE_API_KEY=sk-c444b733ee0c4e29a215b07cd875929f
export SPRING_PROFILES_ACTIVE=prod

JAVA_OPTS_GW="-Xms256m -Xmx512m"
JAVA_OPTS_BE="-Xms512m -Xmx1024m"
JAVA_OPTS_AI="-Xms256m -Xmx512m"
JAVA_OPTS_MP="-Xms256m -Xmx512m"

stop_all() {
    echo "=== 停止所有服务 ==="
    for pidfile in $BASE/*/app.pid $BASE/*/service.pid; do
        [ -f "$pidfile" ] || continue
        pid=$(cat "$pidfile")
        if kill -0 "$pid" 2>/dev/null; then
            echo "  停止 PID $pid ($(dirname $pidfile | xargs basename))"
            kill "$pid" 2>/dev/null
        fi
        rm -f "$pidfile"
    done
    # 杀掉残留Java/Python进程
    pkill -f "travel.*app.jar" 2>/dev/null || true
    pkill -f "uvicorn.*8001" 2>/dev/null || true
    pkill -f "uvicorn.*8083" 2>/dev/null || true
    sleep 2
    echo "  已停止"
}

start_java() {
    local name=$1 dir=$2 opts=$3 port=$4
    echo "▶ 启动 $name (端口 $port)..."
    cd "$BASE/$dir"
    nohup java $opts -jar app.jar > "$LOG/$dir.log" 2>&1 &
    echo $! > "$BASE/$dir/app.pid"
    echo "  PID: $!"
    sleep 2
}

start_python_prediction() {
    echo "▶ 启动 Python预测服务 (端口 8001)..."
    cd "$BASE/prediction/src"
    nohup /opt/miniconda3/envs/travel/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8001 > "$LOG/prediction.log" 2>&1 &
    echo $! > "$BASE/prediction/service.pid"
    echo "  PID: $!"
}

start_python_digital_human() {
    echo "▶ 启动 数字人服务 (端口 8083)..."
    cd "$BASE/digital-human/backend"
    nohup /opt/miniconda3/envs/travel/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8083 > "$LOG/digital-human.log" 2>&1 &
    echo $! > "$BASE/digital-human/service.pid"
    echo "  PID: $!"
}

start_all() {
    echo "=========================================="
    echo "  游韵华章 · 启动所有服务"
    echo "=========================================="

    # 1. Gateway
    start_java "API网关" "gateway" "$JAVA_OPTS_GW" 8888
    # 2. Backend
    start_java "主业务后端" "backend" "$JAVA_OPTS_BE" 8080
    # 3. AI Backend
    start_java "AI智能后端" "ai-backend" "$JAVA_OPTS_AI" 8081
    # 4. MiniProgram Backend
    start_java "小程序后端" "mp-backend" "$JAVA_OPTS_MP" 8082
    # 5. Python Prediction
    start_python_prediction
    # 6. Digital Human
    start_python_digital_human

    sleep 3
    echo ""
    echo "=========================================="
    echo "  启动完成！端口状态:"
    ss -tlnp | grep -E '8888|8080|8081|8082|8001|8083' || echo "  (等待服务就绪...)"
    echo "=========================================="
    echo "  日志目录: $LOG/"
    echo "=========================================="
}

show_status() {
    echo "=== 服务状态 ==="
    ss -tlnp | grep -E '8888|8080|8081|8082|8001|8083' 2>/dev/null || echo "无服务运行"
}

case "${1:-start}" in
    start)   start_all ;;
    stop)    stop_all ;;
    restart) stop_all; sleep 2; start_all ;;
    status)  show_status ;;
    *)       echo "用法: $0 {start|stop|restart|status}" ;;
esac
