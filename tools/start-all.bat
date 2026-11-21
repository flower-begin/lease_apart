@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo.
echo ======================================================================
echo   归心寓 一键启动（5 个窗口）
echo    8080 web-admin / 8081 web-app / 5174 后台界面 / 8888 用户端 H5
echo ======================================================================
echo.

if not exist "%~dp0start-all.ps1" (
    echo   [错误] 找不到 tools\start-all.ps1
    echo.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 (
    echo ----------------------------------------------------------------------
    echo   启动失败（退出码 %RC^)，请把上面的错误信息发给助手
    echo ----------------------------------------------------------------------
    echo.
    pause
)
endlocal
