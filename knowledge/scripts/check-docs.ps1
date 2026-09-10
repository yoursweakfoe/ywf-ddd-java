# requires -Version 5.1
<#
.SYNOPSIS
  check-docs.ps1 - anti-rot machine checks for docs/knowledge/.agents (docs-restructure WP-5, v1)
.DESCRIPTION
  C1 pattern-instantiation  C2 file-count consistency  C3 framework symbols
  C4 business-word neutrality in pedagogy code  C5 exception mapping table parity  C6 dossier zones, two arms:
    freeze arm = body files append-only (decisions bodies allow Status-line 1:1 swap per charter; archive bodies any minus = red; README exempt)
    cull arm   = (a) whole-file body deletions allowed only under a cull bill (proposal.md matching 清册, in-flight on disk OR self-immolated in this diff - see attribution-law §4) AND the zone must be fully emptied (no partial cull);
                 (b) empty-dossier self-consistency: when a dossier is empty, no concrete identifier (ADR-\d{4} / date-slug) may remain anywhere on the living surface
  C7 skill-workspace conformance (.agents residents whitelist + SKILL.md spec gates: name==dir, desc<=1024, body<=500)
  Exit code = number of failing checks. -SelfTest asserts detection of injected violations.
.NOTES
  Sole explanation entry: knowledge/docs/reference/doc-guards.md
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
    while ($RootPath -and -not (Test-Path (Join-Path $RootPath 'AGENTS.md'))) { $RootPath = Split-Path -Parent $RootPath }
    if (-not $RootPath) { throw 'repo root (AGENTS.md marker) not found above script location' }
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
$teachEntries = if ($hasUmbrella) { @('knowledge/docs', '.agents', 'knowledge/specs/current', 'AGENTS.md') } else { @('docs', '.agents', 'AGENTS.md') }
$teachMd = Get-MdPaths $teachEntries
$allMd   = Get-MdPaths (@('knowledge', 'docs', '.agents', 'sample-application/specs', 'AGENTS.md', 'README.md'))
$exDocRel = if ($hasUmbrella) { 'knowledge/docs/reference/api/common-exception.md' } else { 'docs/common/common-exception.md' }
$decDirs = @(@('knowledge/decisions', 'docs/adr') | Where-Object { Test-Path (Join-Path $root $_) })

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
    '根 README-project-overview\.md',
    'knowledge/docs/tutorials/',           # operations manual on the real sample (business words inherent, exempt per doctrine)
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

# ---------- C6 dossier freeze + cull self-consistency (two arms; 历元一清册案 L1；首期清册案增自焚识别) ----------
# git may write harmless warnings (CRLF etc.) to stderr; PS5.1 EAP=Stop turns native stderr into a terminating
# error even with 2>$null — downgrade for this block, restore at the end.
$eapPrev = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
$dossierZones = @($decDirs + @('knowledge/specs/archive', 'sample-application/specs/archive') |
    Select-Object -Unique | Where-Object { Test-Path (Join-Path $root $_) })
function Get-DossierBodyNames([string]$zone) {
    $zp = Join-Path $root $zone
    if (-not (Test-Path $zp)) { return @() }
    if ($zone -match 'archive') { @(Get-ChildItem $zp -Directory -Force -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }) }
    else { @(Get-ChildItem $zp -Filter 'ADR-*.md' -File -Force -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }) }
}
$adrIdRx = 'ADR-\d{4}'
$slugRx  = '\d{4}-\d{2}-[a-z][a-z0-9]*(-[a-z0-9]+)+'
# cull bill in-flight = any live (non-template) proposal under changes/ that legislates a cull
$cullBill = @(Get-ChildItem (Join-Path $root 'knowledge/specs/changes'), (Join-Path $root 'sample-application/specs/changes') -Filter 'proposal.md' -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '_template' -and ((Get-Content $_.FullName -Raw -Encoding UTF8) -match '清册') }).Count -gt 0
# self-immolating cull bill (attribution-law §4): the bill deletes its own dir as terminal act, so at final-gate
# time no proposal is on disk. Evidence = a proposal.md under changes/ deleted in this diff whose HEAD content legislates a cull.
if (-not $cullBill) {
    $cullBill = @(git -C $root diff HEAD --name-only --diff-filter=D -- 'knowledge/specs/changes' 'sample-application/specs/changes' 2>$null |
        Where-Object { $_ -match '(^|/)proposal\.md$' -and $_ -notmatch '_template' } |
        Where-Object { (((git -C $root show "HEAD:$_" 2>$null) | Out-String)) -match '清册' }).Count -gt 0
}

