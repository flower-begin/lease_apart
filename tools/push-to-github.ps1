# =============================================================================
#  提交并推送到 GitHub，并校验「远程 == 本地」
#
#  用法（在项目根目录）：
#      .\tools\push-to-github.ps1
#      .\tools\push-to-github.ps1 -Message "feat: 换成官方移动端 H5，移除 AI 问答"
#
#  参数：
#      -Message  "提交说明"     不传则用带时间戳的默认说明
#      -Remote   origin1        默认 origin1（本仓库配置的远程名）
#      -Branch   master         默认当前分支（你当前是 feature_payment）
#      -NoPush                  只提交不推送（自查用）
#      -AlsoUpdateMaster        推完当前分支后，把远程 master 也指向同一个提交，
#                               这样 GitHub 默认分支看到的代码和本地一致
#
#  会做的事：
#      1. 打印当前分支 / 改动概览
#      2. 检查 node_modules 之类的目录没有被误加入
#      3. git add -A  +  commit
#      4. git push
#      5. git fetch + git ls-remote 双重校验远程 HEAD 与本地 HEAD 一致
#      6. 打印 git status 确认工作区干净
# =============================================================================

param(
    [string]$Message,
    [string]$Remote = 'origin1',
    [string]$Branch,
    [switch]$NoPush,
    [switch]$AlsoUpdateMaster
)

$ErrorActionPreference = 'Stop'

# -----------------------------------------------------------------------------
# 0. 定位仓库根目录
# -----------------------------------------------------------------------------
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root '.git'))) {
    $root = (Get-Location).Path
}
if (-not (Test-Path (Join-Path $root '.git'))) {
    Write-Host "[错误] 找不到 .git 目录，请在 lease 项目根目录运行" -ForegroundColor Red
    exit 1
}
Set-Location $root

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "[错误] 找不到 git，请先安装 Git 并加入 PATH" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  提交并推送到 GitHub" -ForegroundColor Cyan
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""

# -----------------------------------------------------------------------------
# 1. 分支 & 远程
# -----------------------------------------------------------------------------
$currentBranch = (& git rev-parse --abbrev-ref HEAD).Trim()
if (-not $Branch) { $Branch = $currentBranch }

if ($currentBranch -eq 'HEAD') {
    Write-Host "[错误] 当前处于 detached HEAD 状态，请先 git checkout 一个分支" -ForegroundColor Red
    exit 1
}

$remoteUrl = (& git remote get-url $Remote 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $remoteUrl) {
    Write-Host "[错误] 远程 '$Remote' 不存在。现有远程：" -ForegroundColor Red
    & git remote -v
    exit 1
}

Write-Host "  远程   : $Remote -> $remoteUrl" -ForegroundColor Gray
Write-Host "  分支   : $currentBranch -> $Branch" -ForegroundColor Gray
if ($remoteUrl -like 'git@*') {
    Write-Host "           （SSH 方式，需要本机已配置 GitHub SSH key）" -ForegroundColor DarkGray
}
Write-Host ""

# -----------------------------------------------------------------------------
# 2. 改动概览
# -----------------------------------------------------------------------------
Write-Host "[1/5] 当前改动：" -ForegroundColor Cyan
$statusLines = & git status --porcelain
if (-not $statusLines) {
    Write-Host "      工作区干净，没有需要提交的改动" -ForegroundColor Green
}
else {
    $statusLines | Select-Object -First 40 | ForEach-Object { Write-Host "      $_" -ForegroundColor Gray }
    if ($statusLines.Count -gt 40) {
        Write-Host "      ... 共 $($statusLines.Count) 项" -ForegroundColor DarkGray
    }
    Write-Host "      合计 $($statusLines.Count) 项改动" -ForegroundColor Gray
}
Write-Host ""

# -----------------------------------------------------------------------------
# 3. add + 安全检查
# -----------------------------------------------------------------------------
Write-Host "[2/5] git add -A ..." -ForegroundColor Cyan
& git add -A
if ($LASTEXITCODE -ne 0) {
    Write-Host "[错误] git add 失败" -ForegroundColor Red
    exit 1
}

