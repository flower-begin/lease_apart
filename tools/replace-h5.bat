@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo.
echo ======================================================================
echo   把官方 rentHouseH5 复制进 lease 目录
echo ======================================================================
echo.
echo   当前目录: %CD%
echo.

if not exist "%~dp0replace-app-ui.ps1" (
    echo   [错误] 找不到 tools\replace-app-ui.ps1
    echo          请确认本文件在 lease\tools\ 目录下
    echo.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0replace-app-ui.ps1" %*
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 (
    echo ----------------------------------------------------------------------
    echo   执行失败（退出码 %RC%），请把上面的红色错误信息发给助手
    echo ----------------------------------------------------------------------
) else (
    echo ----------------------------------------------------------------------
    echo   完成。接下来：
    echo     1^) 确保 web-app 8081 已启动
    echo     2^) cd rentHouseH5  ^&^&  npm run dev
    echo     3^) 浏览器打开 http://localhost:8888
    echo ----------------------------------------------------------------------
)
echo.
pause
endlocal
