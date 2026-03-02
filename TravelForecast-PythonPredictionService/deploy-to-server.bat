@echo off
chcp 65001
echo ================================
echo   部署Python预测服务到服务器
echo ================================
echo.

REM 步骤1: 检查依赖文件
echo [步骤 1/4] 检查文件...
if not exist "main.py" (
    echo ❌ main.py不存在！
    pause
    exit /b 1
)
if not exist "requirements.txt" (
    echo ❌ requirements.txt不存在！
    pause
    exit /b 1
)
echo ✅ 文件检查通过

REM 步骤2: 打包文件
echo.
echo [步骤 2/4] 打包文件...
if not exist "deploy-temp" mkdir deploy-temp
xcopy /Y /E /I "*.py" "deploy-temp\" > nul 2>&1
copy /Y "requirements.txt" "deploy-temp\" > nul
copy /Y "config.env" "deploy-temp\" > nul
xcopy /Y /E /I "models" "deploy-temp\models\" > nul
if exist "data" xcopy /Y /E /I "data" "deploy-temp\data\" > nul
echo ✅ 文件已打包到 deploy-temp 目录

REM 步骤3: 上传说明
echo.
echo [步骤 3/4] 上传到服务器
echo.
echo 请将 deploy-temp 文件夹上传到服务器:
echo   目标路径: C:\projects\旅游预测-Python预测服务\
echo.
echo 方式1: 远程桌面
echo   - 连接到: 8.148.180.175
echo   - 复制粘贴 deploy-temp 文件夹
echo.
echo 方式2: WinSCP/FileZilla
echo   - 使用FTP客户端上传
echo.
pause
echo.

REM 步骤4: 服务器配置说明
echo [步骤 4/4] 服务器上的操作
echo.
echo ========================================
echo 在服务器上执行以下命令:
echo ========================================
echo.
echo 1. 安装Python依赖:
echo    cd C:\projects\旅游预测-Python预测服务
echo    pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
echo.
echo 2. 配置 config.env:
echo    修改数据库连接信息:
echo      DB_HOST=localhost
echo      DB_USER=travel_user
echo      DB_PASSWORD=你的密码
echo      DB_NAME=travel_prediction
echo.
echo 3. 启动服务:
echo    python main.py
echo.
echo 4. 验证服务:
echo    访问: http://localhost:8001/docs
echo.
echo 5. 后台运行（可选）:
echo    创建 start-python-service.bat:
echo      @echo off
echo      start "Python Prediction" pythonw main.py
echo.
echo 6. 配置防火墙（如果需要外网访问）:
echo    netsh advfirewall firewall add rule name="Python Prediction" dir=in action=allow protocol=TCP localport=8001
echo.
echo ========================================
echo.
echo ✅ Python服务部署准备完成！
echo.
pause