$staged = & git diff --cached --name-only
$danger = $staged | Where-Object {
    $_ -match '(^|/)node_modules/' -or $_ -match '(^|/)target/' -or $_ -match '\.class$' -or $_ -match '(^|/)dist/'
}
if ($danger) {
    Write-Host ""
    Write-Host "[严重] 发现了不该提交的文件（已在 .gitignore 里，但仍被暂存）：" -ForegroundColor Red
    $danger | Select-Object -First 20 | ForEach-Object { Write-Host "       $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "  已自动取消暂存。请检查 .gitignore：" -ForegroundColor Yellow
    & git reset -- $danger | Out-Null
    exit 1
}

$stagedCount = ($staged | Measure-Object).Count
Write-Host "      暂存 $stagedCount 个文件" -ForegroundColor Gray
if ($stagedCount -gt 0) {
    Write-Host "      其中新增/修改最多的目录：" -ForegroundColor DarkGray
    $staged |
        ForEach-Object { ($_ -split '/')[0] } |
        Group-Object |
        Sort-Object Count -Descending |
        Select-Object -First 6 |
        ForEach-Object { Write-Host ("        {0,-28} {1} 个" -f $_.Name, $_.Count) -ForegroundColor DarkGray }
}
Write-Host ""

# -----------------------------------------------------------------------------
# 4. commit
# -----------------------------------------------------------------------------
Write-Host "[3/5] 提交 ..." -ForegroundColor Cyan
if ($stagedCount -eq 0) {
    Write-Host "      没有需要提交的内容，跳过 commit" -ForegroundColor Yellow
}
else {
    if (-not $Message) {
        $Message = "chore: update $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    }
    & git commit -m $Message
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] git commit 失败（可能没配置 user.name / user.email）" -ForegroundColor Red
        Write-Host '        git config --global user.name  "你的名字"' -ForegroundColor Yellow
        Write-Host '        git config --global user.email "你的邮箱"' -ForegroundColor Yellow
        exit 1
    }
    Write-Host "      已提交：$Message" -ForegroundColor Green
}
Write-Host ""

$localHead = (& git rev-parse HEAD).Trim()
Write-Host "  本地 HEAD : $localHead" -ForegroundColor Gray
Write-Host ""

if ($NoPush) {
    Write-Host "[4/5] -NoPush 指定，跳过推送" -ForegroundColor Yellow
    Write-Host ""
    exit 0
}

# -----------------------------------------------------------------------------
# 5. push
# -----------------------------------------------------------------------------
$verifyBranches = @($Branch)

Write-Host "[4/5] 推送 $Remote/$Branch ..." -ForegroundColor Cyan
& git push -u $Remote "$Branch"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[错误] 推送失败。常见原因：" -ForegroundColor Red
    Write-Host "       1) SSH key 没配好    -> ssh -T git@github.com 自测" -ForegroundColor Yellow
    Write-Host "       2) 远程有新提交      -> git pull $Remote $Branch --rebase 再推" -ForegroundColor Yellow
    Write-Host "       3) 没有仓库写权限" -ForegroundColor Yellow
    exit 1
}
Write-Host "      推送成功" -ForegroundColor Green

