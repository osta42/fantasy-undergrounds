#!/usr/bin/env bash
# Runs the full Section 11 gate suite: two vanilla and two modded audits (fresh worlds, same
# seed), plus a 2,016 x 2,016-block pre-generation run for each, then writes the comparison to
# reports/<tag>.md. Linux/macOS port of tools/run_gates.ps1; same jobs, same outputs.
#
# Examples:
#   tools/run_gates.sh --tag phase0
#   tools/run_gates.sh --tag phase1 --skip-vanilla   # reuse existing vanilla baselines
set -uo pipefail

usage() {
    cat <<'EOF'
Usage: tools/run_gates.sh --tag TAG [options]

  --skip-vanilla       reuse the existing vanilla-a/-b and pregen-vanilla results
  --skip-pregen        skip both 2,016 x 2,016-block pre-generation runs
  --radius-chunks N    audit radius in chunks (default 32 = 64x64 chunks)
  --no-ore-mods        do not load the third-party ore mods
EOF
}

tag='' skip_vanilla=false skip_pregen=false radius=32 ore_mods=true
while [[ $# -gt 0 ]]; do
    case "$1" in
        --tag) tag="$2"; shift 2 ;;
        --skip-vanilla) skip_vanilla=true; shift ;;
        --skip-pregen) skip_pregen=true; shift ;;
        --radius-chunks) radius="$2"; shift 2 ;;
        --no-ore-mods) ore_mods=false; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done
[[ -z "$tag" ]] && { usage >&2; exit 2; }

tools="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(dirname "$tools")"
audit="$tools/run_audit.sh"
python="$(command -v python3 || command -v python)"

pregen=(--radius-chunks 63 --no-scan --no-structures --no-registry-dump)
second=(--no-structures --no-registry-dump)

# Each job: variant, label, extra options. Every audit run gets --radius-chunks unless the job
# sets its own, and --ore-mods unless --no-ore-mods was given.
run() {
    local variant="$1" label="$2"; shift 2
    local extra=("$@")
    [[ " ${extra[*]} " == *" --radius-chunks "* ]] || extra+=(--radius-chunks "$radius")
    [[ "$ore_mods" == true ]] && extra+=(--ore-mods)
    echo "=== $label ($variant) $(date +%H:%M:%S)"
    "$audit" --variant "$variant" --label "$label" "${extra[@]}"
}

if [[ "$skip_vanilla" == false ]]; then
    run vanilla vanilla-a
    run vanilla vanilla-b "${second[@]}"
fi
# Ore baseline: the same deep world (floor y -128) with this mod's caves switched off.
[[ "$skip_vanilla" == false || ! -f "$root/runs/audit-modded/fu-audit/deep-baseline.json" ]] \
    && run modded deep-baseline --fu-disabled --no-registry-dump
run modded "$tag-modded-a"
run modded "$tag-modded-b" "${second[@]}"
if [[ "$skip_pregen" == false ]]; then
    [[ "$skip_vanilla" == false ]] && run vanilla pregen-vanilla "${pregen[@]}"
    run modded "$tag-pregen-modded" "${pregen[@]}"
fi

v="$root/runs/audit-vanilla/fu-audit"
m="$root/runs/audit-modded/fu-audit"
reports="$root/reports"
mkdir -p "$reports"

echo "=== vanilla self-determinism"
"$python" "$tools/compare_audit.py" "$v/vanilla-a.json" "$v/vanilla-a.json" \
    --candidate2 "$v/vanilla-b.json" --md "$reports/vanilla-self.md" > /dev/null
echo "=== $tag main comparison"
"$python" "$tools/compare_audit.py" "$v/vanilla-a.json" "$m/$tag-modded-a.json" \
    --candidate2 "$m/$tag-modded-b.json" --baseline2 "$v/vanilla-b.json" \
    --ore-baseline "$m/deep-baseline.json" --md "$reports/$tag.md"
if [[ "$skip_pregen" == false ]]; then
    echo "=== $tag pre-generation comparison"
    "$python" "$tools/compare_audit.py" "$v/pregen-vanilla.json" "$m/$tag-pregen-modded.json" \
        --md "$reports/$tag-pregen.md"
fi
