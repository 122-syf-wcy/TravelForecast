#!/bin/bash
# 重启Python服务
pkill -f "uvicorn" 2>/dev/null
sleep 2

# 导入环境变量
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=travel_prediction
export DB_USER=root
export DB_PASSWORD=TravelDB2025!

PYTHON=/opt/miniconda3/envs/travel/bin/python
LOG=/opt/travel/logs

cd /opt/travel/prediction/src
nohup $PYTHON -m uvicorn main:app --host 0.0.0.0 --port 8001 > $LOG/prediction.log 2>&1 &
echo "Prediction PID: $!"

cd /opt/travel/digital-human/backend
nohup $PYTHON -m uvicorn main:app --host 0.0.0.0 --port 8083 > $LOG/digital-human.log 2>&1 &
echo "DigitalHuman PID: $!"

sleep 5
echo "=== Port Check ==="
ss -tlnp | grep -E '8001|8083'
