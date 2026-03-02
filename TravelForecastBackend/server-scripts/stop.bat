@echo off
chcp 65001
echo ================================
echo   停止旅游预测服务
echo ================================
echo.

REM 查找占用8080端口的进程
echo [1/2] 查找占用8080端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    set PID=%%a
    goto :found
)

:notfound
echo ℹ️  没有找到运行中的服务
pause
exit /b 0

:found
echo 找到进程 PID: %PID%
echo.

REM 停止进程
echo [2/2] 停止服务...
taskkill /F /PID %PID%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 服务已停止
) else (
    echo.
    echo ❌ 停止服务失败
)

echo.
pause

