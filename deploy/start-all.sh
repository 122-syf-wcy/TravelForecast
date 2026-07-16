#!/bin/bash
# ============================================================
# 智教黔行 · 全服务一键启停脚本
# 用法: bash /opt/travel/start-all.sh [start|stop|restart|status]
# ============================================================

BASE="${TRAVEL_HOME:-/opt/travel}"
LOG="$BASE/logs"
mkdir -p "$LOG"

# ---------------- 自动加载 secrets/.env ----------------
# 优先顺序：脚本同级 -> 仓库根 secrets/ -> /opt/travel/secrets/
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
SECRETS_FILE=""
for cand in "$REPO_ROOT/secrets/.env" "$BASE/secrets/.env" "$SCRIPT_DIR/.env"; do
    if [ -f "$cand" ]; then
        SECRETS_FILE="$cand"
        break
    fi
done
if [ -z "$SECRETS_FILE" ]; then
    echo "❌ 未找到 secrets/.env"
    echo "   请复制 secrets/.env.example 为 secrets/.env 并填入真实密钥"
    exit 1
fi
echo "✓ 加载密钥文件: $SECRETS_FILE"
set -a
# shellcheck disable=SC1090
source "$SECRETS_FILE"
set +a
# -------------------------------------------------------

# ---------------- 自动定位 JDK 17 ----------------
# Lombok 1.18.30/1.18.34 与 JDK 21+ 不兼容，避免编译期/启动期 TypeTag 错误。
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
    for cand in \
        "/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home" \
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" \
        "$HOME/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home" \
        "/usr/lib/jvm/java-17-openjdk" \
        "/usr/lib/jvm/java-17-openjdk-amd64" \
        "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"; do
        if [ -x "$cand/bin/java" ] && "$cand/bin/java" -version 2>&1 | grep -q 'version "17'; then
            export JAVA_HOME="$cand"
            break
        fi
    done
fi
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "✓ 使用 JDK: $JAVA_HOME"
else
    echo "⚠️  未找到 JDK 17，仍沿用 PATH 中的 java，可能遇到 Lombok 兼容问题"
fi
# -------------------------------------------------------

