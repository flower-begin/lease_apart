# =============================================================================
#  尚庭公寓（lease）一键启动
#
#  会打开 4 个 PowerShell 窗口：
#    8080  web-admin          后台管理接口
#    8081  web-app            用户端接口
#    5174  web-admin-frontend 后台管理前端
#    8888  rentHouseH5        用户端 H5（官方版，优先使用）
#
#  用户端 H5 的查找顺序（可用 -AppUiPath 强制指定）：
#    ① 本项目下的 rentHouseH5\        ← 还没跑 finalize-web-app 时的位置
#    ② D:\尚庭公寓\09_尚庭公寓\...\rentHouseH5   （原始位置）
#    ③ 本项目下的 web-app-frontend\   ← 跑完 finalize-web-app 后的最终位置（官方 H5）
#
#  用法（在项目根目录执行）：
#      .\tools\start-all.ps1
#
#  可选参数：
#      -SkipBuild        跳过 mvn install（依赖已装过时用，启动更快）
#      -SkipInstall      前端不自动 npm install
#      -AdminUiPath  "D:\xxx\rentHouseAdmin"   手动指定后台前端目录
#      -AppUiPath    "D:\xxx\rentHouseH5"      手动指定用户端 H5 目录
# =============================================================================

param(
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [string]$AdminUiPath,
    [string]$AppUiPath
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
    $root = (Get-Location).Path
}

Write-Host ""
Write-Host "项目根目录: $root" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 1. 环境检查
# -----------------------------------------------------------------------------
foreach ($cmd in @('java', 'mvn', 'npm')) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Host "[错误] 找不到 $cmd，请先安装并加入 PATH" -ForegroundColor Red
        exit 1
    }
}
Write-Host "[1/5] 环境检查通过（java / mvn / npm 都在）" -ForegroundColor Green

# -----------------------------------------------------------------------------
# 2. 定位后台管理前端（官方 rentHouseAdmin）
# -----------------------------------------------------------------------------
function Resolve-AdminUi {
    param([string]$Explicit)

    $candidates = @()
    if ($Explicit) { $candidates += $Explicit }

    # ① 本项目内的后台前端（已按 rentHouseAdmin 风格重做，推荐）
    $candidates += (Join-Path $root 'web-admin-frontend')
    # ② 官方 rentHouseAdmin（已配好 localhost:8080 的那份）
    $candidates += 'D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin'
    # ③ 官方 rentHouseAdmin 课程完成版源码（需改 .env.development）
    $candidates += 'D:\尚硅谷java笔记\前端\07_尚庭公寓前端实战\07_尚庭公寓前端实战\代码\完成版代码\rentHouseAdmin'
    # ④ 如果你把它拷进了项目里
    $candidates += (Join-Path $root 'rentHouseAdmin')

    foreach ($path in $candidates) {
        if ($path -and (Test-Path (Join-Path $path 'package.json'))) {
            return (Resolve-Path $path).Path
        }
    }
    return $null
}

$adminUi = Resolve-AdminUi -Explicit $AdminUiPath
if (-not $adminUi) {
    Write-Host "[错误] 找不到后台管理前端目录（rentHouseAdmin）。" -ForegroundColor Red
    Write-Host "       请用 -AdminUiPath 指定，例如：" -ForegroundColor Yellow
    Write-Host '       .\tools\start-all.ps1 -AdminUiPath "D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin"' -ForegroundColor Yellow
    exit 1
}
Write-Host "[2/5] 后台管理前端: $adminUi" -ForegroundColor Green

