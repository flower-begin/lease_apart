# =============================================================================
#  删除后端「AI 问答」功能的 8 个 Java 文件
#
#  背景：yaml 的 deepseek 配置、/app/ai/** 放行、Knife4j 的 AI 分组
#        已经在代码里改好了，只剩这 8 个文件没删（需要文件系统删除权限）。
#
#  用法（项目根目录，或双击 tools\remove-ai.bat）：
#      .\tools\remove-ai.ps1
# =============================================================================

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
    $root = (Get-Location).Path
}

Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  删除后端 AI 问答代码" -ForegroundColor Cyan
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""

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
        Write-Host "  - $rel" -ForegroundColor DarkGray
    }
    else {
        $missing++
    }
}

Write-Host ""
Write-Host "  删除 $removed 个文件（$missing 个本来就不存在）" -ForegroundColor Green

foreach ($relDir in @(
        'web\web-app\src\main\java\com\atguigu\lease\web\app\controller\ai',
        'web\web-app\src\main\java\com\atguigu\lease\web\app\vo\ai')) {
    $fullDir = Join-Path $root $relDir
    if ((Test-Path $fullDir) -and -not (Get-ChildItem $fullDir -Force)) {
        Remove-Item -Force $fullDir
        Write-Host "  - 空目录 $relDir" -ForegroundColor DarkGray
    }
}

# 检查是否还有残留引用
Write-Host ""
Write-Host "  检查残留引用 ..." -ForegroundColor Cyan
$hits = Get-ChildItem -Path (Join-Path $root 'web\web-app\src') -Recurse -Filter *.java |
    Select-String -Pattern 'AiService|DeepSeek|RestTemplate|ChatRequestVo|RecommendRequestVo' -ErrorAction SilentlyContinue
if ($hits) {
    Write-Host "  [警告] 还有文件引用了 AI 相关类，可能导致编译失败：" -ForegroundColor Yellow
    $hits | Select-Object -First 10 | ForEach-Object { Write-Host "     $($_.Path):$($_.LineNumber)" -ForegroundColor Yellow }
}
else {
    Write-Host "  ✅ 没有残留引用" -ForegroundColor Green
}

Write-Host ""
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host "  完成。记得重启 web-app 后端（要重新编译）" -ForegroundColor Green
Write-Host ("=" * 72) -ForegroundColor Cyan
Write-Host ""
