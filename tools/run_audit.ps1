<#
.SYNOPSIS
  Runs one worldgen audit (Section 11 test gates) in a fresh world on a dedicated dev server.

.EXAMPLE
  .\tools\run_audit.ps1 -Variant vanilla -Label vanilla-a
  .\tools\run_audit.ps1 -Variant modded  -Label modded-a
  .\tools\run_audit.ps1 -Variant vanilla -Label pregen-vanilla -RadiusChunks 63 -NoScan -NoStructures

  Output: runs/audit-<variant>/fu-audit/<label>.json and <label>.log
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('vanilla', 'modded')][string]$Variant,
    [Parameter(Mandatory = $true)][string]$Label,
    [long]$Seed = 1234567890,
    [int]$CenterChunkX = 512,
    [int]$CenterChunkZ = 512,
    [int]$RadiusChunks = 16,
    [int]$TileChunks = 16,
    [int]$StructureSearchRadius = 100,
    [switch]$NoScan,
    [switch]$NoStructures,
    [switch]$NoRegistryDump,
    [switch]$SingleThread,
    [int]$EstimateRadius = 0,
    [switch]$EstimateOnly,
    [switch]$OreMods,
    [switch]$BopMods
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$gameDir = Join-Path $root "runs\audit-$Variant"
New-Item -ItemType Directory -Force $gameDir | Out-Null

# Fresh world every run: determinism and performance numbers are only meaningful from scratch.
$world = Join-Path $gameDir 'world'
if (Test-Path $world) { Remove-Item -Recurse -Force $world }

Set-Content -Encoding ascii (Join-Path $gameDir 'eula.txt') 'eula=true'
@"
level-seed=$Seed
level-name=world
level-type=minecraft\:normal
generate-structures=true
online-mode=false
max-tick-time=-1
view-distance=4
simulation-distance=4
spawn-protection=0
sync-chunk-writes=false
"@ | Set-Content -Encoding ascii (Join-Path $gameDir 'server.properties')

@"
label=$Label
centerChunkX=$CenterChunkX
centerChunkZ=$CenterChunkZ
radiusChunks=$RadiusChunks
tileChunks=$TileChunks
scan=$((-not $NoScan).ToString().ToLower())
structures=$((-not $NoStructures).ToString().ToLower())
structureSearchRadius=$StructureSearchRadius
registryDump=$((-not $NoRegistryDump).ToString().ToLower())
estimateRadius=$EstimateRadius
estimateOnly=$($EstimateOnly.IsPresent.ToString().ToLower())
"@ | Set-Content -Encoding ascii (Join-Path $gameDir 'fu-audit.properties')

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue | Where-Object Name -like 'jdk-25*' | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}

$outDir = Join-Path $gameDir 'fu-audit'
New-Item -ItemType Directory -Force $outDir | Out-Null
Remove-Item (Join-Path $outDir "$Label.json"), (Join-Path $outDir "$Label.log") -ErrorAction SilentlyContinue

$task = if ($Variant -eq 'vanilla') { 'runAuditVanilla' } else { 'runAuditModded' }
$sw = [Diagnostics.Stopwatch]::StartNew()
# Windows PowerShell turns native stderr into terminating errors under 'Stop', so relax it for the
# Gradle call and keep Gradle's console output in a file. -p pins the project directory.
$gradleOut = Join-Path $gameDir "gradle-$Label.out"
$ErrorActionPreference = 'Continue'
$gradleArgs = @('-p', $root, $task, '--no-configuration-cache', '-q')
if ($SingleThread) { $gradleArgs += '-PfuSingleThread' }
if ($OreMods) { $gradleArgs += '-PfuOreMods' }
if ($BopMods) { $gradleArgs += '-PfuBopMods' }
& (Join-Path $root 'gradlew.bat') @gradleArgs 2>&1 | Out-File -Encoding utf8 $gradleOut
$gradleExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($gradleExit -ne 0) { Write-Output "Gradle exited with $gradleExit (see $gradleOut)" }
$sw.Stop()

$log = Join-Path $gameDir 'logs\latest.log'
if (Test-Path $log) { Copy-Item $log (Join-Path $outDir "$Label.log") -Force }
Remove-Item (Join-Path $gameDir 'fu-audit.properties') -ErrorAction SilentlyContinue

$json = Join-Path $outDir "$Label.json"
if (Test-Path $json) {
    Write-Output "Audit '$Label' complete in $([int]$sw.Elapsed.TotalMinutes) min -> $json"
} else {
    Write-Output "Audit '$Label' did NOT produce $json - check $outDir\$Label.log"
    exit 1
}
