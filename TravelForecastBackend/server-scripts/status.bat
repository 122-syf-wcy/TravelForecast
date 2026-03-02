@echo off
chcp 65001
echo ================================
echo   旅游预测服务状态检查
echo ================================
echo.

REM 检查端口占用
echo [1/3] 检查端口状态...
netstat -ano | findstr :8080 | findstr LISTENING > nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ 服务正在运行 (端口8080已监听)
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
        echo    进程 PID: %%a
    )
) else (
    echo ❌ 服务未运行 (端口8080未监听)
)
echo.

REM 检查Java进程
echo [2/3] 检查Java进程...
jps -l | findstr travel-prediction > nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ 发现travel-prediction进程:
    jps -l | findstr travel-prediction
) else (
    echo ⚠️  未发现travel-prediction进程
)
echo.

REM 测试API
echo [3/3] 测试API连接...
curl -s http://localhost:8080/api/actuator/health > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API响应正常
    curl http://localhost:8080/api/actuator/health
) else (
    echo ❌ API无响应
)
echo.

echo ================================
echo   服务信息
echo ================================
echo 🌐 API地址: http://localhost:8080/api
echo 📚 Swagger文档: http://localhost:8080/api/doc.html
echo 📝 日志文件: C:\logs\travel-prediction.log
echo.

pause

