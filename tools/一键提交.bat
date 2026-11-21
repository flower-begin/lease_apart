@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo.
echo ==============================================================================
echo   一键：删 AI 代码  ->  提交  ->  推送  ->  校验「远程 == 本地」
echo ==============================================================================
echo.
echo   仓库: %CD%
echo.

if not exist "%~dp0push-to-github.ps1" (
    echo   [错误] 找不到 tools\push-to-github.ps1，请确认本文件在 lease\tools\ 目录下
    echo.
    pause
    exit /b 1
)

echo [1/3] 删除后端 AI 问答代码（8 个 Java 文件）...
echo ------------------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0remove-ai.ps1"
if errorlevel 1 (
    echo   [警告] 删 AI 代码没成功，但继续提交（不影响后端运行）
)
echo.

echo [2/3] 提交并推送到 GitHub...
echo ------------------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0push-to-github.ps1" -AlsoUpdateMaster -Message "feat: 用户端换成官方 H5，后台按 rentHouseAdmin 重做，补省市区树接口，移除 AI 问答"
set RC=%ERRORLEVEL%
echo.

echo [3/3] 结果
echo ------------------------------------------------------------------------------
if %RC% neq 0 (
    echo.
    echo   ^>^>^> 失败（退出码 %RC%）
    echo.
    echo   最常见的原因：SSH key 没配好。
    echo   先在另一个窗口跑一下这条命令自测：
    echo.
    echo       ssh -T git@github.com
    echo.
    echo   如果提示 "Permission denied (publickey)"，就是 key 的问题。
    echo   想改用 HTTPS 的话，在项目根目录执行：
    echo.
    echo       git remote set-url origin1 https://github.com/flower-begin/lease_apart.git
    echo.
    echo   然后把上面的报错整段发给助手。
) else (
    echo.
    echo   ^>^>^> 成功：已提交、已推送、远程与本地一致
    echo.
    echo   仓库地址：https://github.com/flower-begin/lease_apart
    echo   下一步（想要在线网址的话）：
    echo     1. GitHub -^> Settings -^> Pages -^> Source 选 "GitHub Actions"
    echo     2. Settings -^> Secrets and variables -^> Actions -^> Variables
    echo        新建 VITE_APP_BASE_URL = 你的公网后端地址
    echo)
echo.
pause
endlocal
