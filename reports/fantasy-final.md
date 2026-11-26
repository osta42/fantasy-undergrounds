# Audit comparison: `vanilla-a` (baseline) vs `fantasy-final-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254393 | 261208 | +2.7 | yes | PASS |
| minecraft:copper_ore | 240675 | 244576 | +1.6 | yes | PASS |
| minecraft:deepslate_coal_ore | 2479 | 2292 | -7.5 | yes | PASS |
| minecraft:deepslate_copper_ore | 21724 | 20261 | -6.7 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91432 | 85041 | -7.0 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 6 | -14.3 | no | PASS |
| minecraft:deepslate_gold_ore | 79994 | 74436 | -6.9 | yes | PASS |
| minecraft:deepslate_iron_ore | 106274 | 99438 | -6.4 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49567 | 45933 | -7.3 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126955 | 119506 | -5.9 | yes | PASS |
| minecraft:diamond_ore | 1420 | 1453 | +2.3 | yes | PASS |
| minecraft:emerald_ore | 276 | 280 | +1.4 | yes | PASS |
| minecraft:gold_ore | 13617 | 13182 | -3.2 | yes | PASS |
| minecraft:iron_ore | 169089 | 168885 | -0.1 | yes | PASS |
| minecraft:lapis_ore | 33907 | 34001 | +0.3 | yes | PASS |
| minecraft:redstone_ore | 8855 | 8512 | -3.9 | yes | PASS |
| occultism:silver_ore | 44892 | 46070 | +2.6 | yes | PASS |
| occultism:silver_ore_deepslate | 262726 | 236398 | -10.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29762 | 27449 | -7.8 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4084 | 3788 | -7.2 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40761 | 37631 | -7.7 | yes | PASS |
| powah:uraninite_ore | 6592 | 6498 | -1.4 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 20 | -13.0 | no | PASS |
| powah:uraninite_ore_poor | 29572 | 29863 | +1.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 24.14 | 25.35 | +5.0% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7065 | 7146 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x7118, `minecraft:sculk_patch_deep_dark` x27

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2677 / 4096 (65.4%) | 2653 / 4096 (64.8%) |
| FULL sections mismatching | 8302 / 98304 | 7017 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 65.4%, vanilla 64.8%

Mismatching sections by Y band (candidate): {-64: 272, -48: 588, -32: 732, -16: 936, 0: 1218, 16: 1024, 32: 1230, 48: 887, 64: 879, 80: 401, 96: 108, 112: 23, 128: 4}

Mismatching sections by Y band (vanilla): {-64: 81, -48: 258, -32: 457, -16: 698, 0: 1039, 16: 938, 32: 1164, 48: 917, 64: 939, 80: 399, 96: 95, 112: 22, 128: 10}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63983 | 66880 | +1.045 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55019 | 55035 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25304 | 25510 | +1.008 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5105 | 5263 | +1.031 | PASS |

## Structures added by the candidate (informational)

| structure | found | position | pieces | structure blocks | most common blocks |
|---|:-:|---|---:|---:|---|
| fantasyundergrounds:ancient_ruins | True | 7904,0,7616 | 5 | 4047 | deepslate_tiles 946, amethyst_block 716, polished_deepslate 711, deepslate_bricks 649, cracked_deepslate_bricks 255 |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 12.65 | +73.6 |
| waterSourcesBelowZero | 322056 | 1067626 | +231.5 |
| lavaSourcesBelowZero | 184913 | 538849 | +191.4 |
| darkSpawnableFloor | 618047 | 235708 | -61.9 |
| scheduledFluidTicks | 66211 | 66227 | +0.0 |
| scheduledBlockTicks | 496919 | 476542 | -4.1 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | PASS |
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
| 5a Determinism - terrain before features identical | PASS |
| 5b Determinism - FU functions and features pure | PASS |
| Ore safety - FU features never replace ores or non-base blocks | PASS |
| 6 Structures | PASS |
| 7 Registration | PASS |
