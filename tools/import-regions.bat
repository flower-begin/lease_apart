@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

REM ======================= 数据库连接（按需修改） =======================
set DB_USER=root
set DB_PASS=123456
set DB_NAME=lease
REM =====================================================================

echo.
echo ==============================================================================
echo   导入全国省 / 市 / 区数据（幂等：可重复执行，不会重复插入）
echo ==============================================================================
echo.
echo   数据库 : %DB_NAME%
echo   账号   : %DB_USER%
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo   [错误] 找不到 node，请先安装 Node.js 并加入 PATH
    echo.
    pause
    exit /b 1
)

echo [1/2] 拉取行政区划数据并生成 SQL ...
echo ------------------------------------------------------------------------------
node "%~dp0import-regions.mjs"
if errorlevel 1 (
    echo.
    echo   生成失败，请看上面的错误信息
    echo.
    pause
    exit /b 1
)

echo.
where mysql >nul 2>nul
if errorlevel 1 (
    echo [2/2] 没找到 mysql 命令行客户端，跳过自动导入。
    echo.
    echo       请用 Navicat / DataGrip 打开下面这个文件整体执行：
    echo         sql\region-generated.sql
    echo.
    pause
    exit /b 0
)

echo [2/2] 正在写入数据库 ...
echo ------------------------------------------------------------------------------
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% < "%~dp0..\sql\region-generated.sql"
if errorlevel 1 (
    echo.
    echo   [错误] 导入失败。常见原因：
    echo     - 密码不对（改本文件里的 DB_PASS）
    echo     - 数据库名不是 lease（改 DB_NAME）
    echo     - 表还没建（先执行 sql\lease.sql）
    echo.
    pause
    exit /b 1
)

echo.
echo ==============================================================================
echo   导入完成，当前数据量：
echo ==============================================================================
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% -e "SELECT (SELECT COUNT(*) FROM province_info) AS 省, (SELECT COUNT(*) FROM city_info) AS 市, (SELECT COUNT(*) FROM district_info) AS 区县;"

echo.
echo   刷新前端页面，地区选择器就是全国数据了。
echo.
pause
endlocal
