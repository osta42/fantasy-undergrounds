#!/usr/bin/env python3
"""Compare two fu-audit JSON files and evaluate the Section 11 gates.

Usage:
  python tools/compare_audit.py BASELINE.json CANDIDATE.json [--candidate2 CANDIDATE_B.json]
                                [--ore-tolerance 15] [--perf-tolerance 30] [--md OUT.md]

  BASELINE    run without Fantasy Undergrounds (same seed, same other mods)
  CANDIDATE   run with Fantasy Undergrounds
  --candidate2  second fresh-world run of the candidate, for the determinism gate (gate 5)

Log files named <label>.log next to each JSON are scanned for gate 4 (exceptions and
"Detected setBlock in a far chunk").
"""
import argparse
import json
import os
import re
import sys

FAR_CHUNK = "Detected setBlock in a far chunk"
UNSAFE_READ = "Detected unsafe terrain read during worldgen"
OWN_NAMESPACE = "fantasyundergrounds:"
EXC_RE = re.compile(r"(Exception|Error)(:|\s+at\s)|^\s+at\s+[\w.$]+\(", re.M)
STRUCT_INTACT_RATIO = 0.95


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def log_for(json_path):
    p = os.path.splitext(json_path)[0] + ".log"
    if not os.path.exists(p):
        return None
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()


# Known-benign dev-environment noise (OSHI cannot read Windows perf counters).
BENIGN = ("oshi.", "Perflib", "OSProcess")
SIG_RE = re.compile(r"^((?:[a-z]\w*\.)+[A-Z]\w*(?:Exception|Error)):?\s?(.*)$")


def log_stats(text):
    """Gate 4 inputs: far-chunk warnings and the set of exception signatures in the log."""
    if text is None:
        return None
    far = text.count(FAR_CHUNK)
    # Worldgen boundary violations, attributed to the feature that caused them.
    by_feature = {}
    own = 0
    for l in text.splitlines():
        if FAR_CHUNK in l or UNSAFE_READ in l:
            m = re.search(r"currently generating: ResourceKey\[[^\]]*? / ([^\]]+)\]", l)
            feat = m.group(1) if m else "<unknown>"
            by_feature[feat] = by_feature.get(feat, 0) + 1
            if OWN_NAMESPACE in l:
                own += 1
    error_lines = [l for l in text.splitlines() if "/ERROR]" in l and not any(b in l for b in BENIGN)]
    sigs = set()
    for l in text.splitlines():
        m = SIG_RE.match(l.strip())
        if m and not any(b in l for b in BENIGN):
            sigs.add(m.group(1) + ": " + re.sub(r"\d+", "#", m.group(2))[:160])
    return {"far_chunk_warnings": far, "own_violations": own, "violations_by_feature": by_feature,
            "error_lines": len(error_lines), "signatures": sigs,
            "first_errors": error_lines[:5]}


def pct(a, b):
    if b == 0:
        return None if a == 0 else float("inf")
    return 100.0 * (a - b) / b


def ore_gate(base, cand, tol):
    rows, ok = [], True
    bo, co = base["scan"]["ores"], cand["scan"]["ores"]
    for ore in sorted(set(bo) | set(co)):
        b, c = bo.get(ore, 0), co.get(ore, 0)
        d = pct(c, b)
        # Ores too rare to measure (fewer than 50 blocks in baseline) are reported, not gated.
        gated = b >= 50
        passed = (d is not None and abs(d) <= tol) if gated else True
        ok &= passed
        rows.append((ore, b, c, d, gated, passed))
    return ok, rows


def registry_gate(base, cand):
    """Every (biome, step, feature) and carver in baseline must still exist in candidate, in the
    same relative order. Additions are allowed."""
    problems = []
    bb, cb = base["registry"]["biomes"], cand["registry"]["biomes"]
    for biome, bdata in bb.items():
        cdata = cb.get(biome)
        if cdata is None:
            problems.append(f"biome missing: {biome}")
            continue
        for step, bfeats in bdata["features"].items():
            cfeats = cdata["features"].get(step, [])
            it = iter(cfeats)
            # subsequence check preserves relative order
            missing = [f for f in bfeats if f not in cfeats]
            if missing:
                problems.append(f"{biome} {step}: missing {missing}")
            elif not all(any(f == x for x in it) for f in bfeats):
                problems.append(f"{biome} {step}: order changed")
    bp, cp = set(base["registry"]["placedFeatures"]), set(cand["registry"]["placedFeatures"])
    for f in sorted(bp - cp):
        problems.append(f"placed feature unregistered: {f}")
    removed_carvers = []
    for biome, bdata in bb.items():
        cdata = cb.get(biome)
        if cdata:
            gone = [c for c in bdata["carvers"] if c not in cdata["carvers"]]
            if gone:
                removed_carvers.append((biome, gone))
    return not problems, problems, removed_carvers


