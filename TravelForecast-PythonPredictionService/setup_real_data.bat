@echo off
chcp 65001 >nul
echo ==========================================
echo  客流预测系统 - 真实数据初始化
echo ==========================================
echo.

echo [步骤1/3] 创建数据库表结构...
echo.
echo 请在MySQL中执行以下SQL文件:
echo F:\网页系统开发\数据库表\flow_records.sql
echo.
echo 你可以使用以下命令:
echo mysql -uroot -p travel ^< F:\网页系统开发\数据库表\flow_records.sql
echo.
pause

echo.
echo [步骤2/3] 生成高质量客流数据...
echo 这将生成过去90天的数据，可能需要几分钟...
echo.
python data_generator.py

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 错误: 数据生成失败！
    echo 请检查:
    echo 1. MySQL数据库是否已启动
    echo 2. 数据库配置是否正确 ^(db_connector.py^)
    echo 3. 依赖包是否已安装 ^(pymysql^)
    pause
    exit /b 1
)

echo.
echo [步骤3/3] 数据初始化完成！
echo.
echo 接下来启动Python预测服务...
echo.
pause

echo.
echo 启动预测服务...
python main.py

pause

