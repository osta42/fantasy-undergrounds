# Audit comparison: `bop-vanilla-a` (baseline) vs `bop-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 278019 | 288217 | +3.7 | yes | PASS |
| minecraft:copper_ore | 265408 | 275560 | +3.8 | yes | PASS |
| minecraft:deepslate_coal_ore | 3053 | 3253 | +6.6 | yes | PASS |
| minecraft:deepslate_copper_ore | 25970 | 27034 | +4.1 | yes | PASS |
| minecraft:deepslate_diamond_ore | 98549 | 101395 | +2.9 | yes | PASS |
| minecraft:deepslate_emerald_ore | 24 | 25 | +4.2 | no | PASS |
| minecraft:deepslate_gold_ore | 88474 | 91509 | +3.4 | yes | PASS |
| minecraft:deepslate_iron_ore | 118732 | 123269 | +3.8 | yes | PASS |
| minecraft:deepslate_lapis_ore | 55758 | 57930 | +3.9 | yes | PASS |
| minecraft:deepslate_redstone_ore | 134617 | 137518 | +2.2 | yes | PASS |
| minecraft:diamond_ore | 1669 | 1799 | +7.8 | yes | PASS |
| minecraft:emerald_ore | 461 | 463 | +0.4 | yes | PASS |
| minecraft:gold_ore | 16091 | 16955 | +5.4 | yes | PASS |
| minecraft:iron_ore | 193159 | 200835 | +4.0 | yes | PASS |
| minecraft:lapis_ore | 38586 | 40318 | +4.5 | yes | PASS |
| minecraft:redstone_ore | 10564 | 11075 | +4.8 | yes | PASS |
| occultism:silver_ore | 50696 | 52881 | +4.3 | yes | PASS |
| occultism:silver_ore_deepslate | 294075 | 300488 | +2.2 | yes | PASS |
| powah:deepslate_uraninite_ore | 32240 | 33139 | +2.8 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4559 | 4715 | +3.4 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 45394 | 46807 | +3.1 | yes | PASS |
| powah:uraninite_ore | 7814 | 8035 | +2.8 | yes | PASS |
| powah:uraninite_ore_dense | 28 | 28 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 32678 | 34055 | +4.2 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 32.04 | 31.16 | -2.7% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7423 | 7161 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x7159

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2574 / 4096 (62.8%) | n/a |
| FULL sections mismatching | 6593 / 98304 | n/a |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 62.8%

Mismatching sections by Y band (candidate): {-64: 101, -48: 202, -32: 200, -16: 415, 0: 1027, 16: 914, 32: 1216, 48: 987, 64: 840, 80: 459, 96: 149, 112: 79, 128: 4}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64359 | 64280 | +0.999 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 54869 | 54879 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25309 | 25508 | +1.008 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 3809 | 3900 | +1.024 | PASS |

## Structures added by the candidate (informational)

| structure | found | position | pieces | structure blocks | most common blocks |
|---|:-:|---|---:|---:|---|
| fantasyundergrounds:ancient_ruins | True | 8720,0,8688 | 1 | 2335 | deepslate_bricks 1088, cracked_deepslate_bricks 505, deepslate_tiles 381, polished_deepslate 204, cracked_deepslate_tiles 122 |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 112 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 5.33 | -26.9 |
| waterSourcesBelowZero | 292456 | 219569 | -24.9 |
| lavaSourcesBelowZero | 185012 | 191251 | +3.4 |
| darkSpawnableFloor | 657216 | 229143 | -65.1 |
| scheduledFluidTicks | 66411 | 66512 | +0.2 |
| scheduledBlockTicks | 406967 | 410755 | +0.9 |

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
