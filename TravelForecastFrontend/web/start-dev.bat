@echo off
REM 旅游预测系统 - 前端开发启动脚本

echo.
echo ===================================
echo 旅游预测系统 - 前端开发环境启动
echo ===================================
echo.

cd /d "%~dp0旅游预测\web"

echo [1/2] 检查依赖...
if not exist "node_modules" (
    echo 安装 npm 依赖...
    call npm install
    if errorlevel 1 (
        echo npm 安装失败，请检查 Node.js 环境
        pause
        exit /b 1
    )
)

echo.
echo [2/2] 启动 Vite 开发服务器...
echo 前端将运行在 http://localhost:3000
call npm run dev

pause
