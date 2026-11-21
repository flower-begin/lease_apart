# =============================================================================
#  尚庭公寓（lease）一键自检脚本
#
#  用途：本仓库的代码在交付环境里没能实际编译（开发环境 shell 不可用），
#        所以提供这个脚本，让你在自己机器上一条命令拿到「能不能编译」的结论。
#
#  用法（在项目根目录 D:\JavaProject\lease 下，用 PowerShell 执行）：
#      .\tools\verify.ps1
#
#  可选参数：
#      -SkipFrontend     只检查后端
#      -SkipBackend      只检查前端
#      -Install          前端缺依赖时自动 npm install
#
#  说明：脚本只做「检查」，不会修改源码；失败时退出码为 1。
# =============================================================================

param(
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$Install
)

$ErrorActionPreference = 'Continue'
$script:failures = @()

function Write-Section($title) {
    Write-Host ""
    Write-Host ("=" * 72) -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host ("=" * 72) -ForegroundColor Cyan
}

function Write-Ok($msg) { Write-Host "  [OK]   $msg" -ForegroundColor Green }
function Write-Warn2($msg) { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red; $script:failures += $msg }

function Test-Command($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
    # 兼容直接在项目根目录执行的情况
    $root = (Get-Location).Path
}

Write-Host ""
Write-Host "项目根目录: $root" -ForegroundColor Gray

# -----------------------------------------------------------------------------
# 0. 环境检查
# -----------------------------------------------------------------------------
Write-Section "0. 环境检查"

if (Test-Command 'java') {
    $javaVersion = (java -version 2>&1 | Select-Object -First 1)
    Write-Ok "java: $javaVersion"
} else {
    Write-Err "未找到 java，请安装 JDK 17+"
}

if (Test-Command 'mvn') {
    $mvnVersion = (mvn -v 2>&1 | Select-Object -First 1)
    Write-Ok "maven: $mvnVersion"
} else {
    Write-Warn2 "未找到 mvn（若用 IDEA 内置 Maven 可忽略）"
}

if (Test-Command 'node') {
    Write-Ok "node: $(node -v)"
} elseif (-not $SkipFrontend) {
    Write-Err "未找到 node，前端无法构建（安装 Node.js 18+ 后重试）"
}

if (Test-Command 'npm') {
    Write-Ok "npm: $(npm -v)"
} elseif (-not $SkipFrontend) {
    Write-Warn2 "未找到 npm"
}

# -----------------------------------------------------------------------------
# 1. 后端编译
# -----------------------------------------------------------------------------
if (-not $SkipBackend) {
    Write-Section "1. 后端编译（mvn -DskipTests compile）"

    if (-not (Test-Command 'mvn')) {
        Write-Warn2 "跳过后端编译：没有 mvn"
    } else {
        Push-Location $root
        try {
            & mvn -q -DskipTests -Dmaven.test.skip=true compile
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "后端编译通过"
            } else {
                Write-Err "后端编译失败，请把上面 mvn 的报错贴出来"
            }
        } finally {
            Pop-Location
        }
    }
}

# -----------------------------------------------------------------------------
# 2. 前端类型检查
# -----------------------------------------------------------------------------
$frontends = @()

# 用户端 H5：优先 rentHouseH5（还没改名时），其次原位置，最后 web-app-frontend（最终位置）
$appUiCandidates = @(
    (Join-Path $root 'rentHouseH5'),
    'D:\尚庭公寓\09_尚庭公寓\09_尚庭公寓\代码\代码\前端\rentHouseH5',
    (Join-Path $root 'web-app-frontend')
)
foreach ($candidate in $appUiCandidates) {
    if (Test-Path (Join-Path $candidate 'package.json')) {
        $frontends += @{ Name = "用户端 H5 ($candidate)"; Path = $candidate }
        break
    }
}

# 后台管理前端：优先检查本项目内的 web-admin-frontend（已按 rentHouseAdmin 风格重做）
$adminUiCandidates = @(
    (Join-Path $root 'web-admin-frontend'),
    'D:\尚庭公寓\前端环境\前端环境\rentHouseAdmin',
    'D:\尚硅谷java笔记\前端\07_尚庭公寓前端实战\07_尚庭公寓前端实战\代码\完成版代码\rentHouseAdmin',
    (Join-Path $root 'rentHouseAdmin')
)
foreach ($candidate in $adminUiCandidates) {
    if (Test-Path (Join-Path $candidate 'package.json')) {
        $frontends += @{ Name = "后台管理 ($candidate)"; Path = $candidate }
        break
    }
}

