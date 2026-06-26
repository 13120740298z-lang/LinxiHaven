@echo off
chcp 65001 >nul
title 证言 Testimony.ai - 决赛路演版
echo.
echo ╔════════════════════════════════════════════╗
echo ║     证言 Testimony.ai - 决赛路演版          ║
echo ║   上海市青少年科技创新大赛 · 决赛项目       ║
echo ╚════════════════════════════════════════════╝
echo.

:: 检查 Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Python，请先安装 Python 3.8+
    pause
    exit /b 1
)

:: 设置端口
set PORT=9000

:: 启动服务
echo [*] 正式启动 Web 服务...
echo [*] 监听地址: http://0.0.0.0:%PORT%
echo [*] 按 Ctrl+C 停止服务
echo.
python server.py %PORT%

pause
