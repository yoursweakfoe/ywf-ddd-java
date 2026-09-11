# render-diagrams.ps1 —— 把 knowledge/diagrams/ 下全部 .d2 源渲染为同目录邻座 SVG 并写 manifest。
# 同名并存约定：diagrams/<文档仓库相对路径(去 .md)>/<图名>.d2 → 同目录 <图名>.svg；manifest 落 diagrams/ 根。
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

$sources = Get-ChildItem -Recurse -File -Path $dgrid -Filter '*.d2' | Sort-Object FullName
$manifest = @()
$failed = 0

foreach ($src in $sources) {
    $rel = $src.FullName.Substring($dgrid.Length + 1)                       # knowledge\README\drive-relations.d2
    $out = Join-Path $src.DirectoryName ($src.BaseName + '.svg')             # 源旁邻座同名 SVG，不落独立产物层
    $outRel = ($rel -replace '\.d2$', '.svg') -replace '\\', '/'            # knowledge/README/drive-relations.svg
    Write-Host "render: $rel"
    # d2 把成功横幅写 stderr——EAP=Stop 下原生命中会被包装成终止错误；
    # 正规解法：2>&1 接流后手动还原 ErrorRecord 文本，成败只以 $LASTEXITCODE 裁决
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $d2out = & $d2 --layout $Layout $src.FullName $out 2>&1
    $d2out | ForEach-Object { if ($_ -is [System.Management.Automation.ErrorRecord]) { Write-Host "  $($_.Exception.Message)" } else { Write-Host "  $_" } }
    $ErrorActionPreference = $prevEAP
    if ($LASTEXITCODE -ne 0) { Write-Warning "d2 编译失败: $rel"; $failed++; continue }
    $manifest += ('{0}  {1}  {2}' -f (Get-Sha256 $src.FullName), (Get-Sha256 $out), $outRel)
}

if ($failed -gt 0) { Write-Error "$failed 张图编译失败，manifest 未写"; exit 1 }
[System.IO.File]::WriteAllLines((Join-Path $dgrid 'manifest.sha256'), [string[]]$manifest, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("done: {0} 张图已渲染，manifest 写入 diagrams/manifest.sha256" -f $manifest.Count)