if (-not $SkipFrontend) {
    foreach ($fe in $frontends) {
        Write-Section "2. 前端类型检查 - $($fe.Name)"

        if (-not (Test-Path $fe.Path)) {
            Write-Err "目录不存在: $($fe.Path)"
            continue
        }

        Push-Location $fe.Path
        try {
            if (-not (Test-Path 'node_modules')) {
                if ($Install) {
                    Write-Host "  node_modules 不存在，执行 npm install ..." -ForegroundColor Gray
                    & npm install --no-audit --no-fund
                    if ($LASTEXITCODE -ne 0) {
                        Write-Err "$($fe.Name): npm install 失败"
                        continue
                    }
                } else {
                    Write-Warn2 "$($fe.Name) 缺少 node_modules，已跳过（加 -Install 自动安装）"
                    continue
                }
            }

            if (-not (Test-Path 'node_modules\.bin\vue-tsc.cmd') -and -not (Test-Path 'node_modules/vue-tsc')) {
                Write-Warn2 "$($fe.Name) 未安装 vue-tsc，跳过类型检查"
                continue
            }

            Write-Host "  执行 vue-tsc --noEmit ..." -ForegroundColor Gray
            & npx --no-install vue-tsc --noEmit
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "$($fe.Name) 类型检查通过"
            } else {
                Write-Err "$($fe.Name) 类型检查失败，按上面的文件:行号逐条修"
            }
        } finally {
            Pop-Location
        }
    }
}

# -----------------------------------------------------------------------------
# 3. 数据库与配置体检
# -----------------------------------------------------------------------------
Write-Section "3. 配置体检"

$sqlFile = Join-Path $root 'sql\lease.sql'
if (Test-Path $sqlFile) {
    $sqlLines = (Get-Content $sqlFile).Count
    Write-Ok "sql/lease.sql 存在（$sqlLines 行）"
    Write-Host "         执行：mysql -uroot -p --default-character-set=utf8mb4 < `"$sqlFile`"" -ForegroundColor Gray
} else {
    Write-Err "缺少 sql/lease.sql"
}

foreach ($cfg in @(
        'web\web-admin\src\main\resources\application.yaml',
        'web\web-app\src\main\resources\application.yaml')) {
    $p = Join-Path $root $cfg
    if (Test-Path $p) {
        Write-Ok "$cfg 存在"
    } else {
        Write-Err "$cfg 缺失"
    }
}

# 数据库连通性（有 mysql 客户端才测）
if (Test-Command 'mysql') {
    try {
        $probe = & mysql -uroot -p123456 -h127.0.0.1 -P3306 -N -e "select count(*) from information_schema.tables where table_schema='lease';" 2>&1
        if ($LASTEXITCODE -eq 0) {
            $count = ($probe | Select-Object -Last 1).ToString().Trim()
            if ([int]$count -gt 20) {
                Write-Ok "数据库 lease 已初始化（$count 张表）"
            } else {
                Write-Warn2 "数据库 lease 里只有 $count 张表，可能还没执行 sql/lease.sql"
            }
        } else {
            Write-Warn2 "连不上 MySQL（root/123456@127.0.0.1:3306），请确认已启动并改过密码"
        }
    } catch {
        Write-Warn2 "MySQL 探测失败：$($_.Exception.Message)"
    }
} else {
    Write-Warn2 "未找到 mysql 客户端，跳过数据库连通性检查"
}

# 上传目录
$uploadDir = 'D:\upload'
if (Test-Path $uploadDir) {
    Write-Ok "上传目录 $uploadDir 存在"
} else {
    Write-Warn2 "上传目录 $uploadDir 不存在；后端首次上传时会自动创建，也可先手动建一个"
}

# -----------------------------------------------------------------------------
# 4. 汇总
# -----------------------------------------------------------------------------
Write-Section "4. 汇总"

if ($script:failures.Count -eq 0) {
    Write-Host "  全部检查通过 ✅" -ForegroundColor Green
    Write-Host ""
    Write-Host "  下一步启动（三个终端）：" -ForegroundColor Gray
    Write-Host "    mvn -pl web/web-admin spring-boot:run" -ForegroundColor Gray
    Write-Host "    mvn -pl web/web-app   spring-boot:run" -ForegroundColor Gray

    if ($frontends.Count -gt 0) {
        Write-Host "    cd `"$($frontends[0].Path)`"; npm run dev" -ForegroundColor Gray
    }
    $adminFe = $adminUiCandidates | Where-Object { Test-Path (Join-Path $_ 'package.json') } | Select-Object -First 1
    if ($adminFe) {
        Write-Host "    cd `"$adminFe`"; npm run dev" -ForegroundColor Gray
    }

    Write-Host ""
    Write-Host "  后台界面 http://localhost:5174 （admin/123456）" -ForegroundColor Gray
    Write-Host "  用户端 H5 端口看它自己的 .env.development（官方 rentHouseH5 是 8888）" -ForegroundColor Gray
} else {
    Write-Host "  有 $($script:failures.Count) 项未通过：" -ForegroundColor Red
    $i = 1
    foreach ($f in $script:failures) {
        Write-Host "    $i) $f" -ForegroundColor Red
        $i++
    }
    exit 1
}
