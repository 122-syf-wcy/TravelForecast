@echo off
chcp 65001 >nul
echo ========================================
echo   智教黔行 - API网关服务启动脚本
echo   Travel Forecast Gateway
echo ========================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Java环境，请先安装JDK 17+
    pause
    exit /b 1
)

REM 检查Maven环境
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Maven环境，请先安装Maven 3.6+
    pause
    exit /b 1
)

echo [信息] 开始编译项目...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo [错误] 项目编译失败
    pause
    exit /b 1
)

echo.
echo [信息] 启动网关服务...
echo [提示] 网关地址: http://localhost:8888
echo [提示] 按 Ctrl+C 停止服务
echo.

java -jar target\travel-gateway-1.0.0.jar

pause

