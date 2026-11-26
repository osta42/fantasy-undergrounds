<#
.SYNOPSIS
  Runs the full Section 11 gate suite: two vanilla and two modded audits (1,024 chunks each,
  fresh worlds, same seed), plus a 2,016 x 2,016-block pre-generation run for each, then
  writes the comparison to reports/<Tag>.md.

.EXAMPLE
  .\tools\run_gates.ps1 -Tag phase0
  .\tools\run_gates.ps1 -Tag phase1 -SkipVanilla   # reuse existing vanilla baselines
#>
param(
    [Parameter(Mandatory = $true)][string]$Tag,
    [switch]$SkipVanilla,
    [switch]$SkipPregen,
    [int]$RadiusChunks = 32,
    [switch]$NoOreMods
)
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$audit = Join-Path $PSScriptRoot 'run_audit.ps1'

$jobs = @()
if (-not $SkipVanilla) {
    $jobs += , @('vanilla', 'vanilla-a', @{})
    $jobs += , @('vanilla', 'vanilla-b', @{NoStructures = $true; NoRegistryDump = $true})
}
# Ore baseline: the same deep world (floor y -128) with this mod's caves switched off.
$deepBaseline = Join-Path $root 'runs\audit-modded\fu-audit\deep-baseline.json'
if (-not $SkipVanilla -or -not (Test-Path $deepBaseline)) {
    $jobs += , @('modded', 'deep-baseline', @{FuDisabled = $true; NoRegistryDump = $true})
}
$jobs += , @('modded', "$Tag-modded-a", @{})
$jobs += , @('modded', "$Tag-modded-b", @{NoStructures = $true; NoRegistryDump = $true})
if (-not $SkipPregen) {
    if (-not $SkipVanilla) { $jobs += , @('vanilla', 'pregen-vanilla', @{RadiusChunks = 63; NoScan = $true; NoStructures = $true; NoRegistryDump = $true}) }
    $jobs += , @('modded', "$Tag-pregen-modded", @{RadiusChunks = 63; NoScan = $true; NoStructures = $true; NoRegistryDump = $true})
}

foreach ($j in $jobs) {
    $variant, $label, $extra = $j
    $extra = $extra.Clone()
    if (-not $extra.ContainsKey('RadiusChunks')) { $extra['RadiusChunks'] = $RadiusChunks }
    if (-not $NoOreMods) { $extra['OreMods'] = $true }
    Write-Output "=== $label ($variant) $(Get-Date -Format HH:mm:ss)"
    & $audit -Variant $variant -Label $label @extra
}

$v = Join-Path $root 'runs\audit-vanilla\fu-audit'
$m = Join-Path $root 'runs\audit-modded\fu-audit'
$reports = Join-Path $root 'reports'
New-Item -ItemType Directory -Force $reports | Out-Null

Write-Output "=== vanilla self-determinism"
python (Join-Path $PSScriptRoot 'compare_audit.py') "$v\vanilla-a.json" "$v\vanilla-a.json" --candidate2 "$v\vanilla-b.json" --md "$reports\vanilla-self.md" | Out-Null
Write-Output "=== $Tag main comparison"
python (Join-Path $PSScriptRoot 'compare_audit.py') "$v\vanilla-a.json" "$m\$Tag-modded-a.json" --candidate2 "$m\$Tag-modded-b.json" --baseline2 "$v\vanilla-b.json" --ore-baseline "$m\deep-baseline.json" --md "$reports\$Tag.md"
if (-not $SkipPregen) {
    Write-Output "=== $Tag pre-generation comparison"
    python (Join-Path $PSScriptRoot 'compare_audit.py') "$v\pregen-vanilla.json" "$m\$Tag-pregen-modded.json" --md "$reports\$Tag-pregen.md"
}
