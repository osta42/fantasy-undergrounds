# Overhaul: monumental chambers — results

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

Screenshots (no shaders, no night vision, gamma 1.0, environment fog on): `screenshots/overhaul-*.jpg`. Vertical cut through the audit region: `images/overhaul-vertical.png`.
