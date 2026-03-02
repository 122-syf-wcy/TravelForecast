@echo off
chcp 65001
echo ================================
echo   启动Python预测服务
echo ================================
echo.

cd /d %~dp0

REM 检查main.py是否存在
if not exist "main.py" (
    echo ❌ 找不到main.py文件
    echo 请确保在正确的目录下运行此脚本
    pause
    exit /b 1
)

REM 检查是否已经运行
echo [1/3] 检查服务状态...
netstat -ano | findstr :8001 > nul
if %ERRORLEVEL% EQU 0 (
    echo ⚠️  端口8001已被占用
    echo.
    choice /C YN /M "是否停止旧服务并重启"
    if errorlevel 2 goto :end
    if errorlevel 1 (
        echo 正在停止旧服务...
        for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8001 ^| findstr LISTENING') do taskkill /F /PID %%a > nul 2>&1
        timeout /t 2 /nobreak > nul
    )
)

REM 检查Python环境
echo [2/3] 检查Python环境...
python --version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Python未安装或未配置到PATH
    echo 请安装Python 3.8+
    pause
    exit /b 1
)
echo ✅ Python环境正常

REM 检查依赖包
echo 检查依赖包...
pip show fastapi > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  依赖包未安装
    echo.
    choice /C YN /M "是否现在安装依赖包"
    if errorlevel 2 goto :skipinstall
    if errorlevel 1 (
        echo 正在安装依赖包...
        pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
    )
)
:skipinstall

REM 启动服务
echo.
echo [3/3] 启动服务...
echo.
echo 服务信息:
echo   名称: 六盘水景区客流预测服务
echo   端口: 8001
echo   文档: http://localhost:8001/docs
echo.

REM 方式1: 前台运行(可以看到日志)
python main.py

REM 方式2: 后台运行(取消上面的注释,使用下面的命令)
REM start "Python Prediction Service" pythonw main.py
REM echo.
REM echo ✅ 服务已在后台启动
REM echo.
REM echo 📚 API文档: http://localhost:8001/docs
REM echo 📝 查看日志: logs\prediction_service_*.log
REM echo.

:end
pause

