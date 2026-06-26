@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

:: ============================================================
:: 证言 Testimony.ai Web Demo 一键部署工具 (Windows)
:: AI深伪时代青少年霸凌事件可信存证系统
:: ============================================================

title 🛡️ 证言 Testimony.ai - 部署工具
color 0A

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║     🛡️  证言 Testimony.ai Web Demo 部署工具      ║
echo ║    AI深伪时代青少年霸凌事件可信存证系统         ║
echo ╚══════════════════════════════════════════════════╝
echo.

set SERVER=220.154.134.151
set PORT=9000
set USER=root
set PASSWORD=Testimony#2026Ops!
set DEPLOY_DIR=/opt/linxihaven_demo
set LOCAL_DIR=%~dp0web-demo

:: 检查文件
if not exist "%LOCAL_DIR%\index.html" (
    echo [错误] web-demo目录不存在！请确保在testimony根目录运行此脚本。
    pause
    exit /b 1
)

echo [✓] 找到Web Demo文件
echo.

:: 尝试检测可用的SSH工具
echo [信息] 正在检查SSH工具...
where plink >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set SSH_TOOL=plink
    set SSH_CMD=plink -ssh -pw %PASSWORD%
    echo [✓] 检测到 PuTTY/plink
) else (
    where ssh >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set SSH_TOOL=ssh
        set SSH_CMD=sshpass -p %PASSWORD% ssh -o StrictHostKeyChecking=no
        echo [✓] 检测到 OpenSSH/ssh
    ) else (
        goto :need_ssh
    )
)
echo.

:: 显示菜单
:menu
echo ============================================
echo   请选择操作:
echo ============================================
echo   [1] 连接测试 (ping服务器)
echo   [2] 上传并部署到ECS服务器
echo   [3) 仅上传文件到服务器
echo   [4] 在本地预览Web Demo
echo   [5] 查看部署状态
echo   [6] 重启服务器上的Web服务
echo   [7] 显示访问信息
echo   [0] 退出
echo ============================================
echo.

set /p choice="请输入选项 (0-7): "

if "%choice%"=="1" goto :ping_test
if "%choice%"=="2" goto :deploy
if "%choice%"=="3" goto :upload_only
if "%choice%"=="4" goto :local_preview
if "%choice%"=="5" goto :check_status
if "%choice%"=="6" goto :restart_service
if "%choice%"=="7" goto :show_info
if "%choice%"=="0" goto :eof
goto :menu

:ping_test
echo.
echo [信息] 测试服务器连通性...
ping -n 3 %SERVER%
echo.
pause
goto :menu

:upload_only
echo.
echo [信息] 开始上传文件...
call :do_upload
if %UPLOAD_SUCCESS%==1 (
    echo [成功] 文件已上传到服务器！
    echo        请选择 [2] 完成部署或 [6] 启动服务
) else (
    echo [失败] 上传出错，请检查网络连接
)
echo.
pause
goto :menu

:deploy
echo.
echo [========== 开始部署 =========]
echo.

:: Step 1: 上传文件
call :do_upload
if not %UPLOAD_SUCCESS%==1 goto :deploy_failed

:: Step 2: 远程执行部署命令
echo.
echo [步骤2] 在服务器上执行部署...

if "%SSH_TOOL%"=="plink" (
    %SSH_CMD% %USER%@%SERVER% "mkdir -p %DEPLOY_DIR% && cd %DEPLOY_DIR% && pkill -f 'python.*%PORT%' 2>/dev/null; nohup python3 -m http.server %PORT% > /var/log/testimony-web.log 2>&1 &"
) else if "%SSH_TOOL%"=="ssh" (
    sshpass -p %PASSWORD% ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "mkdir -p %DEPLOY_DIR% && cd %DEPLOY_DIR% && pkill -f 'python.*%PORT%' 2>/dev/null; nohup python3 -m http.server %PORT% > /var/log/testimony-web.log 2>&1 &"
)

echo.
echo [========== 部署完成 =========]
echo.
call :show_info
echo.
echo [提示] 如果服务未启动，可能需要在服务器上手动执行:
echo       cd %DEPLOY_DIR% && python3 -m http.server %PORT%
echo.
pause
goto :menu