def structure_gate(base, cand):
    rows, ok = [], True
    for name, b in base.get("structures", {}).items():
        c = cand.get("structures", {}).get(name, {})
        found = bool(c.get("found")) and bool(c.get("startValid"))
        bt, ct = b.get("structureBlockTotal", 0), c.get("structureBlockTotal", 0)
        same_pos = b.get("pos") == c.get("pos")
        ratio = (ct / bt) if bt else None
        passed = found and (ratio is None or ratio >= STRUCT_INTACT_RATIO)
        ok &= passed
        rows.append((name, b.get("pos"), c.get("pos"), same_pos, bt, ct, ratio, passed))
    return ok, rows


def determinism(a, b):
    """Compare two fresh-world runs. Returns full-chunk, per-section-band and terrain-only results."""
    ha, hb = a["scan"]["chunkHashes"], b["scan"]["chunkHashes"]
    keys = sorted(set(ha) | set(hb))
    diff = [k for k in keys if ha.get(k) != hb.get(k)]
    # Section index 0 is y=-64..-49 in the Overworld; bucket mismatching sections by 16-block band.
    bands = {}
    sa, sb = a["scan"].get("sectionHashes", {}), b["scan"].get("sectionHashes", {})
    sec_total = sec_diff = 0
    for k in keys:
        la, lb = sa.get(k), sb.get(k)
        if not la or not lb:
            continue
        for i, (x, y) in enumerate(zip(la, lb)):
            sec_total += 1
            if x != y:
                sec_diff += 1
                band = -64 + 16 * i
                bands[band] = bands.get(band, 0) + 1
    ta, tb = a.get("terrainHashes", {}), b.get("terrainHashes", {})
    tkeys = sorted(set(ta) | set(tb))
    tdiff = [k for k in tkeys if ta.get(k) != tb.get(k)]
    return {"chunks": len(keys), "diff": diff, "sections": sec_total, "section_diff": sec_diff,
            "bands": dict(sorted(bands.items())), "terrain_chunks": len(tkeys), "terrain_diff": tdiff}


