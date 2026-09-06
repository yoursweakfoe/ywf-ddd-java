# requires -Version 5.1
<#
.SYNOPSIS
  check-docs.ps1 - anti-rot machine checks for docs/knowledge/.agents (docs-restructure WP-5, v1)
.DESCRIPTION
  C1 pattern-instantiation  C2 file-count consistency  C3 framework symbols
  C4 business-word neutrality in pedagogy code  C5 exception mapping table parity  C6 decisions append-only
  Exit code = number of failing checks. -SelfTest asserts detection of injected violations.
#>
[CmdletBinding()]
param(
    [switch]$SelfTest,
    [string]$RootPath
)
$ErrorActionPreference = 'Stop'
if (-not $RootPath) {
    $here = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
    $RootPath = Split-Path -Parent $here
}
$root = (Resolve-Path $RootPath).Path

$fails = @{}
function Add-Fail([string]$check, [string]$msg) {
    if (-not $fails.ContainsKey($check)) { $fails[$check] = New-Object System.Collections.ArrayList }
    [void]$fails[$check].Add($msg)
}

function Get-MdPaths([string[]]$entries) {
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($e in $entries) {
        $p = Join-Path $root $e
        if (-not (Test-Path $p)) { continue }
        if (-not (Get-Item $p).PSIsContainer) { $out.Add($p); continue }
        Get-ChildItem $p -Recurse -Filter *.md -Force |
            Where-Object { $_.FullName -notmatch '\\\.omo\\|\\target\\|\\node_modules\\' } |
            ForEach-Object { $out.Add($_.FullName) }
    }
    $out
}

# ---------- inventory ----------
$hasUmbrella = Test-Path (Join-Path $root 'knowledge/docs')
$teachEntries = if ($hasUmbrella) { @('knowledge/docs', '.agents', 'AGENTS.md') } else { @('docs', '.agents', 'AGENTS.md') }
$teachMd = Get-MdPaths $teachEntries
$allMd   = Get-MdPaths (@('knowledge', 'docs', '.agents', 'AGENTS.md', 'README.md'))
$exDocRel = if ($hasUmbrella) { 'knowledge/docs/reference/api/common-exception.md' } else { 'docs/common/common-exception.md' }
$decDirs = @('knowledge/decisions', 'docs/adr') | Where-Object { Test-Path (Join-Path $root $_) }