:deploy_failed
echo [失败] 部署中断
pause
goto :menu

:local_preview
echo.
echo [信息] 在浏览器中打开本地预览...
start http://localhost:8000
cd /d "%LOCAL_DIR%"
python -m http.server 8000
goto :menu

:check_status
echo.
echo [信息] 检查服务器状态...
curl -s --connect-timeout 5 http://%SERVER%:%PORT%/ >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [成功] 服务器 http://%SERVER%:%PORT%/ 可正常访问！
) else (
    echo [警告] 服务器无响应，可能需要重新部署
)
echo.
pause
goto :menu

:restart_service
echo.
echo [信息] 重启Web服务...
if "%SSH_TOOL%"=="plink" (
    %SSH_CMD% %USER%@%SERVER% "pkill -f 'python.*%PORT%' 2>/dev/null; sleep 1; cd %DEPLOY_DIR% && nohup python3 -m http.server %PORT% > /var/log/testimony-web.log 2>&1 &"
) else if "%SSH_TOOL%"=="ssh" (
    sshpass -p %PASSWORD% ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "pkill -f 'python.*%PORT%' 2>/dev/null; sleep 1; cd %DEPLOY_DIR% && nohup python3 -m http.server %PORT% > /var/log/testimony-web.log 2>&1 &"
)
echo [完成] 服务重启命令已发送
timeout /t 3 >nul
call :check_status
pause
goto :menu

:show_info
echo.
echo ╔══════════════════════════════════════╗
echo ║          📱 访问信息                 ║
echo ╠══════════════════════════════════════╣
echo ║  URL: http://%SERVER%:%PORT%        ║
echo ║  PIN: 123456                        ║
echo ║  秘密手势: 点击左上角6次            ║
echo ╠══════════════════════════════════════╣
echo ║  功能清单:                         ║
echo ║  ✓ 伪装计算器（真实可用）           ║
echo ║  ✓ PIN码验证系统                   ║
echo ║  ✓ 安全空间模拟                    ║
echo ║  ✓ AI引导访谈引擎                  ║
echo ║  ✓ 家长端风险评估                  ║
echo ║  ✓ 证据包生成预览                  ║
echo ║  ✓ Merkle树可视化                  ║
echo ║  ✓ 司法采纳度报告                  ║
echo ╚══════════════════════════════════════╝
echo.
goto :eof

:need_ssh
echo.
echo [错误] 未找到SSH客户端！
echo.
echo 请安装以下任一工具后重试:
echo   1. PuTTY (推荐): https://www.chiark.greenend.org.uk/~sgtatham/putty/
echo   2. OpenSSH for Windows: Windows设置 → 应用 → 可选功能 → OpenSSH客户端
echo.
echo 或使用其他SSH工具(如XShell, MobaXterm, FinalShell等)手动执行:
echo.
echo   # 1. 上传文件:
echo   scp -r web-demo/* root@220.154.134.151:/opt/linxihaven_demo/
echo.
echo   # 2. SSH登录并启动服务:
echo   ssh root@220.154.134.151
echo   cd /opt/linxihaven_demo && nohup python3 -m http.server 9000 &
echo.
pause
exit /b 1

:do_upload
set UPLOAD_SUCCESS=0
echo [步骤1] 上传Web Demo文件到服务器...

if "%SSH_TOOL%"=="plink" (
    :: 使用pscp上传
    pscp -r -pw %PASSWORD% "%LOCAL_DIR%\*" %USER%@%SERVER%:%DEPLOY_DIR%\
    if %ERRORLEVEL% EQU 0 set UPLOAD_SUCCESS=1
) else if "%SSH_TOOL%"=="ssh" (
    :: 使用scp上传
    scp -r -o StrictHostKeyChecking=no "%LOCAL_DIR%\*" %USER%@%SERVER%:%DEPLOY_DIR%/
    if %ERRORLEVEL% EQU 0 set UPLOAD_SUCCESS=1
)
goto :eof

:eof
echo.
echo 感谢使用证言Testimony.ai部署工具！
echo.
pause