# Audit comparison: `vanilla-a` (baseline) vs `overhaul-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

Baseline for this gate: `deep-baseline`, the same deep world (floor y -128) with this mod's caves switched off. Against the 384-block vanilla world the stone/deepslate split of ores whose height range is anchored to the world bottom moves down; see the ore family totals below.

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 253634 | 267450 | +5.4 | yes | PASS |
| minecraft:copper_ore | 242570 | 255023 | +5.1 | yes | PASS |
| minecraft:deepslate_coal_ore | 2629 | 2611 | -0.7 | yes | PASS |
| minecraft:deepslate_copper_ore | 21789 | 20898 | -4.1 | yes | PASS |
| minecraft:deepslate_diamond_ore | 102298 | 99418 | -2.8 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 3 | -57.1 | no | PASS |
| minecraft:deepslate_gold_ore | 83970 | 77389 | -7.8 | yes | PASS |
| minecraft:deepslate_iron_ore | 120696 | 113152 | -6.3 | yes | PASS |
| minecraft:deepslate_lapis_ore | 64106 | 60635 | -5.4 | yes | PASS |
| minecraft:deepslate_redstone_ore | 139205 | 137973 | -0.9 | yes | PASS |
| minecraft:diamond_ore | 12 | 18 | +50.0 | no | PASS |
| minecraft:emerald_ore | 276 | 284 | +2.9 | yes | PASS |
| minecraft:gold_ore | 13678 | 14626 | +6.9 | yes | PASS |
| minecraft:iron_ore | 163266 | 173023 | +6.0 | yes | PASS |
| minecraft:lapis_ore | 26205 | 27754 | +5.9 | yes | PASS |
| minecraft:redstone_ore | 5242 | 5541 | +5.7 | yes | PASS |
| occultism:silver_ore | 45424 | 48086 | +5.9 | yes | PASS |
| occultism:silver_ore_deepslate | 265846 | 249499 | -6.1 | yes | PASS |
| powah:deepslate_uraninite_ore | 36404 | 34509 | -5.2 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4607 | 4357 | -5.4 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 58370 | 55488 | -4.9 | yes | PASS |
| powah:uraninite_ore | 3837 | 3994 | +4.1 | yes | PASS |
| powah:uraninite_ore_dense | 5 | 6 | +20.0 | no | PASS |
| powah:uraninite_ore_poor | 19630 | 20786 | +5.9 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 26.61 | 34.86 | +31.0% |
| informational: `deep-baseline` (deep world, caves off) | 33.61 | 34.86 | +3.7% |

Deep world alone vs vanilla: +26.3%; this mod's caves and decoration on top of it: +3.7%.

## Ore families vs `vanilla-a` (stone + deepslate variants together; informational)

| ore family | baseline | candidate | change % |
|---|---:|---:|---:|
| minecraft:coal_ore | 256892 | 270061 | +5.1 |
| minecraft:copper_ore | 262431 | 275921 | +5.1 |
| minecraft:diamond_ore | 92870 | 99436 | +7.1 |
| minecraft:emerald_ore | 285 | 287 | +0.7 |
| minecraft:gold_ore | 93700 | 92015 | -1.8 |
| minecraft:iron_ore | 275222 | 286175 | +4.0 |
| minecraft:lapis_ore | 83468 | 88389 | +5.9 |
| minecraft:redstone_ore | 135845 | 143514 | +5.6 |
| occultism:silver_ore | 307608 | 297585 | -3.3 |
| powah:uraninite_ore | 36348 | 38503 | +5.9 |
| powah:uraninite_ore_dense | 4107 | 4363 | +6.2 |
| powah:uraninite_ore_poor | 70327 | 76274 | +8.5 |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 5653 | 6177 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x6176

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2601 / 4096 (63.5%) | 2604 / 4096 (63.6%) |
| FULL sections mismatching | 7079 / 114688 | 6842 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 63.5%, vanilla 63.6%

Mismatching sections by Y band (candidate): {-64: 23, -48: 105, -32: 159, -16: 189, 0: 194, 16: 400, 32: 381, 48: 516, 64: 975, 80: 837, 96: 1086, 112: 876, 128: 824, 144: 381, 160: 112, 176: 19, 192: 2}

Mismatching sections by Y band (vanilla): {-64: 85, -48: 232, -32: 453, -16: 736, 0: 1036, 16: 945, 32: 1111, 48: 940, 64: 878, 80: 349, 96: 65, 112: 12}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

Baseline for this gate: `deep-baseline` (same deep world, this mod's caves off).

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64689 | 67985 | +1.051 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55059 | 55081 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 24871 | 25417 | +1.022 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 2961 | 3760 | +1.270 | PASS |
| fantasyundergrounds:ancient_ruins | None | 7904,0,7616 | False | 0 | 4339 | n/a | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.29 | 11.29 | +54.9 |
| waterSourcesBelowZero | 323122 | 745725 | +130.8 |
| lavaSourcesBelowZero | 184912 | 852890 | +361.2 |
| darkSpawnableFloor | 619028 | 437744 | -29.3 |
| scheduledFluidTicks | 66205 | 66177 | -0.0 |
| scheduledBlockTicks | 495703 | 473322 | -4.5 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | PASS |
| 3 Performance | FAIL |
| 4 Stability (this run) | PASS |
| 5a Determinism - terrain before features identical | PASS |
| 5b Determinism - FU functions and features pure | PASS |
| Ore safety - FU features never replace ores or non-base blocks | PASS |
| 6 Structures | PASS |
| 7 Registration | PASS |
