# =============================================================================
#  收尾：把 rentHouseH5 变成 web-app-frontend，并删除 AI 问答功能
#
#  做完之后目录是：
#    lease\web-app-frontend\        ← 官方 H5（Vue3 + Vant4，13 个页面，端口 8888）
#    lease\web-admin-frontend\      ← 后台管理
#
#  同时会删掉后端 AI 相关的 8 个文件：
#    controller\ai\AiChatController.java
#    service\AiService.java
#    service\impl\AiServiceImpl.java
#    vo\ai\ChatRequestVo.java
#    vo\ai\ChatResponseVo.java
#    vo\ai\RecommendRequestVo.java
#    custom\config\DeepSeekConfig.java
#    custom\config\DeepSeekConfigProperties.java
#
#  用法（在项目根目录，或直接双击 tools\finalize-web-app.bat）：
#      .\tools\finalize-web-app.ps1
#
#  参数：
#      -KeepBackup      删除旧 web-app-frontend 前先压成 web-app-frontend-backup.zip
#      -SkipAiCleanup   只做文件夹替换，不动后端 AI 代码
#      -Force           旧目录存在时直接覆盖（不加会先询问）
# =============================================================================

param(
    [switch]$KeepBackup,
    [switch]$SkipAiCleanup,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
    $root = (Get-Location).Path
}

$src = Join-Path $root 'rentHouseH5'
$dst = Join-Path $root 'web-app-frontend'

Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  收尾：rentHouseH5  ->  web-app-frontend  +  删除 AI 问答" -ForegroundColor Cyan
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""
Write-Host "  lease 根目录 : $root" -ForegroundColor Gray
Write-Host ""

# -----------------------------------------------------------------------------
# 1. 检查源目录
# -----------------------------------------------------------------------------
if (-not (Test-Path $src)) {
    Write-Host "[错误] 找不到 $src" -ForegroundColor Red
    Write-Host "       请先把官方 rentHouseH5 复制到 lease 根目录下" -ForegroundColor Yellow
    exit 1
}
if (-not (Test-Path (Join-Path $src 'package.json'))) {
    Write-Host "[错误] $src 里没有 package.json，不像是 H5 项目" -ForegroundColor Red
    exit 1
}

$pkg = Get-Content (Join-Path $src 'package.json') -Raw | ConvertFrom-Json
if ($pkg.name -ne 'vue3-h5-template') {
    Write-Host "[提醒] 项目名是 '$($pkg.name)'，不是预期的 'vue3-h5-template'" -ForegroundColor Yellow
}

$hasNM = Test-Path (Join-Path $src 'node_modules')
Write-Host "  [1/4] 源目录检查通过（node_modules: $(if ($hasNM) { '有' } else { '无，稍后需 npm install' })）" -ForegroundColor Green

# -----------------------------------------------------------------------------
# 2. 处理已存在的 web-app-frontend
# -----------------------------------------------------------------------------
if (Test-Path $dst) {
    Write-Host ""
    Write-Host "  [2/4] 旧的 web-app-frontend 存在，准备删除" -ForegroundColor Yellow
    Write-Host "        （如果它的 npm run dev 还开着，请先关掉那个窗口，否则会占用文件）" -ForegroundColor DarkGray

    if ($KeepBackup) {
        $zip = Join-Path $root 'web-app-frontend-backup.zip'
        Write-Host "        正在备份到 $zip ..." -ForegroundColor Cyan
        if (Test-Path $zip) { Remove-Item -Force $zip }
        Compress-Archive -Path $dst -DestinationPath $zip -CompressionLevel Optimal
        Write-Host "        备份完成" -ForegroundColor Green
    }

    if (-not $Force) {
        $answer = Read-Host "        确认删除 $dst ? (y/N)"
        if ($answer -ne 'y' -and $answer -ne 'Y') {
            Write-Host "        已取消" -ForegroundColor Yellow
            exit 1
        }
    }

    try {
        Remove-Item -Recurse -Force $dst
        Write-Host "        已删除旧目录" -ForegroundColor Green
    }
    catch {
        Write-Host "[错误] 删除失败：$($_.Exception.Message)" -ForegroundColor Red
        Write-Host "       多半是还有进程占用（关掉 npm run dev / 资源管理器窗口后重试）" -ForegroundColor Yellow
        exit 1
    }
}
else {
    Write-Host ""
    Write-Host "  [2/4] 没有旧的 web-app-frontend，跳过删除" -ForegroundColor Green
}

