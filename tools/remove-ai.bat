@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
echo.
echo ======================================================================
echo   删除后端 AI 问答代码（8 个 Java 文件）
echo ======================================================================
echo.
if not exist "%~dp0remove-ai.ps1" (
    echo   [错误] 找不到 tools\remove-ai.ps1
    echo.
    pause
    exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0remove-ai.ps1"
set RC=%ERRORLEVEL%
echo.
if %RC% neq 0 (
    echo   失败（退出码 %RC%）
) else (
    echo   完成。请重启 web-app 后端。
)
echo.
pause
endlocal
