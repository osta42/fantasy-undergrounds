# Overhaul: monumental chambers — results

**Current build: world floor y −96 (user choice after the −128 version missed the speed gate). All gates pass.**

| Gate | Plain (Occultism + Powah) `overhaul96.md` | With Biomes O' Plenty `overhaul96-bop.md` |
|---|---|---|
| 2 Ores (vs same deep world, caves off) | PASS, −7.3 … +6.7% | PASS, −7.9 … +4.0% |
| Ore families vs vanilla (informational) | −4.5 … +4.9% | — |
| 3 Performance vs vanilla (≤ +30%) | PASS, +0.1% audit / +3.0% pre-generation | PASS, +12.7% / +17.4% (BoP baselines from 24 Nov) |
| 4 Stability (15,876-chunk pre-generation) | PASS | PASS |
| 5a / 5b Determinism, ore safety | PASS | PASS |
| 6 Structures | PASS (all ≥ 100%) | PASS |
| 7 Registration | PASS | PASS (BoP features unchanged) |

## History: the first version, floor y −128

Gate suite `tools/run_gates.sh --tag overhaul` (fresh vanilla baselines, Occultism and Powah loaded, seed 1234567890, 64 × 64-chunk audits plus a 2,016 × 2,016-block pre-generation). Full tables: `overhaul.md`, `overhaul-pregen.md`.

| Gate | Result | Notes |
|---|:-:|---|
| 2 Ores | PASS | Every ore −7.8 … +6.9% against `deep-baseline` (same deep world, this mod's caves off). Ore families (stone + deepslate) −3.3 … +8.5% against 384-block vanilla. |
| 3 Performance | **FAIL** | +31.0% (audit), +34.9% (pre-generation) against vanilla. The deep world alone: +26.3%; this mod's caves and decoration on top of it: +3.7%. |
| 4 Stability | PASS | 15,876 pre-generated chunks: no far-chunk warnings, no unsafe reads or far writes by this mod, no new exception signatures. |
| 5a / 5b Determinism | PASS | Terrain before features identical across two fresh worlds; this mod's functions and features pure. |
| Ore safety | PASS | This mod's features never replace ores or non-base blocks (200,000-sample self-test, 0 mismatches). |
| 6 Structures | PASS | Ancient city, trial chambers, stronghold, mineshaft ≥ 100% of their blocks against `deep-baseline` (the deeper floor moves mineshafts, so vanilla is not comparable there). |
| 7 Registration | PASS | |

Why the deep world is slow: profiling (Java Flight Recorder, chunk worker threads) puts vanilla's `fillFromNoise` at 30% and `SurfaceSystem.buildSurface` at 23% of the time; both scale with world height (448 vs 384 layers) and the surface rules run on every stone block below y 0. All of this mod's code together is about 6% (strata 2.6%, density functions ~2%, structures and placement filters the rest).

Screenshots of the −96 build (no shaders, no night vision, gamma 1.0, environment fog on): `screenshots/overhaul-*.jpg`. Vertical cut through the audit region: `images/overhaul-vertical.png`.

## Biomes O' Plenty (with TerraBlender, GlitchCore, Occultism, Powah)

`overhaul-bop.md`, `overhaul-bop-pregen.md`. Baselines: `bop-vanilla-a` / `bop-pregen-vanilla` (vanilla + the same mods, recorded on 24 Nov) and `bop-deep-baseline` (deep world, this mod's caves off, today).

| Gate | Result | Notes |
|---|:-:|---|
| 2 Ores | PASS | −8.6 … +4.9% against `bop-deep-baseline` |
| 3 Performance | **FAIL** (audit) / PASS (pre-generation) | +31.4% / +29.6% against vanilla + BoP; deep world alone +24.2%, caves and decoration +5.9% |
| 4 Stability | PASS | no far-chunk warnings, no new exception signatures (15,876 chunks) |
| 5a / 5b, ore safety | PASS | |
| 6 Structures | PASS | |
| 7 Registration | PASS | 0 problems: every BoP biome keeps its features; only the three vanilla carvers are removed, as designed |

TerraBlender logs "Initialized TerraBlender biomes for level stem minecraft:overworld" with the deep-world overrides in place, and the same 42 BoP block kinds appear in similar amounts.