# --- freeze arm: in-place minus lines = red (decisions bodies: Status-line 1:1 swap exempt per charter) ---
$freezeChecked = 0
foreach ($z in $dossierZones) {
    foreach ($p in @(git -C $root diff HEAD --name-only --diff-filter=M -- $z 2>$null)) {
        $relP = $p -replace '\\', '/'
        if ($relP -match '(^|/)README\.md$') { continue }
        $freezeChecked++
        $diff = @((git -C $root diff HEAD -- $p 2>$null | Out-String) -split "\r?\n")
        $minus = @($diff | Where-Object { $_ -match '^-' -and $_ -notmatch '^---' })
        if ($minus.Count -eq 0) { continue }
        $isDec = @($decDirs | Where-Object { $relP -match ('^' + [regex]::Escape($_) + '/') }).Count -gt 0
        if ($isDec) {
            $bad = @($minus | Where-Object { $_ -notmatch '^-\*\*Status\*\*' })
            $plusStatus = @($diff | Where-Object { $_ -match '^\+\*\*Status\*\*' })
            if ($bad.Count -eq 0 -and $plusStatus.Count -eq $minus.Count) {
                "C6 info: freeze arm - Status-line supersede swap allowed: $relP ($($minus.Count))"
                continue
            }
            Add-Fail 'C6' "$relP : 在位涂改 $($minus.Count) line(s) - 冻结臂：除 Status 行 1:1 对替外零容忍；supersede 走新立 ADR + 索引表状态列"
        } else {
            Add-Fail 'C6' "$relP : $($minus.Count) deleted line(s) - 案卷本体在位不改（冻结臂）"
        }
    }
}
# --- cull arm (a): whole-file body deletions only under an in-flight cull bill, and only as total emptying ---
foreach ($z in $dossierZones) {
    $dels = @(git -C $root diff HEAD --name-only --diff-filter=D -- $z 2>$null | Where-Object { $_ -notmatch '(^|/)README\.md$' })
    if ($dels.Count -eq 0) { continue }
    if (-not $cullBill) { Add-Fail 'C6' "$z : $($dels.Count) body file(s) deleted but no cull bill (on-disk in-flight or self-immolated in diff; proposal 含「清册」) - 清册臂(a)"; continue }
    $remain = Get-DossierBodyNames $z
    if ($remain.Count -gt 0) { Add-Fail 'C6' "$z : 清册必须整册归零，删后仍余 $($remain.Count) 件 - 禁拆件/择留（清册臂(a)）" }
    else { "C6 info: $z emptied under in-flight cull bill - 清册臂(a) OK" }
}
# --- cull arm (b): empty dossier ⇒ zero concrete identifiers on the living surface ---
$zoneSelfRx = '^knowledge/(decisions|specs/(changes|archive))/|^docs/adr/|^sample-application/specs/(changes|archive)/'
$decBodies = @(Get-DossierBodyNames 'knowledge/decisions')
$decEmpty = ($decBodies.Count) -eq 0
$arcEmpty = (@(Get-DossierBodyNames 'knowledge/specs/archive')).Count -eq 0 -and (@(Get-DossierBodyNames 'sample-application/specs/archive')).Count -eq 0
$idHits = @()
if ($decEmpty -or $arcEmpty) {
    $idTargets = @($allMd | Where-Object { (($_.Replace($root + '\', '')) -replace '\\', '/') -notmatch $zoneSelfRx } | Sort-Object -Unique)
    foreach ($f in $idTargets) {
        $rel = (($f.Replace($root + '\', '')) -replace '\\', '/')
        $t = [System.IO.File]::ReadAllText($f, [Text.Encoding]::UTF8)
        if ($decEmpty) { foreach ($m in [regex]::Matches($t, $adrIdRx)) { $idHits += "$rel : $($m.Value)" } }
        if ($arcEmpty) { foreach ($m in [regex]::Matches($t, $slugRx))  { $idHits += "$rel : slug $($m.Value)" } }
    }
}
if ($idHits.Count) {
    Add-Fail 'C6' "空册自洽被破：活面残留该期具体标识符 $($idHits.Count) 处（首5：$(($idHits | Select-Object -First 5) -join ' | ')）- 清册臂(b)"
}
"C6 info: freeze-arm-modified=$freezeChecked; cull-bill-in-flight=$cullBill; decisions=$(if ($decEmpty) { 'EMPTY' } else { "$($decBodies.Count) bodies" }); archive=$(if ($arcEmpty) { 'EMPTY' } else { 'OCCUPIED' }); active-id-residue=$($idHits.Count)"
$ErrorActionPreference = $eapPrev

# ---------- C7 skill-workspace conformance (agents-workspace L2) ----------
$namePat = '^[a-z0-9]+(-[a-z0-9]+)*$'
$agentsRoot = Join-Path $root '.agents'
if (Test-Path $agentsRoot) {
    $allowedTop = @('README.md', 'skills', 'memory', 'logs')
    foreach ($e in (Get-ChildItem $agentsRoot -Force)) {
        if ($allowedTop -notcontains $e.Name) { Add-Fail 'C7' ".agents unauthorized resident: $($e.Name) (whitelist: README.md + skills/ + gitignored memory|logs)" }
    }
    $skillRoot = Join-Path $agentsRoot 'skills'
    if (Test-Path $skillRoot) {
        foreach ($sf in (Get-ChildItem $skillRoot -File -Force)) {
            Add-Fail 'C7' ".agents/skills stray file: $($sf.Name) (only skill dirs allowed)"
        }
        foreach ($d in (Get-ChildItem $skillRoot -Directory -Force)) {
            $sk = Join-Path $d.FullName 'SKILL.md'
            if (-not (Test-Path $sk)) { Add-Fail 'C7' "skill dir '$($d.Name)' missing SKILL.md"; continue }
            $lines = [System.IO.File]::ReadAllLines($sk, [Text.Encoding]::UTF8)
            if ($lines.Count -gt 0 -and $lines[0].Length -gt 0 -and [int][char]$lines[0][0] -eq 0xFEFF) { $lines[0] = $lines[0].Substring(1) }
            if ($lines.Count -lt 2 -or $lines[0] -notmatch '^---\s*$') { Add-Fail 'C7' "$($d.Name)/SKILL.md: missing YAML frontmatter"; continue }
            $end = -1
            for ($i = 1; $i -lt $lines.Count; $i++) { if ($lines[$i] -match '^---\s*$') { $end = $i; break } }
            if ($end -lt 0) { Add-Fail 'C7' "$($d.Name)/SKILL.md: frontmatter not closed"; continue }
            $name = ''; $desc = ''
            for ($i = 1; $i -lt $end; $i++) {
                if ($lines[$i] -match '^name\s*:\s*(.+)$') { $name = $Matches[1].Trim().Trim('"').Trim("'") }
                if ($lines[$i] -match '^description\s*:\s*(.+)$') { $desc = $Matches[1].Trim().Trim('"').Trim("'") }
            }
            if ($name -cnotmatch $namePat -or $name.Length -gt 64) { Add-Fail 'C7' "$($d.Name)/SKILL.md: name '$name' violates spec (^[a-z0-9-]+$ no lead/trail/consecutive hyphen, <=64)" }
            if ($name -ne $d.Name) { Add-Fail 'C7' "$($d.Name)/SKILL.md: name '$name' != directory name" }
            if (-not $desc) { Add-Fail 'C7' "$($d.Name)/SKILL.md: description empty" }
            elseif ($desc.Length -gt 1024) { Add-Fail 'C7' "$($d.Name)/SKILL.md: description length $($desc.Length) > 1024" }
            if (($lines.Count - $end - 1) -gt 500) { Add-Fail 'C7' "$($d.Name)/SKILL.md: body > 500 lines (spec cap)" }
        }
    }
}

# ---------- report ----------
"================ check-docs report ================"
$total = 0
foreach ($k in @('C1', 'C2', 'C3', 'C4', 'C5', 'C6', 'C7')) {
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
    $r4 = ('valid-name1' -cmatch $namePat) -and -not ('Bad_Name' -cmatch $namePat)
    $r5 = ('ADR-0099' -match $adrIdRx) -and -not ('ADR-NNNN' -match $adrIdRx) -and -not ('ADR-000N' -match $adrIdRx)
    $r6 = ('2026-99-demo-cull' -match $slugRx) -and -not ('2026-09-06' -match $slugRx) -and -not ('2026-09' -match $slugRx)
    "ST1 ghost-path detected:     $(if ($r1) { 'PASS' } else { 'FAIL' })"
    "ST2 business-word detected:   $(if ($r2) { 'PASS' } else { 'FAIL' })"
    "ST3 ghost-class detected:     $(if ($r3) { 'PASS' } else { 'FAIL' })"
    "ST4 skill-name pattern enforced: $(if ($r4) { 'PASS' } else { 'FAIL' })"
    "ST5 cull-id regex (ADR digits vs placeholder): $(if ($r5) { 'PASS' } else { 'FAIL' })"
    "ST6 date-slug regex (vs ISO dates): $(if ($r6) { 'PASS' } else { 'FAIL' })"
    if (-not ($r1 -and $r2 -and $r3 -and $r4 -and $r5 -and $r6)) { $total++ }
}

"================ result: $total failing checks ================"
exit $total
