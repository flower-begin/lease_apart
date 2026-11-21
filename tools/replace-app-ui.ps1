# =============================================================================
#  用官方 rentHouseH5 替换自建的 web-app-frontend
#
#  ⚠️ 说明：这个脚本只负责"把文件复制进 lease"。
#     复制完之后请再跑一次 tools\finalize-web-app.ps1，它会：
#       - 把 rentHouseH5 重命名为 web-app-frontend（你最终想要的名字）
#       - 删除旧的 web-app-frontend
#       - 删除后端 AI 问答代码
#     如果你已经手动把 rentHouseH5 复制进来了，直接跑 finalize 即可，不用跑本脚本。
#
#  它会做两件事：
#    1. 把官方 H5（含 node_modules，如果源目录里有）复制到本项目根目录下的 rentHouseH5\
#    2. 打印后续启动方式
#
#  为什么是脚本而不是我直接改：当前会话的执行沙箱无法启动任何子进程，
#  而 rentHouseH5 有两万多个文件（含二进制资源），只能由你在本机执行复制。
#
#  用法（在项目根目录）：
#      .\tools\replace-app-ui.ps1
#
#  常用参数：
#      -SkipNodeModules   只复制源码，之后自己 npm install（体积小、跨盘更快）
#      -Target "D:\xxx"   复制到别的位置（必须在 lease 目录内）
#      -Force             目标已存在时仍然执行（合并覆盖，不会删除多余文件）
#      -RemoveOld         复制成功后删掉旧的 web-app-frontend（真正完成"替换"）
# =============================================================================

param(
    [string]$Source = 'D:\尚庭公寓\09_尚庭公寓\09_尚庭公寓\代码\代码\前端\rentHouseH5',
    [string]$Target,
    [switch]$SkipNodeModules,
    [switch]$Force,
    [switch]$RemoveOld
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
    $root = (Get-Location).Path
}

