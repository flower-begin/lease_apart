@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo.
echo ==============================================================================
echo   移动端 H5 · 一键构建并分享
echo ==============================================================================
echo.

REM ---------- 1. 探测后端 ----------
echo [1/3] 检查后端 8081 ...
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/app/region/province/list' -UseBasicParsing -TimeoutSec 4; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>nul
if errorlevel 1 (
    echo       [警告] 8081 没有响应。
    echo              页面能打开，但房源 / 地区数据会是空的。
    echo              请先在另一个窗口启动 web-app。
    echo.
) else (
    echo       OK，后端在线
    echo.
)

REM ---------- 2. 构建 ----------
echo [2/3] 构建移动端 H5（mode=share，接口走同源相对路径）...
echo ------------------------------------------------------------------------------
pushd web-app-frontend
call npm run build -- --mode share
set BUILD_RESULT=%errorlevel%
popd
if not "%BUILD_RESULT%"=="0" (
    echo.
    echo   [失败] 构建出错，请看上面的报错。
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] 启动静态分享服务 ...
echo ------------------------------------------------------------------------------
echo   启动后另开一个窗口执行：hostc 8888
echo   如果提示端口占用，说明 npm run dev 正跑在 8888 上，先停掉它。
echo ------------------------------------------------------------------------------
echo.
node tools\share-static.mjs --dist web-app-frontend\dist --port 8888 --name "移动端 H5" %*

echo.
pause
endlocal
