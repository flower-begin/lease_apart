@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo.
echo ======================================================================
echo   提交并推送到 GitHub（最后会校验「远程 == 本地」）
echo ======================================================================
echo.

if not exist "%~dp0push-to-github.ps1" (
    echo   [错误] 找不到 tools\push-to-github.ps1
    echo.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0push-to-github.ps1" %*
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 (
    echo ----------------------------------------------------------------------
    echo   失败（退出码 %RC%），把上面的错误信息发给助手
    echo ----------------------------------------------------------------------
) else (
    echo ----------------------------------------------------------------------
    echo   完成。GitHub Pages 部署见 .github\workflows\deploy-h5.yml
    echo   需要先在 Settings ^> Pages 把 Source 设成 "GitHub Actions"
    echo ----------------------------------------------------------------------
)
echo.
pause
endlocal