if (-not $Target) {
    $Target = Join-Path $root 'rentHouseH5'
}

Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host "  用官方 rentHouseH5 替换自建用户端 H5" -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host ""
Write-Host "  源目录 : $Source" -ForegroundColor Gray
Write-Host "  目标   : $Target" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 0. 目标必须在 lease 项目目录内（你要求"一定要在 lease 文件夹里面包含"）
# -----------------------------------------------------------------------------
$rootFull = (Resolve-Path $root).Path.TrimEnd('\')
$targetFull = [System.IO.Path]::GetFullPath($Target).TrimEnd('\')
if (-not $targetFull.StartsWith($rootFull + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Host "[错误] 目标目录不在 lease 项目内：" -ForegroundColor Red
    Write-Host "       lease 根目录 : $rootFull" -ForegroundColor Red
    Write-Host "       目标         : $targetFull" -ForegroundColor Red
    Write-Host '       请把 -Target 指定成 lease 下的子目录（默认就是 lease\rentHouseH5）' -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✓ 目标在 lease 目录内" -ForegroundColor Green
Write-Host ""

# -----------------------------------------------------------------------------
# 1. 检查源目录
# -----------------------------------------------------------------------------
if (-not (Test-Path $Source)) {
    Write-Host "[错误] 源目录不存在：$Source" -ForegroundColor Red
    Write-Host '       请用 -Source 指定正确路径' -ForegroundColor Yellow
    exit 1
}
if (-not (Test-Path (Join-Path $Source 'package.json'))) {
    Write-Host "[错误] 源目录里没有 package.json，看起来不是 rentHouseH5 项目" -ForegroundColor Red
    exit 1
}

# 确认是那个 H5 项目（而不是后台）
$pkg = Get-Content (Join-Path $Source 'package.json') -Raw | ConvertFrom-Json
if ($pkg.name -ne 'vue3-h5-template') {
    Write-Host "[提醒] 源项目名是 '$($pkg.name)'，不是预期的 'vue3-h5-template'，请确认路径是否正确" -ForegroundColor Yellow
    $answer = Read-Host "     继续吗？(y/N)"
    if ($answer -ne 'y' -and $answer -ne 'Y') { exit 1 }
}

$sourceHasNM = Test-Path (Join-Path $Source 'node_modules')
Write-Host "  源目录 node_modules: $(if ($sourceHasNM) { '有' } else { '无' })" -ForegroundColor Gray

# -----------------------------------------------------------------------------
# 2. 目标目录已存在时确认
# -----------------------------------------------------------------------------
if ((Test-Path $Target) -and -not $Force) {
    Write-Host "[错误] 目标目录已存在：$Target" -ForegroundColor Red
    Write-Host '       加 -Force 覆盖（合并复制），或删掉它再重跑' -ForegroundColor Yellow
    exit 1
}

# -----------------------------------------------------------------------------
# 3. 复制（robocopy 比 Copy-Item 快且能排除大目录）
# -----------------------------------------------------------------------------
if (-not (Get-Command robocopy -ErrorAction SilentlyContinue)) {
    Write-Host "[错误] 找不到 robocopy（Windows 自带，应在 C:\Windows\System32）" -ForegroundColor Red
    exit 1
}

$rcArgs = @($Source, $Target, '/E', '/NFL', '/NDL', '/NJH', '/NJS', '/NP', '/R:1', '/W:1')
if ($SkipNodeModules -or -not $sourceHasNM) {
    $rcArgs += @('/XD', 'node_modules', '.git')
    Write-Host "[1/3] 仅复制源码（排除 node_modules / .git）..." -ForegroundColor Cyan
}
else {
    $rcArgs += @('/XD', '.git')
    Write-Host "[1/3] 复制全部内容（含 node_modules）..." -ForegroundColor Cyan
}

& robocopy @rcArgs | Out-Null
$rc = $LASTEXITCODE
# robocopy: 0=无变化 1=已复制 2=多余文件 3=1+2 ... 8 以上才是真失败
if ($rc -ge 8) {
    Write-Host "[错误] 复制失败（robocopy 退出码 $rc）" -ForegroundColor Red
    exit 1
}
Write-Host "      复制完成" -ForegroundColor Green

# -----------------------------------------------------------------------------
# 4. 检查依赖
# -----------------------------------------------------------------------------
$targetHasNM = Test-Path (Join-Path $Target 'node_modules')
if (-not $targetHasNM) {
    Write-Host "[2/3] 目标目录没有 node_modules，执行 npm install ..." -ForegroundColor Cyan
    Push-Location $Target
    try {
        & npm install --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[错误] npm install 失败" -ForegroundColor Red
            Pop-Location
            exit 1
        }
    }
    finally { Pop-Location }
    Write-Host "      依赖安装完成" -ForegroundColor Green
}
else {
    Write-Host "[2/3] node_modules 已就位" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 5. 校验后端地址
# -----------------------------------------------------------------------------
$envFile = Join-Path $Target '.env.development'
$port = 8888
$baseUrl = '(未配置)'
if (Test-Path $envFile) {
    $portLine = Select-String -Path $envFile -Pattern 'VITE_APP_PORT' | Select-Object -First 1
    if ($portLine) {
        $portRaw = ($portLine.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
        if ($portRaw -match '^\d+$') { $port = [int]$portRaw }
    }
    $baseLine = Select-String -Path $envFile -Pattern 'VITE_APP_BASE_URL' | Select-Object -First 1
    if ($baseLine) {
        $baseUrl = ($baseLine.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
    }
}

Write-Host "[3/3] 配置检查" -ForegroundColor Cyan
Write-Host "      端口        : $port"
Write-Host "      后端地址    : $baseUrl"
if ($baseUrl -notmatch 'localhost|127\.0\.0\.1') {
    Write-Host ""
    Write-Host "      [警告] 后端地址不是本机！请把 .env.development 里的 VITE_APP_BASE_URL 改成 'http://localhost:8081'" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# 4. 可选：删掉旧的 web-app-frontend，真正完成"替换"
# -----------------------------------------------------------------------------
$oldAppUi = Join-Path $root 'web-app-frontend'
if ($RemoveOld) {
    if (Test-Path $oldAppUi) {
        Write-Host ""
        Write-Host "  -RemoveOld 指定，正在删除旧的 web-app-frontend ..." -ForegroundColor Cyan
        Remove-Item -Recurse -Force $oldAppUi
        Write-Host "      已删除：$oldAppUi" -ForegroundColor Green
    }
    else {
        Write-Host "  （旧的 web-app-frontend 不存在，跳过删除）" -ForegroundColor DarkGray
    }
}

# -----------------------------------------------------------------------------
# 完成
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host "  替换完成 🎉" -ForegroundColor Green
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host ""
Write-Host "  官方 H5 已就位：$Target" -ForegroundColor White

# 打印一下关键结构，确认真的落在 lease 里
Write-Host ""
Write-Host "  lease 目录下的前端：" -ForegroundColor Gray
foreach ($dir in @('rentHouseH5', 'web-admin-frontend', 'web-app-frontend')) {
    $full = Join-Path $root $dir
    if (Test-Path $full) {
        $marker = if (Test-Path (Join-Path $full 'package.json')) { '✓' } else { ' ' }
        Write-Host "    $marker $dir" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "  启动方式（先确保 web-app 8081 已启动）：" -ForegroundColor Gray
Write-Host "    cd `"$Target`"" -ForegroundColor Gray
Write-Host "    npm run dev" -ForegroundColor Gray
Write-Host "    → http://localhost:$port" -ForegroundColor Gray
Write-Host ""
Write-Host "  或者用一键脚本（它会自动优先启动本目录下的 rentHouseH5）：" -ForegroundColor Gray
Write-Host "    .\tools\start-all.ps1" -ForegroundColor Gray
Write-Host ""
if (-not $RemoveOld -and (Test-Path $oldAppUi)) {
    Write-Host "  旧的 web-app-frontend 还在（已标记弃用）。想真正删掉它：" -ForegroundColor DarkGray
    Write-Host "    .\tools\replace-app-ui.ps1 -RemoveOld -Force" -ForegroundColor DarkGray
    Write-Host "    或手动：Remove-Item -Recurse -Force `"$oldAppUi`"" -ForegroundColor DarkGray
    Write-Host ""
}
