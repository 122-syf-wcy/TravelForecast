@echo off
chcp 65001
echo ================================
echo   启动旅游预测服务
echo ================================
echo.

REM 设置变量
set APP_NAME=travel-prediction
set JAR_NAME=travel-prediction-0.0.1-SNAPSHOT.jar
set PROFILE=prod
set LOG_PATH=C:\logs

REM 创建日志目录
if not exist "%LOG_PATH%" mkdir "%LOG_PATH%"

REM 检查是否已经运行
echo [1/3] 检查服务状态...
netstat -ano | findstr :8080 > nul
if %ERRORLEVEL% EQU 0 (
    echo ⚠️  端口8080已被占用，正在停止旧服务...
    call stop.bat
    timeout /t 3 /nobreak > nul
)

REM 检查jar包
echo [2/3] 检查jar包...
if not exist "target\%JAR_NAME%" (
    echo ❌ 找不到jar包: target\%JAR_NAME%
    pause
    exit /b 1
)

REM 启动服务
echo [3/3] 启动服务...
echo.
echo 应用名称: %APP_NAME%
echo 配置环境: %PROFILE%
echo 日志路径: %LOG_PATH%\%APP_NAME%.log
echo.

start "Travel Prediction Service" javaw ^
  -Xms512m ^
  -Xmx1024m ^
  -Dspring.profiles.active=%PROFILE% ^
  -Dfile.encoding=UTF-8 ^
  -Djava.awt.headless=true ^
  -jar target\%JAR_NAME% ^
  > %LOG_PATH%\%APP_NAME%.log 2>&1

REM 等待启动
echo 正在启动服务，请稍候...
timeout /t 5 /nobreak > nul

REM 检查启动状态
netstat -ano | findstr :8080 > nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 服务启动成功！
    echo.
    echo 🌐 API地址: http://localhost:8080/api
    echo 📚 Swagger文档: http://localhost:8080/api/doc.html
    echo 📝 查看日志: %LOG_PATH%\%APP_NAME%.log
    echo.
    echo 按任意键查看实时日志...
    pause > nul
    powershell -Command "Get-Content '%LOG_PATH%\%APP_NAME%.log' -Wait"
) else (
    echo.
    echo ❌ 服务启动失败！
    echo 请检查日志: %LOG_PATH%\%APP_NAME%.log
    echo.
    pause
)

