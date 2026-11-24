#!/usr/bin/env bash
# Runs one worldgen audit (Section 11 test gates) in a fresh world on a dedicated dev server.
# Linux/macOS port of tools/run_audit.ps1; same options, same outputs.
#
# Examples:
#   tools/run_audit.sh --variant vanilla --label vanilla-a
#   tools/run_audit.sh --variant modded  --label modded-a
#   tools/run_audit.sh --variant vanilla --label pregen-vanilla --radius-chunks 63 --no-scan --no-structures
#
# Output: runs/audit-<variant>/fu-audit/<label>.json and <label>.log
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: tools/run_audit.sh --variant vanilla|modded --label LABEL [options]

  --seed N                     world seed (default 1234567890)
  --center-chunk-x N           region centre, chunk X (default 512)
  --center-chunk-z N           region centre, chunk Z (default 512)
  --radius-chunks N            region radius in chunks (default 16 = 32x32 chunks)
  --tile-chunks N              tile size in chunks (default 16)
  --structure-search-radius N  locate radius for the structure probe (default 100)
  --no-scan                    skip the block scan (pre-generation runs)
  --no-structures              skip the structure probe
  --no-registry-dump           skip the biome/feature registry dump
  --single-thread              one worldgen worker (-PfuSingleThread)
  --estimate-radius N          density estimate radius in blocks (default 0 = off)
  --estimate-only              only run the density estimate
  --ore-mods                   load the third-party ore mods (-PfuOreMods)
EOF
}

variant='' label='' seed=1234567890 cx=512 cz=512 radius=16 tile=16 ssr=100
scan=true structures=true registry=true single=false estimate_radius=0 estimate_only=false ore_mods=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --variant) variant="$2"; shift 2 ;;
        --label) label="$2"; shift 2 ;;
        --seed) seed="$2"; shift 2 ;;
        --center-chunk-x) cx="$2"; shift 2 ;;
        --center-chunk-z) cz="$2"; shift 2 ;;
        --radius-chunks) radius="$2"; shift 2 ;;
        --tile-chunks) tile="$2"; shift 2 ;;
        --structure-search-radius) ssr="$2"; shift 2 ;;
        --no-scan) scan=false; shift ;;
        --no-structures) structures=false; shift ;;
        --no-registry-dump) registry=false; shift ;;
        --single-thread) single=true; shift ;;
        --estimate-radius) estimate_radius="$2"; shift 2 ;;
        --estimate-only) estimate_only=true; shift ;;
        --ore-mods) ore_mods=true; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done
if [[ "$variant" != vanilla && "$variant" != modded ]] || [[ -z "$label" ]]; then
    usage >&2; exit 2
fi

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
game_dir="$root/runs/audit-$variant"
mkdir -p "$game_dir"

# Fresh world every run: determinism and performance numbers are only meaningful from scratch.
rm -rf "$game_dir/world"

echo 'eula=true' > "$game_dir/eula.txt"
cat > "$game_dir/server.properties" <<EOF
level-seed=$seed
level-name=world
level-type=minecraft\:normal
generate-structures=true
online-mode=false
max-tick-time=-1
view-distance=4
simulation-distance=4
spawn-protection=0
sync-chunk-writes=false
EOF

cat > "$game_dir/fu-audit.properties" <<EOF
label=$label
centerChunkX=$cx
centerChunkZ=$cz
radiusChunks=$radius
tileChunks=$tile
scan=$scan
structures=$structures
structureSearchRadius=$ssr
registryDump=$registry
estimateRadius=$estimate_radius
estimateOnly=$estimate_only
EOF

# Minecraft 26.2 needs Java 25. Use JAVA_HOME if set, else the first JDK 25 in the usual places.
if [[ -z "${JAVA_HOME:-}" ]]; then
    for jdk in /usr/lib/jvm/java-25-* /usr/lib/jvm/temurin-25* /usr/lib/jvm/jdk-25* \
               /Library/Java/JavaVirtualMachines/*25*/Contents/Home; do
        if [[ -x "$jdk/bin/java" ]]; then export JAVA_HOME="$jdk"; break; fi
    done
fi

out_dir="$game_dir/fu-audit"
mkdir -p "$out_dir"
rm -f "$out_dir/$label.json" "$out_dir/$label.log"

task=runAuditVanilla
[[ "$variant" == modded ]] && task=runAuditModded
gradle_args=(-p "$root" "$task" --no-configuration-cache -q)
[[ "$single" == true ]] && gradle_args+=(-PfuSingleThread)
[[ "$ore_mods" == true ]] && gradle_args+=(-PfuOreMods)

# Keep Gradle's console output in a file, next to the run.
gradle_out="$game_dir/gradle-$label.out"
start=$SECONDS
set +e
"$root/gradlew" "${gradle_args[@]}" > "$gradle_out" 2>&1
gradle_exit=$?
set -e
[[ $gradle_exit -ne 0 ]] && echo "Gradle exited with $gradle_exit (see $gradle_out)"
elapsed_min=$(( (SECONDS - start + 30) / 60 ))

log="$game_dir/logs/latest.log"
[[ -f "$log" ]] && cp -f "$log" "$out_dir/$label.log"
rm -f "$game_dir/fu-audit.properties"

json="$out_dir/$label.json"
if [[ -f "$json" ]]; then
    echo "Audit '$label' complete in $elapsed_min min -> $json"
else
    echo "Audit '$label' did NOT produce $json - check $out_dir/$label.log"
    exit 1
fi
