# Audit comparison: `vanilla-a` (baseline) vs `vanilla-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254393 | 254393 | 0.0 | yes | PASS |
| minecraft:copper_ore | 240675 | 240675 | 0.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2479 | 2479 | 0.0 | yes | PASS |
| minecraft:deepslate_copper_ore | 21724 | 21724 | 0.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91432 | 91432 | 0.0 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 7 | 0.0 | no | PASS |
| minecraft:deepslate_gold_ore | 79994 | 79994 | 0.0 | yes | PASS |
| minecraft:deepslate_iron_ore | 106274 | 106274 | 0.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49567 | 49567 | 0.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126955 | 126955 | 0.0 | yes | PASS |
| minecraft:diamond_ore | 1420 | 1420 | 0.0 | yes | PASS |
| minecraft:emerald_ore | 276 | 276 | 0.0 | yes | PASS |
| minecraft:gold_ore | 13617 | 13617 | 0.0 | yes | PASS |
| minecraft:iron_ore | 169089 | 169089 | 0.0 | yes | PASS |
| minecraft:lapis_ore | 33907 | 33907 | 0.0 | yes | PASS |
| minecraft:redstone_ore | 8855 | 8855 | 0.0 | yes | PASS |
| occultism:silver_ore | 44892 | 44892 | 0.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262726 | 262726 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29762 | 29762 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4084 | 4084 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40761 | 40761 | 0.0 | yes | PASS |
| powah:uraninite_ore | 6592 | 6592 | 0.0 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29572 | 29572 | 0.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 24.14 | 24.14 | 0.0% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7065 | 7065 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x7064

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2653 / 4096 (64.8%) | n/a |
| FULL sections mismatching | 7017 / 98304 | n/a |

5c (reported only): finished chunks differing - candidate 64.8%

Mismatching sections by Y band (candidate): {-64: 81, -48: 258, -32: 457, -16: 698, 0: 1039, 16: 938, 32: 1164, 48: 917, 64: 939, 80: 399, 96: 95, 112: 22, 128: 10}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63983 | 63983 | +1.000 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55019 | 55019 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25304 | 25304 | +1.000 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5105 | 5105 | +1.000 | PASS |

## Gate 7 - feature registration

- problems: 0

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 7.28 | 0.0 |
| waterSourcesBelowZero | 322056 | 322056 | 0.0 |
| lavaSourcesBelowZero | 184913 | 184913 | 0.0 |
| darkSpawnableFloor | 618047 | 618047 | 0.0 |
| scheduledFluidTicks | 66211 | 66211 | 0.0 |
| scheduledBlockTicks | 496919 | 496919 | 0.0 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | PASS |
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
| 5a Determinism - terrain before features identical | PASS |
| 5b Determinism - FU functions and features pure | FAIL |
| 6 Structures | PASS |
| 7 Registration | PASS |
