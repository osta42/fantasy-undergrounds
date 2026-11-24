# Audit comparison: `vanilla-a` (baseline) vs `phase3-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254393 | 265330 | +4.3 | yes | PASS |
| minecraft:copper_ore | 240675 | 250264 | +4.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2479 | 2704 | +9.1 | yes | PASS |
| minecraft:deepslate_copper_ore | 21724 | 22591 | +4.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91432 | 93981 | +2.8 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 6 | -14.3 | no | PASS |
| minecraft:deepslate_gold_ore | 79994 | 82543 | +3.2 | yes | PASS |
| minecraft:deepslate_iron_ore | 106274 | 110265 | +3.8 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49567 | 51177 | +3.2 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126955 | 129586 | +2.1 | yes | PASS |
| minecraft:diamond_ore | 1420 | 1528 | +7.6 | yes | PASS |
| minecraft:emerald_ore | 276 | 283 | +2.5 | yes | PASS |
| minecraft:gold_ore | 13617 | 14411 | +5.8 | yes | PASS |
| minecraft:iron_ore | 169089 | 176280 | +4.3 | yes | PASS |
| minecraft:lapis_ore | 33907 | 35519 | +4.8 | yes | PASS |
| minecraft:redstone_ore | 8855 | 9281 | +4.8 | yes | PASS |
| occultism:silver_ore | 44892 | 47122 | +5.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262726 | 268065 | +2.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29762 | 30375 | +2.1 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4084 | 4182 | +2.4 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40761 | 41601 | +2.1 | yes | PASS |
| powah:uraninite_ore | 6592 | 6923 | +5.0 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29572 | 30739 | +3.9 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 24.14 | 22.26 | -7.8% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7065 | 7112 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x7111

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2538 / 4096 (62.0%) | 2653 / 4096 (64.8%) |
| FULL sections mismatching | 6656 / 98304 | 7017 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 62.0%, vanilla 64.8%

Mismatching sections by Y band (candidate): {-64: 75, -48: 215, -32: 356, -16: 530, 0: 1063, 16: 908, 32: 1136, 48: 943, 64: 923, 80: 379, 96: 95, 112: 24, 128: 9}

Mismatching sections by Y band (vanilla): {-64: 81, -48: 258, -32: 457, -16: 698, 0: 1039, 16: 938, 32: 1164, 48: 917, 64: 939, 80: 399, 96: 95, 112: 22, 128: 10}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63983 | 64719 | +1.012 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55019 | 55029 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25304 | 25508 | +1.008 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5105 | 5496 | +1.077 | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 5.44 | -25.3 |
| waterSourcesBelowZero | 322056 | 246447 | -23.5 |
| lavaSourcesBelowZero | 184913 | 192304 | +4.0 |
| darkSpawnableFloor | 618047 | 229486 | -62.9 |
| scheduledFluidTicks | 66211 | 66248 | +0.1 |
| scheduledBlockTicks | 496919 | 482302 | -2.9 |

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