# -----------------------------------------------------------------------------
# 3. 改名 rentHouseH5 -> web-app-frontend
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "  [3/4] 把 rentHouseH5 重命名为 web-app-frontend ..." -ForegroundColor Cyan
try {
    Move-Item -Path $src -Destination $dst
    Write-Host "        完成：$dst" -ForegroundColor Green
}
catch {
    Write-Host "[错误] 重命名失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# -----------------------------------------------------------------------------
# 4. 删除后端 AI 问答功能
# -----------------------------------------------------------------------------
Write-Host ""
if ($SkipAiCleanup) {
    Write-Host "  [4/4] -SkipAiCleanup 指定，保留后端 AI 代码" -ForegroundColor Yellow
}
else {
    Write-Host "  [4/4] 删除后端 AI 问答相关文件 ..." -ForegroundColor Cyan
    $aiFiles = @(
        'web\web-app\src\main\java\com\atguigu\lease\web\app\controller\ai\AiChatController.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\service\AiService.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\service\impl\AiServiceImpl.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\vo\ai\ChatRequestVo.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\vo\ai\ChatResponseVo.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\vo\ai\RecommendRequestVo.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\custom\config\DeepSeekConfig.java',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\custom\config\DeepSeekConfigProperties.java'
    )

    $removed = 0
    $missing = 0
    foreach ($rel in $aiFiles) {
        $full = Join-Path $root $rel
        if (Test-Path $full) {
            Remove-Item -Force $full
            $removed++
            Write-Host "        - $rel" -ForegroundColor DarkGray
        }
        else {
            $missing++
        }
    }
    Write-Host "        删除 $removed 个文件（$missing 个本来就不存在）" -ForegroundColor Green

    # 顺手清掉空目录
    foreach ($relDir in @(
            'web\web-app\src\main\java\com\atguigu\lease\web\app\controller\ai',
            'web\web-app\src\main\java\com\atguigu\lease\web\app\vo\ai')) {
        $fullDir = Join-Path $root $relDir
        if ((Test-Path $fullDir) -and -not (Get-ChildItem $fullDir -Force)) {
            Remove-Item -Force $fullDir
            Write-Host "        - 空目录 $relDir" -ForegroundColor DarkGray
        }
    }
}

# -----------------------------------------------------------------------------
# 结果
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  完成 🎉" -ForegroundColor Green
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""
Write-Host "  lease 目录下的前端：" -ForegroundColor Gray
foreach ($dir in @('web-app-frontend', 'web-admin-frontend')) {
    $full = Join-Path $root $dir
    if (Test-Path $full) {
        Write-Host "    ✓ $dir" -ForegroundColor Gray
    }
    else {
        Write-Host "    ✗ $dir （不存在）" -ForegroundColor DarkGray
    }
}

# 读一下新前端自己的端口
$port = 8888
$envFile = Join-Path $dst '.env.development'
if (Test-Path $envFile) {
    $line = Select-String -Path $envFile -Pattern 'VITE_APP_PORT' | Select-Object -First 1
    if ($line) {
        $raw = ($line.Line -split '=', 2)[1].Trim().Trim("'").Trim('"')
        if ($raw -match '^\d+$') { $port = [int]$raw }
    }
}

Write-Host ""
Write-Host "  接下来：" -ForegroundColor Gray
Write-Host "    1) 重启后端（AI 代码被删了，必须重新编译）" -ForegroundColor Gray
Write-Host "         cd web\web-app ; mvn spring-boot:run" -ForegroundColor Gray
Write-Host "    2) 起移动端" -ForegroundColor Gray
Write-Host "         cd web-app-frontend ; npm run dev" -ForegroundColor Gray
Write-Host "         → http://localhost:$port" -ForegroundColor Gray
Write-Host "    3) 或者一条命令起全部： .\tools\start-all.bat" -ForegroundColor Gray
Write-Host ""
if (-not $hasNM) {
    Write-Host "  [提醒] 这份没有 node_modules，先执行： cd web-app-frontend ; npm install" -ForegroundColor Yellow
    Write-Host ""
}