# 固定环境变量（非敏感）
export DB_HOST=${DB_HOST:-127.0.0.1}
export DB_PORT=${DB_PORT:-3306}
export DB_NAME=${DB_NAME:-travel_prediction}
export DB_USER=${DB_USER:-root}
export REDIS_HOST=${REDIS_HOST:-127.0.0.1}
export REDIS_PORT=${REDIS_PORT:-6379}
export REDIS_PASSWORD=${REDIS_PASSWORD:-}
export WECHAT_APPID=${WECHAT_APPID:-}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
export PREDICTION_SERVICE_URL=${PREDICTION_SERVICE_URL:-http://localhost:8001}
export PREDICTION_CORS_ORIGINS=${PREDICTION_CORS_ORIGINS:-*}
export DIGITAL_HUMAN_PORT=${DIGITAL_HUMAN_PORT:-8083}
export PREDICTION_SERVICE_PORT=${PREDICTION_SERVICE_PORT:-8001}
# 邮箱授权码未配置时不应影响服务整体健康状态；邮件发送失败由业务日志暴露。
export MANAGEMENT_HEALTH_MAIL_ENABLED=${MANAGEMENT_HEALTH_MAIL_ENABLED:-false}

# 校验关键密钥是否就位
for var in DB_PASSWORD JWT_SECRET DASHSCOPE_API_KEY WECHAT_APPSECRET; do
    if [ -z "${!var}" ]; then
        echo "❌ 密钥 $var 未在 $SECRETS_FILE 中设置"
        exit 1
    fi
done

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

# 解析 Python 解释器：优先使用环境变量 TRAVEL_PYTHON 指定的解释器，
# 其次探测 miniconda / system python
resolve_python() {
    if [ -n "$TRAVEL_PYTHON" ] && [ -x "$TRAVEL_PYTHON" ]; then
        echo "$TRAVEL_PYTHON"; return
    fi
    for cand in \
        "/opt/miniconda3/envs/travel/bin/python" \
        "$HOME/miniconda3/envs/travel/bin/python" \
        "$(command -v python3 2>/dev/null)" \
        "$(command -v python 2>/dev/null)"; do
        if [ -n "$cand" ] && [ -x "$cand" ]; then
            echo "$cand"; return
        fi
    done
    echo ""
}
PYTHON_BIN="$(resolve_python)"

start_python_prediction() {
    echo "▶ 启动 Python预测服务 (端口 ${PREDICTION_SERVICE_PORT})..."
    cd "$BASE/prediction/src"
    if [ -z "$PYTHON_BIN" ]; then
        echo "  ❌ 未找到可用的 Python 解释器，设置 TRAVEL_PYTHON=/path/to/python"
        return 1
    fi
    nohup "$PYTHON_BIN" -m uvicorn main:app --host 0.0.0.0 --port "${PREDICTION_SERVICE_PORT}" > "$LOG/prediction.log" 2>&1 &
    echo $! > "$BASE/prediction/service.pid"
    echo "  PID: $! (使用 $PYTHON_BIN)"
}

start_python_digital_human() {
    echo "▶ 启动 数字人服务 (端口 ${DIGITAL_HUMAN_PORT})..."
    cd "$BASE/digital-human/backend"
    if [ -z "$PYTHON_BIN" ]; then
        echo "  ❌ 未找到可用的 Python 解释器，设置 TRAVEL_PYTHON=/path/to/python"
        return 1
    fi
    nohup "$PYTHON_BIN" -m uvicorn main:app --host 0.0.0.0 --port "${DIGITAL_HUMAN_PORT}" > "$LOG/digital-human.log" 2>&1 &
    echo $! > "$BASE/digital-human/service.pid"
    echo "  PID: $! (使用 $PYTHON_BIN)"
}

# 通用健康检查：按最多 N 次、每次间隔 2 秒轮询指定 URL
health_check() {
    local name=$1 url=$2 retries=${3:-15}
    local i=0
    while [ $i -lt $retries ]; do
        if curl -sSf -m 2 "$url" >/dev/null 2>&1; then
            echo "  ✓ $name 就绪 ($url)"
            return 0
        fi
        i=$((i+1))
        sleep 2
    done
    echo "  ✗ $name 超时未就绪 ($url)，请查看 $LOG/"
    return 1
}

start_all() {
    echo "=========================================="
    echo "  智教黔行 · 启动所有服务"
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
    if command -v ss >/dev/null 2>&1; then
        ss -tlnp | grep -E '8888|8080|8081|8082|8001|8083' || echo "  (等待服务就绪...)"
    else
        lsof -nP -iTCP -sTCP:LISTEN | grep -E '8888|8080|8081|8082|8001|8083' || echo "  (等待服务就绪...)"
    fi
    echo "------------------------------------------"
    health_check "API 网关"       "http://127.0.0.1:8888/health"
    health_check "主业务后端"     "http://127.0.0.1:8080/api/actuator/health"
    health_check "AI 智能后端"    "http://127.0.0.1:8081/ai-api/actuator/health"
    health_check "小程序后端"     "http://127.0.0.1:8082/actuator/health"
    health_check "客流预测服务"   "http://127.0.0.1:${PREDICTION_SERVICE_PORT}/health"
    health_check "数字人服务"     "http://127.0.0.1:${DIGITAL_HUMAN_PORT}/health"
    echo "=========================================="
    echo "  日志目录: $LOG/"
    echo "=========================================="
}

show_status() {
    echo "=== 服务状态 ==="
    if command -v ss >/dev/null 2>&1; then
        ss -tlnp | grep -E '8888|8080|8081|8082|8001|8083' 2>/dev/null || echo "无服务运行"
    else
        lsof -nP -iTCP -sTCP:LISTEN | grep -E '8888|8080|8081|8082|8001|8083' || echo "无服务运行"
    fi
}

case "${1:-start}" in
    start)   start_all ;;
    stop)    stop_all ;;
    restart) stop_all; sleep 2; start_all ;;
    status)  show_status ;;
    *)       echo "用法: $0 {start|stop|restart|status}" ;;
esac
