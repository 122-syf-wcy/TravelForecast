@echo off
chcp 65001
echo ================================
echo   重启旅游预测服务
echo ================================
echo.

call stop.bat
timeout /t 3 /nobreak > nul
call start.bat

