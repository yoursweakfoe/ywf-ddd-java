# render-diagrams.ps1 —— 把 knowledge/diagrams/ 下全部 .d2 源渲染为 gen/ 镜像路径 SVG 并写 manifest。
# 镜像约定：diagrams/<文档仓库相对路径(去 .md)>/<图名>.d2 → diagrams/gen/<同路径>/<图名>.svg。
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/render-diagrams.ps1 [-Layout tala]
# 依赖：d2 CLI（winget Terrastruct.D2；未装时探测 C:\Program Files\D2\d2.exe）。
# 默认引擎 TALA（d2 官方原生架构图库内引擎；三引擎同源码肉眼对比裁决 2026-09-09，dagre 斜线乱串、elk 尚可、tala 最优）。
param([string]$Layout = 'tala')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)   # 仓库根（scripts 在 knowledge/ 下）
$dgrid = Join-Path $root 'knowledge\diagrams'
if (-not (Test-Path $dgrid)) { Write-Error "diagrams 目录不存在: $dgrid"; exit 1 }

$d2 = Get-Command d2 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
if (-not $d2) { $cand = 'C:\Program Files\D2\d2.exe'; if (Test-Path $cand) { $d2 = $cand } }
if (-not $d2) { Write-Error 'd2 CLI 未安装（winget install --id Terrastruct.D2 -e）'; exit 1 }

function Get-Sha256($path) {
    (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant()
}

$sources = Get-ChildItem -Recurse -File -Path $dgrid -Filter '*.d2' |
    Where-Object { $_.FullName -notmatch '\\gen\\' } | Sort-Object FullName
$manifest = @()
$failed = 0

foreach ($src in $sources) {
    $rel = $src.FullName.Substring($dgrid.Length + 1)                       # knowledge\README\drive-relations.d2
    $outRel = Join-Path (Split-Path -Parent $rel) ($src.BaseName + '.svg')  # knowledge\README\drive-relations.svg
    $out = Join-Path $dgrid (Join-Path 'gen' $outRel)
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $out) | Out-Null
    Write-Host "render: $rel"
    # d2 把成功横幅写 stderr——EAP=Stop 下原生命中会被包装成终止错误；
    # 正规解法：2>&1 接流后手动还原 ErrorRecord 文本，成败只以 $LASTEXITCODE 裁决
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $d2out = & $d2 --layout $Layout $src.FullName $out 2>&1
    $d2out | ForEach-Object { if ($_ -is [System.Management.Automation.ErrorRecord]) { Write-Host "  $($_.Exception.Message)" } else { Write-Host "  $_" } }
    $ErrorActionPreference = $prevEAP
    if ($LASTEXITCODE -ne 0) { Write-Warning "d2 编译失败: $rel"; $failed++; continue }
    $manifest += ('{0}  {1}  {2}' -f (Get-Sha256 $src.FullName), (Get-Sha256 $out), ($outRel -replace '\\','/'))
}

if ($failed -gt 0) { Write-Error "$failed 张图编译失败，manifest 未写"; exit 1 }
$genDir = Join-Path $dgrid 'gen'
New-Item -ItemType Directory -Force -Path $genDir | Out-Null
[System.IO.File]::WriteAllLines((Join-Path $genDir 'manifest.sha256'), [string[]]$manifest, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("done: {0} 张图已渲染，manifest 写入 gen/manifest.sha256" -f $manifest.Count)