$sampleBase = Join-Path $root 'sample-application'
$fwBase     = Join-Path $root 'ywf-ddd-common'
$srcJava = @(Get-ChildItem $sampleBase, $fwBase -Recurse -Filter *.java -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\target\\|\\\.omo\\' })

$srcPaths = New-Object System.Collections.Generic.HashSet[string]
$esc = [regex]::Escape($root + '\')
foreach ($item in (Get-ChildItem $sampleBase, $fwBase -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\target\\|\\\.omo\\' })) {
    $rel = ($item.FullName -replace $esc, '') -replace '\\', '/'
    if ($item.PSIsContainer) { [void]$srcPaths.Add($rel + '/') }
    else { [void]$srcPaths.Add($rel) }
}
$classSet = New-Object System.Collections.Generic.HashSet[string]
foreach ($j in $srcJava) { [void]$classSet.Add($j.BaseName) }

$aggList = @(
    (Get-ChildItem $sampleBase -Recurse -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\src\\main\\java\\.*\\domain\\[^\\]+$' -and $_.Name -ne 'shared' } |
        ForEach-Object { $_.Name } | Sort-Object -Unique)
)
"info: aggregates=[$($aggList -join ',')] java-classes=$($classSet.Count) md-scan=$(($allMd).Count)"

$fictional = '^(Payment|Reservation|Stock|Warehouse|Consignment|Shipment|Checkout|Alipay|AliOss|Your|Xxx|Todo|Nonexistent)'
$lineAllow = '@Order\b|Ordered\.|ORDER BY|真实例|实现状态|\bsample\b|sample-application|（如|\(如|e\.g\.'
$pathAllow = 'domain/order|application/order|contract/order|infrastructure/[^`]*order|adapter/task|master/order|domain/product|application/product|contract/product|infrastructure/[^`]*product|master/product'
$classRefs = 'OrderFactory|ProductFactory|OrderRepository|OrderQueryRepository|ProductRepository|ProductQueryRepository|OrderController|ProductController|OrderCO|ProductCO|OrderDTO|ProductDTO|ProductViewDTO|OrderPO|ProductPO|OrderItem|OrderItemDTO|OrderStatus|ProductStatus|OrderAppService|ProductAppService|OrderMapper|ProductMapper|OrderMapper\.xml|OrderConverter|ProductConverter|OrderPresenter|OrderAssembler|ProductPresenter|ProductAssembler|PayOrder|PlaceOrder|CancelOrder|ShipOrder|ShipOrderForm|DeliverOrder|ConfirmOrder|CompleteOrder|CreateProduct|GetOrderPage|GetOrderQuery|GetProductQuery|AutoDeliver|RetryablePlace|OrderAutoDeliverScheduler|OrderQueryRepositoryImpl|ProductQueryRepositoryImpl|ProductFixtures|OrderFixtures|OrdersTable|ProductTable|GetOrderPageHandler|GetProductHandler|OrderControllerImpl|ProductControllerImpl|OrderAggregateTest|OrderAppServiceTest|PayOrderHandlerTest|ConfirmOrderHandler'
$fileSkipC4 = @(
    # (new-test SKILL 豁免已拆除——D6 迁移完成，模板只住 how-to/testing.md)
    'rules/01-project-overview\.md',
    'knowledge/docs/tutorials/',           # operations manual on the real sample (business words inherent, exempt per doctrine)
    'reference/structure\.md',           # generated map of the REAL tree - dir names order/product are facts, not pedagogy
    'glossary\.md',                 # 业务词汇+命名映射节 = 通用语言/真实例之家（§2.1 辖域裁定）
    'docs/README\.md$|/README\.md$'
)
# 外置豁免清单（C3；新增须 PR 评审）
$wl = @()
$wlFile = if ($PSCommandPath) { Join-Path (Split-Path -Parent $PSCommandPath) 'check-docs.whitelist.txt' } else { $null }
if ($wlFile -and (Test-Path $wlFile)) {
    $wl = @(Get-Content $wlFile -Encoding UTF8 | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -notmatch '^#' })
}

function Test-BizCode([string]$s) {
    if ($s -match $lineAllow) { return $false }
    $probe = $s -replace $classRefs, '' -replace $pathAllow, ''
    return ($probe -cmatch '\b(order|Order|product|Product)\b')
}

# ---------- C1 pattern instantiation (directory part only) ----------
foreach ($f in $teachMd) {
    if ($f -match 'AGENTS\.md$') { continue }
    $lines = [System.IO.File]::ReadAllLines($f, [Text.Encoding]::UTF8)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        foreach ($m in [regex]::Matches($lines[$i], '[\w/.{}-]*\{agg\}/[\w/.{}-]*')) {
            $dirPart = $m.Value
            if ($dirPart -match '\.java') { $dirPart = ($dirPart -replace '/[^/]*\.java$', '') }
            $dirPart = (($dirPart -replace '\{[A-Za-z]+\}', '*') -replace '/\*', '/*').TrimEnd('/')
            $dirPart = $dirPart.TrimStart('.')
            if ($dirPart -notmatch '/') { continue }
            foreach ($agg in $aggList) {
                $probe = ($dirPart -replace '\{agg\}', $agg)
                if ($probe -match '\*') { continue }
                $tail = '/' + $probe.TrimStart('/') + '/'
                $hit = $false
                foreach ($sp in $srcPaths) { if ($sp -like "*$tail*") { $hit = $true; break } }
                if (-not $hit) { Add-Fail 'C1' "$($f.Replace($root + '\', '')):L$($i + 1): [$($m.Value)] agg='$agg' dir not found: $probe" }
            }
        }
    }
}

# ---------- C2 file-count vs circled inventory ----------
foreach ($f in ($allMd | Sort-Object -Unique)) {
    $text = [System.IO.File]::ReadAllText($f, [Text.Encoding]::UTF8)
    $decls = [regex]::Matches($text, '(\d{2})\s*个文件|\((\d{2})\s*\+\s*2|\*\*(\d{2})\s*个文件\*\*|=\s*(\d{2})\s*\+\s*2')
    if ($decls.Count -eq 0) { continue }
    $declared = @{}
    foreach ($d in $decls) {
        foreach ($g in 1..4) { $v = $d.Groups[$g].Value; if ($v) { [int]$iv = $v; if ($iv -ge 10) { $declared[$iv] = $true } } }
    }
    $count = 0
    foreach ($cp in (0x2460..0x2473 + 0x3251..0x325F)) { if ($text.Contains([string][char]$cp)) { $count++ } }
    if ($count -eq 0) { continue }
    if (-not $declared.ContainsKey($count)) {
        Add-Fail 'C2' "$($f.Replace($root + '\', '')): circled-inventory=$count but claims=[$(($declared.Keys | Sort-Object) -join ',')] mismatch"
    }
}

# ---------- C3 framework symbols ----------
foreach ($f in $allMd) {
    $text = [System.IO.File]::ReadAllText($f, [Text.Encoding]::UTF8)
    foreach ($m in [regex]::Matches($text, 'com\.yoursweakfoe\.common(?:\.[\w]+)*\.[A-Z]\w*')) {
        $cls = ($m.Value -split '\.')[-1]
        if ($wl | Where-Object { $cls -match $_ }) { continue }
        if (-not $classSet.Contains($cls)) { Add-Fail 'C3' "$($f.Replace($root + '\', '')): FQN $($m.Value) -> class '$cls' absent from sources" }
    }
    foreach ($m in [regex]::Matches($text, '(?<![\w.?!/])([A-Z]\w*(?:Exception|Mapper|AutoConfiguration|TypeHandler|Assembler|Presenter|Persistence|Fixtures))(?![\w.])')) {
        $cls = $m.Groups[1].Value
        if ($cls -match $fictional) { continue }
        if ($classSet.Contains($cls)) { continue }
        if ($wl | Where-Object { $cls -match $_ }) { continue }
        Add-Fail 'C3' "$($f.Replace($root + '\', '')): class token '$cls' not resolvable in sources"
    }
}

# ---------- C4 business neutrality in pedagogy code ----------
foreach ($f in $teachMd) {
    $rel = ($f.Replace($root + '\', '')) -replace '\\', '/'
    $skip = $false
    foreach ($pat in $fileSkipC4) { if ($rel -match $pat) { $skip = $true; break } }
    if ($skip) { continue }
    $lines = [System.IO.File]::ReadAllLines($f, [Text.Encoding]::UTF8)
    $inFence = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $L = $lines[$i]
        if ($L -match '^\s*```') { $inFence = -not $inFence; continue }
        $viol = $false
        if ($inFence) {
            if (Test-BizCode $L) { $viol = $true }
        } elseif ($L -notmatch $lineAllow) {
            foreach ($sp in [regex]::Matches($L, '`[^`\n]+`')) {
                if (Test-BizCode $sp.Value) { $viol = $true; break }
            }
        }
        if ($viol) { Add-Fail 'C4' "$rel`:L$($i + 1): $($L.Trim())" }
    }
}

# ---------- C5 mapping table parity: per-handler coverage ----------
$handlerSrc = Get-ChildItem $fwBase -Recurse -Filter GlobalRestExceptionHandler.java -ErrorAction SilentlyContinue | Select-Object -First 1
$exDoc = Join-Path $root $exDocRel
if ($handlerSrc -and (Test-Path $exDoc)) {
    $hText = [System.IO.File]::ReadAllText($handlerSrc.FullName, [Text.Encoding]::UTF8)
    $docText = [System.IO.File]::ReadAllText($exDoc, [Text.Encoding]::UTF8)
    $covered = 0; $missing = @()
    foreach ($m in [regex]::Matches($hText, '@ExceptionHandler\s*\(([^)]*)\)')) {
        foreach ($t in [regex]::Matches($m.Groups[1].Value, '([\w.]+)\.class')) {
            $name = ($t.Groups[1].Value -split '\.')[-1]
            if ($name -eq 'Exception') { continue }
            if ($docText -match [regex]::Escape($name)) { $covered++ } else { $missing += $name }
        }
    }
    if ($missing.Count) { Add-Fail 'C5' "common-exception 映射表未覆盖处理器: $($missing -join ', ')" }
    else { "C5 info: handlers covered $covered/$(($covered + $missing.Count)) OK" }
}

# ---------- C6 decisions append-only ----------
foreach ($dd in $decDirs) {
    $changed = @(git -C $root diff --name-only HEAD -- $dd 2>$null)
    foreach ($p in $changed) {
        if ($p -replace '\\', '/' -match '(^|/)README\.md$') { continue }
        $diff = @((git -C $root diff HEAD -- $p 2>$null | Out-String) -split "\r?\n")
        $minus = @($diff | Where-Object { $_ -match '^-' -and $_ -notmatch '^---' })
        if ($minus.Count -gt 0) { Add-Fail 'C6' "$p : $($minus.Count) deleted line(s) - decision bodies are append-only (supersede instead)" }
    }
}

# ---------- report ----------
"================ check-docs report ================"
$total = 0
foreach ($k in @('C1', 'C2', 'C3', 'C4', 'C5', 'C6')) {
    $arr = if ($fails.ContainsKey($k)) { @($fails[$k]) } else { @() }
    $verdict = if ($arr.Count) { "FAIL($($arr.Count))" } else { 'PASS' }
    '{0} {1}' -f $k, $verdict
    $arr | Select-Object -First 12 | ForEach-Object { "    - $_" }
    if ($arr.Count -gt 12) { "    ... $($arr.Count - 12) more" }
    if ($arr.Count) { $total++ }
}

# ---------- self-test ----------
if ($SelfTest) {
    "-------------- SelfTest --------------"
    $r1 = $true
    $ghost = 'application/{agg}/repository/application/NonexistentHolderX.java'
    $probeDir = ($ghost -replace '\{agg\}', 'order') -replace '/[^/]*\.java$', ''
    $tail = '/' + $probeDir.TrimStart('./') + '/'
    foreach ($sp in $srcPaths) { if ($sp.Contains($tail)) { $r1 = $false; break } }
    $r2 = Test-BizCode 'Order order = create(); // injected'
    $r3 = -not $classSet.Contains('DDDArchitectureRules')
    "ST1 ghost-path detected:     $(if ($r1) { 'PASS' } else { 'FAIL' })"
    "ST2 business-word detected:   $(if ($r2) { 'PASS' } else { 'FAIL' })"
    "ST3 ghost-class detected:     $(if ($r3) { 'PASS' } else { 'FAIL' })"
    if (-not ($r1 -and $r2 -and $r3)) { $total++ }
}

"================ result: $total failing checks ================"
exit $total