def fmt(x, nd=1):
    if x is None:
        return "n/a"
    if isinstance(x, float):
        if x == float("inf"):
            return "inf"
        return f"{x:+.{nd}f}" if nd and x != 0 else f"{x:.{nd}f}"
    return str(x)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("baseline")
    ap.add_argument("candidate")
    ap.add_argument("--candidate2")
    ap.add_argument("--baseline2", help="second fresh-world vanilla run, to measure vanilla's own variation")
    ap.add_argument("--ore-tolerance", type=float, default=15.0)
    ap.add_argument("--perf-tolerance", type=float, default=30.0)
    ap.add_argument("--md")
    args = ap.parse_args()

    base, cand = load(args.baseline), load(args.candidate)
    out = []
    w = out.append
    w(f"# Audit comparison: `{base['label']}` (baseline) vs `{cand['label']}`\n")
    w(f"Seed {base['seed']} / {cand['seed']}; chunks {base['performance']['chunks']} / {cand['performance']['chunks']}\n")
    if base["seed"] != cand["seed"]:
        w("**WARNING: seeds differ - comparison invalid.**\n")
    gates = {}

    # Gate 2 - ores
    if "scan" in base and "scan" in cand:
        ok, rows = ore_gate(base, cand, args.ore_tolerance)
        gates["2 Ore audit"] = ok
        w(f"\n## Gate 2 - ore audit (tolerance +/-{args.ore_tolerance:.0f}%, ores with >=50 baseline blocks gated)\n")
        w("| ore | baseline | candidate | change % | gated | pass |\n|---|---:|---:|---:|:-:|:-:|")
        for ore, b, c, d, g, p in rows:
            w(f"| {ore} | {b} | {c} | {fmt(d)} | {'yes' if g else 'no'} | {'PASS' if p else 'FAIL'} |")

    # Gate 3 - performance
    bp, cp = base["performance"]["msPerChunk"], cand["performance"]["msPerChunk"]
    d = pct(cp, bp)
    gates["3 Performance"] = d is not None and d <= args.perf_tolerance
    w(f"\n## Gate 3 - performance (<= +{args.perf_tolerance:.0f}%)\n")
    w(f"| | baseline | candidate | change |\n|---|---:|---:|---:|\n| ms per chunk (wall, parallel batch) | {bp:.2f} | {cp:.2f} | {fmt(d)}% |")

    # Gate 4 - stability (logs): no far-chunk warnings, no exception signature absent from baseline
    bst, cst = log_stats(log_for(args.baseline)), log_stats(log_for(args.candidate))
    if bst is None or cst is None:
        w("\n_Gate 4 skipped: log missing._")
    else:
        new_sigs = sorted(cst["signatures"] - bst["signatures"])
        gates["4 Stability (this run)"] = cst["far_chunk_warnings"] == 0 and cst["own_violations"] == 0 and not new_sigs
        w("\n## Gate 4 - log check\n\n| | baseline | candidate |\n|---|---:|---:|")
        w(f"| far-chunk warnings | {bst['far_chunk_warnings']} | {cst['far_chunk_warnings']} |")
        w(f"| unsafe reads / far writes by this mod's features | {bst['own_violations']} | {cst['own_violations']} |")
        w(f"| ERROR lines (excluding known dev noise) | {bst['error_lines']} | {cst['error_lines']} |")
        w(f"| exception signatures | {len(bst['signatures'])} | {len(cst['signatures'])} |")
        if cst["violations_by_feature"]:
            w("\nWorldgen boundary violations by feature (candidate; only this mod's are gated): "
              + ", ".join(f"`{k}` x{v}" for k, v in sorted(cst["violations_by_feature"].items(), key=lambda kv: -kv[1])))
        w(f"\nNew exception signatures in candidate: {len(new_sigs)}")
        for sig in new_sigs[:10]:
            w(f"  - `{sig}`")

    # Gate 5 - determinism
    if args.candidate2:
        c2 = load(args.candidate2)
        dc = determinism(cand, c2)
        dv = determinism(base, load(args.baseline2)) if args.baseline2 else None
        strict_ok = not dc["diff"]
        terrain_ok = dc["terrain_chunks"] > 0 and not dc["terrain_diff"]
        rate_c = len(dc["diff"]) / max(1, dc["chunks"])
        # Approved replacement for gate 5 (BASELINES.md): 5a terrain exact, 5b FU functions pure,
        # 5c finished-chunk variation reported only. The strict wording is shown for reference;
        # vanilla itself fails it, so it is not part of the pass/fail summary.
        info_strict = "fails in vanilla too" if not strict_ok else "identical"
        gates["5a Determinism - terrain before features identical"] = terrain_ok
        purity = cand.get("purity")
        feats = (purity or {}).get("features", {})
        feats_ok = bool(feats) and all(f.get("identical") and f.get("changedBlocks", 0) > 0 for f in feats.values())
        gates["5b Determinism - FU functions and features pure"] = (
            purity is not None and purity.get("mismatches") == 0 and feats_ok)
        if feats:
            gates["Ore safety - FU features never replace ores or non-base blocks"] = all(
                f.get("oresTouched") == 0 and f.get("illegalReplacements") == 0 for f in feats.values())
        rate_v = len(dv["diff"]) / max(1, dv["chunks"]) if dv else None
        w("\n## Gate 5 - determinism (two fresh worlds, same seed and config)\n")
        w(f"Strict wording (all finished chunks identical): {'PASS' if strict_ok else 'FAIL'} ({info_strict}; see BASELINES.md)\n")
        w("| | candidate pair | vanilla pair |\n|---|---:|---:|")
        w(f"| terrain-only chunks (noise+surface+carvers) mismatching | {len(dc['terrain_diff'])} / {dc['terrain_chunks']} | "
          + (f"{len(dv['terrain_diff'])} / {dv['terrain_chunks']}" if dv else "n/a") + " |")
        w(f"| FULL chunks mismatching | {len(dc['diff'])} / {dc['chunks']} ({100*rate_c:.1f}%) | "
          + (f"{len(dv['diff'])} / {dv['chunks']} ({100*rate_v:.1f}%)" if dv else "n/a") + " |")
        w(f"| FULL sections mismatching | {dc['section_diff']} / {dc['sections']} | "
          + (f"{dv['section_diff']} / {dv['sections']}" if dv else "n/a") + " |")
        if purity is not None:
            w(f"| FU purity self-test: mismatches over {purity.get('samples')} positions | {purity.get('mismatches', purity.get('error'))} | n/a |")
        for name, f in feats.items():
            w(f"| feature `{name}` on synthetic cave: identical / changed / illegal / ores touched | "
              f"{f.get('identical')} / {f.get('changedBlocks')} / {f.get('illegalReplacements')} / {f.get('oresTouched')} | n/a |")
        w(f"\n5c (reported only): finished chunks differing - candidate {100*rate_c:.1f}%"
          + (f", vanilla {100*rate_v:.1f}%" if dv else ""))
        w(f"\nMismatching sections by Y band (candidate): {dc['bands']}")
        if dv:
            w(f"\nMismatching sections by Y band (vanilla): {dv['bands']}")

    # Gate 6 - structures
    if "structures" in base and "structures" in cand:
        ok, rows = structure_gate(base, cand)
        gates["6 Structures"] = ok
        w(f"\n## Gate 6 - structures (found, and >= {STRUCT_INTACT_RATIO:.0%} of baseline structure blocks present)\n")
        w("| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |\n|---|---|---|:-:|---:|---:|---:|:-:|")
        for r in rows:
            w(f"| {r[0]} | {r[1]} | {r[2]} | {r[3]} | {r[4]} | {r[5]} | {fmt(r[6], 3) if r[6] is not None else 'n/a'} | {'PASS' if r[7] else 'FAIL'} |")

    # This mod's own structures (only in the candidate): reported, not gated against vanilla.
    extra = {k: v for k, v in cand.get("structures", {}).items() if k not in base.get("structures", {})}
    if extra:
        w("\n## Structures added by the candidate (informational)\n")
        w("| structure | found | position | pieces | structure blocks | most common blocks |\n|---|:-:|---|---:|---:|---|")
        for name, c in extra.items():
            top = sorted(c.get("structureBlocks", {}).items(), key=lambda kv: -kv[1])[:5]
            top_txt = ", ".join(k.split(":")[-1] + " " + str(v) for k, v in top)
            ok_found = bool(c.get("found")) and bool(c.get("startValid"))
            w(f"| {name} | {ok_found} | {c.get('pos')} | {c.get('pieces', 0)} | {c.get('structureBlockTotal', 0)} | {top_txt} |")

    # Gate 7 - registration
    if "registry" in base and "registry" in cand:
        ok, problems, removed_carvers = registry_gate(base, cand)
        gates["7 Registration"] = ok
        w(f"\n## Gate 7 - feature registration\n\n- problems: {len(problems)}")
        for p in problems[:20]:
            w(f"  - {p}")
        if removed_carvers:
            ids = sorted({c for _, cs in removed_carvers for c in cs})
            w(f"- carvers removed (informational; allowed by design, PLAN.md 2.2): {ids} in {len(removed_carvers)} biomes")

    # Metrics
    if "scan" in base and "scan" in cand:
        bs, cs = base["scan"], cand["scan"]
        w("\n## Metrics (not gates)\n\n| metric | baseline | candidate | change % |\n|---|---:|---:|---:|")
        for key in ("airBelowZeroPct", "waterSourcesBelowZero", "lavaSourcesBelowZero", "darkSpawnableFloor",
                    "scheduledFluidTicks", "scheduledBlockTicks"):
            b, c = bs[key], cs[key]
            w(f"| {key} | {b:.2f} | {c:.2f} | {fmt(pct(c, b))} |" if isinstance(b, float)
              else f"| {key} | {b} | {c} | {fmt(pct(c, b))} |")

    w("\n## Summary\n\n| gate | result |\n|---|:-:|")
    for g, ok in gates.items():
        w(f"| {g} | {'PASS' if ok else 'FAIL'} |")

    text = "\n".join(out) + "\n"
    if args.md:
        with open(args.md, "w", encoding="utf-8") as f:
            f.write(text)
    sys.stdout.write(text)
    return 0 if all(gates.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
