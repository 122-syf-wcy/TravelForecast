@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo 旅游预测系统 - 本地开发启动脚本
echo ========================================
echo.

:: 检查是否安装了 Node.js
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [错误] Node.js 未安装或不在环境变量中
    pause
    exit /b 1
)

echo [✓] 检查到 Node.js 已安装
node --version

:: 设置环境变量
set APP_ENV=local
set APP_FRONTEND_URL=http://localhost:3000

echo.
echo [*] 环境配置:
echo     APP_ENV=%APP_ENV%
echo     APP_FRONTEND_URL=%APP_FRONTEND_URL%
echo.

:: 启动后端
echo [*] 正在启动后端服务 (http://localhost:8080) ...
cd /d "f:\网页系统开发\旅游预测后端"
if exist "target\travel-prediction-1.0.0.jar" (
    start "后端服务" java -jar target/travel-prediction-1.0.0.jar
    echo [✓] 后端启动命令已发送
    timeout /t 5 /nobreak
) else (
    echo [警告] JAR 文件不存在，尝试编译...
    call mvn clean package -DskipTests
    if !ERRORLEVEL! equ 0 (
        start "后端服务" java -jar target/travel-prediction-1.0.0.jar
        echo [✓] 后端启动命令已发送
        timeout /t 5 /nobreak
    ) else (
        echo [错误] 编译失败
        pause
        exit /b 1
    )
)

:: 启动前端
echo.
echo [*] 正在启动前端开发服务 (http://localhost:3000) ...
cd /d "f:\网页系统开发\旅游预测\web"
if exist "node_modules" (
    echo [✓] node_modules 已存在，直接启动...
) else (
    echo [*] 安装依赖中...
    call npm install
)

start "前端服务" npm run dev
echo [✓] 前端启动命令已发送

echo.
echo ========================================
echo [✓] 系统启动完成！
echo ========================================
echo.
echo 前端访问地址: http://localhost:3000
echo 后端 API 地址: http://localhost:8080/api
echo.
echo [提示] 两个服务窗口已在后台启动
echo [提示] 关闭任意一个服务窗口可停止对应的服务
echo [提示] 按任意键继续...
pause >nul
