# check-diagrams.ps1 —— 图产物防陈旧闸（不依赖 d2，纯三方哈希对账）：
#   源(.d2) 变而未重渲染 → stale；产物(svg) 被手改/丢失/孤儿 → stale；manifest 缺失 → fail。
# 交付前与 check-docs 同跑（ddd-review 末步）。用法：powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-diagrams.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$dgrid = Join-Path $root 'knowledge\diagrams'
$gen = Join-Path $dgrid 'gen'
$manifestPath = Join-Path $gen 'manifest.sha256'

if (-not (Test-Path $manifestPath)) {
    Write-Host 'result: FAIL - manifest 缺失（先跑 scripts/render-diagrams.ps1）'
    exit 1
}

function Get-Sha256($path) { (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant() }

$entries = @{}
foreach ($line in (Get-Content -LiteralPath $manifestPath -Encoding UTF8)) {
    if ($line -match '^([0-9a-f]{64})\s+([0-9a-f]{64})\s+(.+)$') { $entries[$matches[3]] = @($matches[1], $matches[2]) }
}

$problems = @()
$sources = Get-ChildItem -Recurse -File -Path $dgrid -Filter '*.d2' |
    Where-Object { $_.FullName -notmatch '\\gen\\' }
foreach ($src in $sources) {
    $rel = ($src.FullName.Substring($dgrid.Length + 1) -replace '\\','/')
    $svgRel = ($rel -replace '\.d2$','.svg')
    if (-not $entries.ContainsKey($svgRel)) { $problems += "$rel : 未入 manifest（跑 render-diagrams）"; continue }
    $srcHash, $svgHash = $entries[$svgRel]
    if ((Get-Sha256 $src.FullName) -ne $srcHash) { $problems += "$rel : 源已变、产物过期（重渲染）"; continue }
    $svg = Join-Path $gen ($svgRel -replace '/','\')
    if (-not (Test-Path -LiteralPath $svg)) { $problems += "$svgRel : 产物缺失（须入库）" }
    elseif ((Get-Sha256 $svg) -ne $svgHash) { $problems += "$svgRel : 产物与源脱钩（SVG 被手改？）" }
}
# 孤儿产物：gen 下 svg 无对应源
foreach ($svg in (Get-ChildItem -Recurse -File -Path $gen -Filter '*.svg')) {
    $rel = ($svg.FullName.Substring($gen.Length + 1) -replace '\\','/')
    if (-not (Test-Path -LiteralPath (Join-Path $dgrid ($rel -replace '\.svg$','.d2')))) { $problems += "gen/${rel}: 孤儿产物（无源文件）" }
}

if ($problems.Count -gt 0) {
    $problems | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("result: FAIL({0}) - 图产物陈旧，先跑 render-diagrams 并重审语义" -f $problems.Count)
    exit 1
}
Write-Host ("result: OK - {0} 图源/产物/manifest 三方一致" -f $sources.Count)
exit 0