# 检查它代理到哪个后端
$envFile = Join-Path $adminUi '.env.development'
if (Test-Path $envFile) {
    $baseUrlLine = Select-String -Path $envFile -Pattern 'VITE_APP_BASE_URL' | Select-Object -First 1
    if ($baseUrlLine) {
        $baseUrl = ($baseUrlLine.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
        if ($baseUrl -match 'localhost|127\.0\.0\.1') {
            Write-Host "      后端代理: $baseUrl （正确）" -ForegroundColor Green
        }
        else {
            Write-Host ""
            Write-Host "      [注意] 这份 .env.development 指向的是 $baseUrl ，不是本地后端！" -ForegroundColor Yellow
            Write-Host "             把该文件里的 VITE_APP_BASE_URL 改成：'http://localhost:8080'" -ForegroundColor Yellow
            Write-Host "             （推荐直接用 D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin，它已经配好了）" -ForegroundColor Yellow
            Write-Host ""
        }
    }
}
if (-not (Test-Path (Join-Path $adminUi 'node_modules'))) {
    Write-Host "      缺少 node_modules，稍后会自动 npm install" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# 2b. 定位用户端 H5
# -----------------------------------------------------------------------------
function Resolve-AppUi {
    param([string]$Explicit)

    $candidates = @()
    if ($Explicit) { $candidates += $Explicit }

    # ① 还没跑 finalize-web-app 时的位置
    $candidates += (Join-Path $root 'rentHouseH5')
    # ② 官方 rentHouseH5 原位置（页面更全，已配好 localhost:8081）
    $candidates += 'D:\尚庭公寓\09_尚庭公寓\09_尚庭公寓\代码\代码\前端\rentHouseH5'
    # ③ 跑完 finalize-web-app 后的最终位置（官方 H5 就住在这里）
    $candidates += (Join-Path $root 'web-app-frontend')

    foreach ($path in $candidates) {
        if ($path -and (Test-Path (Join-Path $path 'package.json'))) {
            return (Resolve-Path $path).Path
        }
    }
    return $null
}

$appUi = Resolve-AppUi -Explicit $AppUiPath
if (-not $appUi) {
    Write-Host "[错误] 找不到用户端 H5 目录。" -ForegroundColor Red
    Write-Host '       请用 -AppUiPath 指定，例如：' -ForegroundColor Yellow
    Write-Host '       .\tools\start-all.ps1 -AppUiPath "D:\尚庭公寓\09_尚庭公寓\09_尚庭公寓\代码\代码\前端\rentHouseH5"' -ForegroundColor Yellow
    exit 1
}
Write-Host "      用户端 H5: $appUi" -ForegroundColor Green

# 用户端端口 & 后端地址
$appPort = 5173
$appEnvFile = Join-Path $appUi '.env.development'
if (Test-Path $appEnvFile) {
    $portLine = Select-String -Path $appEnvFile -Pattern 'VITE_APP_PORT' | Select-Object -First 1
    if ($portLine) {
        $portRaw = ($portLine.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
        if ($portRaw -match '^\d+$') { $appPort = [int]$portRaw }
    }
    $appBaseLine = Select-String -Path $appEnvFile -Pattern 'VITE_APP_BASE_URL' | Select-Object -First 1
    if ($appBaseLine) {
        $appBase = ($appBaseLine.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
        if ($appBase -match 'localhost|127\.0\.0\.1') {
            Write-Host "      后端代理: $appBase ，端口 $appPort" -ForegroundColor Green
        }
        else {
            Write-Host "      [注意] 这份 .env.development 指向 $appBase ，不是本地后端！应改成 'http://localhost:8081'" -ForegroundColor Yellow
        }
    }
}
if (-not (Test-Path (Join-Path $appUi 'node_modules'))) {
    Write-Host "      缺少 node_modules，稍后会自动 npm install" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# 3. 后端依赖安装（model / common 必须先 install，否则单独 spring-boot:run 解析不到）
# -----------------------------------------------------------------------------
$modelJar = Join-Path $root 'model\target\model-1.0-SNAPSHOT.jar'
$commonJar = Join-Path $root 'common\target\common-1.0-SNAPSHOT.jar'

if ($SkipBuild) {
    Write-Host "[3/5] 跳过 mvn install" -ForegroundColor Yellow
}
elseif ((Test-Path $modelJar) -and (Test-Path $commonJar)) {
    Write-Host "[3/5] model/common 已构建过，跳过 install（要强制重建请删掉 target 目录）" -ForegroundColor Green
}
else {
    Write-Host "[3/5] 首次启动，执行 mvn -DskipTests install（1~3 分钟）..." -ForegroundColor Cyan
    Push-Location $root
    try {
        & mvn -DskipTests -Dmaven.test.skip=true install
        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "[错误] 后端构建失败，请把上面的报错贴出来" -ForegroundColor Red
            exit 1
        }
    }
    finally { Pop-Location }
    Write-Host "      后端构建完成" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 4. 前端依赖
# -----------------------------------------------------------------------------
if (-not $SkipInstall) {
    $needInstall = @()
    if (-not (Test-Path (Join-Path $adminUi 'node_modules'))) { $needInstall += $adminUi }
    if (-not (Test-Path (Join-Path $appUi 'node_modules'))) { $needInstall += $appUi }

    foreach ($fe in $needInstall) {
        Write-Host "[4/5] $fe 缺少依赖，执行 npm install ..." -ForegroundColor Cyan
        Push-Location $fe
        try {
            & npm install --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) { Write-Host "[错误] $fe npm install 失败" -ForegroundColor Red; exit 1 }
        }
        finally { Pop-Location }
    }
    Write-Host "[4/5] 前端依赖就绪" -ForegroundColor Green
}
else {
    Write-Host "[4/5] 跳过 npm install" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# 5. 起四个窗口
# -----------------------------------------------------------------------------
function Start-ServiceWindow {
    param([string]$Title, [string]$WorkDir, [string]$Command)
    $inner = "`$host.UI.RawUI.WindowTitle = '$Title'; Set-Location '$WorkDir'; Write-Host '$Title' -ForegroundColor Cyan; $Command"
    Start-Process pwsh -ArgumentList '-NoExit', '-NoProfile', '-Command', $inner | Out-Null
    Start-Sleep -Milliseconds 400
    Write-Host "      已启动 $Title" -ForegroundColor Green
}

Write-Host "[5/5] 启动四个服务..." -ForegroundColor Cyan
Write-Host ""

Start-ServiceWindow -Title 'lease :: web-admin  (8080)' `
    -WorkDir (Join-Path $root 'web\web-admin') -Command 'mvn spring-boot:run'

Start-ServiceWindow -Title 'lease :: web-app    (8081)' `
    -WorkDir (Join-Path $root 'web\web-app') -Command 'mvn spring-boot:run'

Start-ServiceWindow -Title 'lease :: admin-ui   (5174)' `
    -WorkDir $adminUi -Command 'npm run dev'

Start-ServiceWindow -Title "lease :: app-ui     ($appPort)" `
    -WorkDir $appUi -Command 'npm run dev'

# -----------------------------------------------------------------------------
# 摘要
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host "  等服务起来后访问：" -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host ""
Write-Host "  后台管理   http://localhost:5174    账号 admin / 123456" -ForegroundColor White
Write-Host "  用户端 H5  http://localhost:$appPort    验证码看 8081 控制台，或用 123456" -ForegroundColor White
Write-Host ""
Write-Host "  接口文档   http://localhost:8080/doc.html   （后台）" -ForegroundColor Gray
Write-Host "             http://localhost:8081/doc.html   （用户端）" -ForegroundColor Gray
Write-Host ""
Write-Host "  Spring Boot 首次启动 20~60 秒；vite 3~5 秒。" -ForegroundColor DarkGray
Write-Host "  想强行指定用户端 H5 目录：" -ForegroundColor DarkGray
Write-Host '    .\tools\start-all.ps1 -AppUiPath "D:\尚庭公寓\09_尚庭公寓\09_尚庭公寓\代码\代码\前端\rentHouseH5"' -ForegroundColor DarkGray
Write-Host ""