# 可选：把 master 也指向同一个提交，让 GitHub 默认分支看到的代码和本地一致
if ($AlsoUpdateMaster -and $Branch -ne 'master') {
    Write-Host ""
    Write-Host "      同步更新远程 master 到同一提交 ..." -ForegroundColor Cyan
    & git push $Remote "HEAD:refs/heads/master"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "      ✅ master 已同步" -ForegroundColor Green
        $verifyBranches += 'master'
    }
    else {
        Write-Host "      ⚠ master 同步失败（多半是远程 master 有本地没有的提交，不是快进）" -ForegroundColor Yellow
        Write-Host "        可以手动处理：" -ForegroundColor Yellow
        Write-Host "          git fetch $Remote" -ForegroundColor DarkYellow
        Write-Host "          git push $Remote `"HEAD:refs/heads/master`" --force-with-lease" -ForegroundColor DarkYellow
    }
}
elseif ($Branch -eq 'master') {
    $verifyBranches = @('master')
}
else {
    Write-Host ""
    Write-Host "      提示：只推了 $Branch。如果希望 GitHub 默认分支（master）也是这份代码，" -ForegroundColor DarkGray
    Write-Host "            重跑时加 -AlsoUpdateMaster： .\tools\push-to-github.ps1 -AlsoUpdateMaster" -ForegroundColor DarkGray
}
Write-Host ""

# -----------------------------------------------------------------------------
# 6. 校验「远程 == 本地」
# -----------------------------------------------------------------------------
Write-Host "[5/5] 校验远程与本地是否一致 ..." -ForegroundColor Cyan
Write-Host ""
Write-Host "  本地 HEAD : $localHead" -ForegroundColor Gray

& git fetch $Remote --quiet 2>$null

$ok = $true
$checkedAny = $false

foreach ($b in $verifyBranches) {
    $fetched = $null
    $ref = (& git rev-parse "$Remote/$b" 2>$null)
    if ($LASTEXITCODE -eq 0 -and $ref) { $fetched = $ref.Trim() }

    # ls-remote 是权威值（直接问服务器）
    $live = $null
    $lsOut = & git ls-remote $Remote "refs/heads/$b" 2>$null
    if ($LASTEXITCODE -eq 0 -and $lsOut) { $live = ($lsOut -split '\s+')[0].Trim() }

    $branchOk = $true
    if ($live -and $live -ne $localHead) { $branchOk = $false }
    if ($fetched -and $fetched -ne $localHead) { $branchOk = $false }
    if ($live -or $fetched) { $checkedAny = $true }

    $shown = if ($live) { $live } elseif ($fetched) { $fetched } else { '(读取失败)' }
    if ($branchOk -and ($live -or $fetched)) {
        Write-Host "  ✅ $Remote/$b = $shown" -ForegroundColor Green
    }
    elseif ($live -or $fetched) {
        Write-Host "  ❌ $Remote/$b = $shown  （与本地不一致）" -ForegroundColor Red
        $ok = $false
    }
    else {
        Write-Host "  ⚠ $Remote/$b 无法读取（网络 / 权限），跳过" -ForegroundColor Yellow
    }

    # 文件级差异
    $diffStat = & git diff --stat "HEAD" "$Remote/$b" 2>$null
    if ($diffStat) {
        Write-Host "     与 $b 存在文件差异：" -ForegroundColor DarkYellow
        $diffStat | Select-Object -First 10 | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkYellow }
    }
}

if (-not $checkedAny) {
    Write-Host "  [警告] 没能读到任何远程 HEAD，无法确认一致性" -ForegroundColor Yellow
    $ok = $false
}

Write-Host ""
if ($ok) {
    Write-Host "  ✅ 远程仓库与本地完全一致（提交哈希 + 文件内容都比对过了）" -ForegroundColor Green
}
else {
    Write-Host "  ❌ 远程与本地不一致，请看上面标红的分支" -ForegroundColor Red
}

# 工作区是否干净
$dirty = & git status --porcelain
if ($dirty) {
    Write-Host ""
    Write-Host "  ⚠ 注意：工作区还有未提交的改动" -ForegroundColor Yellow
    $dirty | Select-Object -First 20 | ForEach-Object { Write-Host "     $_" -ForegroundColor DarkYellow }
}
else {
    Write-Host "  ✅ 工作区干净（没有未提交的改动）" -ForegroundColor Green
}

Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  完成" -ForegroundColor Green
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""
Write-Host "  仓库地址：https://github.com/flower-begin/lease_apart" -ForegroundColor Gray
Write-Host "  已推送  ：$($verifyBranches -join ', ')" -ForegroundColor Gray
Write-Host ""
