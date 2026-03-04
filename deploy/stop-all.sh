#!/bin/bash
# ============================================================
# 游韵华章 · 一键停止所有Java服务
# 用法: bash /opt/deploy/stop-all.sh
# ============================================================

DEPLOY_DIR="/opt"

echo "=========================================="
echo "  游韵华章 · 服务停止"
echo "=========================================="

for service in gateway backend ai miniprogram; do
    PID_FILE="$DEPLOY_DIR/$service.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo "■ 停止 $service (PID: $PID)..."
            kill "$PID"
            rm -f "$PID_FILE"
        else
            echo "⚠ $service (PID: $PID) 已不在运行"
            rm -f "$PID_FILE"
        fi
    else
        echo "- $service: 无PID文件，跳过"
    fi
done

echo "=========================================="
echo "  所有服务已停止"
echo "=========================================="
