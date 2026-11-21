@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo.
echo ======================================================================
echo   收尾：rentHouseH5 变成 web-app-frontend，并删除 AI 问答功能
echo ======================================================================
echo.
echo   当前目录: %CD%
echo.

if not exist "%~dp0finalize-web-app.ps1" (
    echo   [错误] 找不到 tools\finalize-web-app.ps1
    echo.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0finalize-web-app.ps1" %*
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 (
    echo ----------------------------------------------------------------------
    echo   执行失败（退出码 %RC%），把上面的错误信息发给助手
    echo ----------------------------------------------------------------------
) else (
    echo ----------------------------------------------------------------------
    echo   完成。记得重启 web-app 后端，再 cd web-app-frontend ^&^& npm run dev
    echo ----------------------------------------------------------------------
)
echo.
pause
endlocal
