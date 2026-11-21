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
echo   一键导入全国省 / 市 / 区（按顺序跑 3 个 SQL，全部幂等）
echo ==============================================================================
echo.
echo   数据库 : %DB_NAME%     账号 : %DB_USER%
echo   目录   : %CD%
echo.

if not exist "sql\region-full.sql" (
    echo   [错误] 找不到 sql\region-full.sql，请确认在 lease 根目录运行
    pause & exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
    echo   [错误] 找不到 mysql 命令行客户端。
    echo.
    echo   请改用 Navicat / DataGrip，**按顺序**执行下面三个文件（缺一不可）：
    echo     1. sql\region-full.sql          ^<-- 34 个省 + 337 个市 + 317 个区县
    echo     2. sql\region-districts-01.sql  ^<-- 江苏/浙江/山东/广东 的区县
    echo     3. sql\region-districts-02.sql  ^<-- 其余 23 个省级的区县
    echo.
    pause
    exit /b 1
)

echo ------------------------------------------------------------------------------
echo [1/3] region-full.sql   （省 + 市 + 主要区县）
echo ------------------------------------------------------------------------------
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% < "sql\region-full.sql"
if errorlevel 1 (
    echo.
    echo   [失败] region-full.sql 执行出错，已中止。把上面的报错发给助手。
    pause & exit /b 1
)

echo.
echo ------------------------------------------------------------------------------
echo [2/3] region-districts-01.sql   （江苏 / 浙江 / 山东 / 广东）
echo ------------------------------------------------------------------------------
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% < "sql\region-districts-01.sql"
if errorlevel 1 (
    echo.
    echo   [失败] region-districts-01.sql 执行出错，已中止。
    pause & exit /b 1
)

echo.
echo ------------------------------------------------------------------------------
echo [3/3] region-districts-02.sql   （其余 23 个省级行政区）
echo ------------------------------------------------------------------------------
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% < "sql\region-districts-02.sql"
if errorlevel 1 (
    echo.
    echo [失败] region-districts-02.sql 执行出错，已中止。
    pause & exit /b 1
)

echo.
echo ==============================================================================
echo   全部执行完成，最终数据量：
echo ==============================================================================
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% -e "SELECT (SELECT COUNT(*) FROM province_info) AS 省, (SELECT COUNT(*) FROM city_info) AS 市, (SELECT COUNT(*) FROM district_info) AS 区县;"

echo.
echo   期望：省 34 / 市 337 / 区县 约 2880
echo   然后刷新前端页面（地区数据是页面加载时拉的）。
echo.
pause
endlocal
