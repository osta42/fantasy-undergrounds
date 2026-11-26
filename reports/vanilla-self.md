# Audit comparison: `vanilla-a` (baseline) vs `vanilla-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254440 | 254440 | 0.0 | yes | PASS |
| minecraft:copper_ore | 240742 | 240742 | 0.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2452 | 2452 | 0.0 | yes | PASS |
| minecraft:deepslate_copper_ore | 21689 | 21689 | 0.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91459 | 91459 | 0.0 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 7 | 0.0 | no | PASS |
| minecraft:deepslate_gold_ore | 80053 | 80053 | 0.0 | yes | PASS |
| minecraft:deepslate_iron_ore | 106174 | 106174 | 0.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49544 | 49544 | 0.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126985 | 126985 | 0.0 | yes | PASS |
| minecraft:diamond_ore | 1411 | 1411 | 0.0 | yes | PASS |
| minecraft:emerald_ore | 278 | 278 | 0.0 | yes | PASS |
| minecraft:gold_ore | 13647 | 13647 | 0.0 | yes | PASS |
| minecraft:iron_ore | 169048 | 169048 | 0.0 | yes | PASS |
| minecraft:lapis_ore | 33924 | 33924 | 0.0 | yes | PASS |
| minecraft:redstone_ore | 8860 | 8860 | 0.0 | yes | PASS |
| occultism:silver_ore | 44900 | 44900 | 0.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262708 | 262708 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29761 | 29761 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4084 | 4084 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40758 | 40758 | 0.0 | yes | PASS |
| powah:uraninite_ore | 6587 | 6587 | 0.0 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29569 | 29569 | 0.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 26.61 | 26.61 | 0.0% |

## Ore families vs `vanilla-a` (stone + deepslate variants together; informational)

| ore family | baseline | candidate | change % |
|---|---:|---:|---:|
| minecraft:coal_ore | 256892 | 256892 | 0.0 |
| minecraft:copper_ore | 262431 | 262431 | 0.0 |
| minecraft:diamond_ore | 92870 | 92870 | 0.0 |
| minecraft:emerald_ore | 285 | 285 | 0.0 |
| minecraft:gold_ore | 93700 | 93700 | 0.0 |
| minecraft:iron_ore | 275222 | 275222 | 0.0 |
| minecraft:lapis_ore | 83468 | 83468 | 0.0 |
| minecraft:redstone_ore | 135845 | 135845 | 0.0 |
| occultism:silver_ore | 307608 | 307608 | 0.0 |
| powah:uraninite_ore | 36348 | 36348 | 0.0 |
| powah:uraninite_ore_dense | 4107 | 4107 | 0.0 |
| powah:uraninite_ore_poor | 70327 | 70327 | 0.0 |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 5653 | 5653 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x5652

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2604 / 4096 (63.6%) | n/a |
| FULL sections mismatching | 6842 / 98304 | n/a |

5c (reported only): finished chunks differing - candidate 63.6%

Mismatching sections by Y band (candidate): {-64: 85, -48: 232, -32: 453, -16: 736, 0: 1036, 16: 945, 32: 1111, 48: 940, 64: 878, 80: 349, 96: 65, 112: 12}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63987 | 63987 | +1.000 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55015 | 55015 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25308 | 25308 | +1.000 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5123 | 5123 | +1.000 | PASS |

## Gate 7 - feature registration

- problems: 0

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.29 | 7.29 | 0.0 |
| waterSourcesBelowZero | 323122 | 323122 | 0.0 |
| lavaSourcesBelowZero | 184912 | 184912 | 0.0 |
| darkSpawnableFloor | 619028 | 619028 | 0.0 |
| scheduledFluidTicks | 66205 | 66205 | 0.0 |
| scheduledBlockTicks | 495703 | 495703 | 0.0 |

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
