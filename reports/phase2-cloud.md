# Audit comparison: `vanilla-a` (baseline) vs `phase2-cloud-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254393 | 265012 | +4.2 | yes | PASS |
| minecraft:copper_ore | 240675 | 250328 | +4.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2479 | 2661 | +7.3 | yes | PASS |
| minecraft:deepslate_copper_ore | 21724 | 22616 | +4.1 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91432 | 94018 | +2.8 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 6 | -14.3 | no | PASS |
| minecraft:deepslate_gold_ore | 79994 | 82540 | +3.2 | yes | PASS |
| minecraft:deepslate_iron_ore | 106274 | 110353 | +3.8 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49567 | 51252 | +3.4 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126955 | 129630 | +2.1 | yes | PASS |
| minecraft:diamond_ore | 1420 | 1542 | +8.6 | yes | PASS |
| minecraft:emerald_ore | 276 | 285 | +3.3 | yes | PASS |
| minecraft:gold_ore | 13617 | 14430 | +6.0 | yes | PASS |
| minecraft:iron_ore | 169089 | 176398 | +4.3 | yes | PASS |
| minecraft:lapis_ore | 33907 | 35564 | +4.9 | yes | PASS |
| minecraft:redstone_ore | 8855 | 9270 | +4.7 | yes | PASS |
| occultism:silver_ore | 44892 | 47139 | +5.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262726 | 268021 | +2.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29762 | 30411 | +2.2 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4084 | 4182 | +2.4 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40761 | 41647 | +2.2 | yes | PASS |
| powah:uraninite_ore | 6592 | 6924 | +5.0 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29572 | 30720 | +3.9 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 24.14 | 21.86 | -9.5% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7065 | 7106 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x7105

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2508 / 4096 (61.2%) | 2653 / 4096 (64.8%) |
| FULL sections mismatching | 6394 / 98304 | 7017 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 61.2%, vanilla 64.8%

Mismatching sections by Y band (candidate): {-64: 103, -48: 234, -32: 370, -16: 573, 0: 1019, 16: 873, 32: 1115, 48: 875, 64: 811, 80: 310, 96: 92, 112: 17, 128: 2}

Mismatching sections by Y band (vanilla): {-64: 81, -48: 258, -32: 457, -16: 698, 0: 1039, 16: 938, 32: 1164, 48: 917, 64: 939, 80: 399, 96: 95, 112: 22, 128: 10}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63983 | 65471 | +1.023 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55019 | 55030 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25304 | 25506 | +1.008 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5105 | 5437 | +1.065 | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 5.41 | -25.8 |
| waterSourcesBelowZero | 322056 | 250032 | -22.4 |
| lavaSourcesBelowZero | 184913 | 192314 | +4.0 |
| darkSpawnableFloor | 618047 | 230017 | -62.8 |
| scheduledFluidTicks | 66211 | 66260 | +0.1 |
| scheduledBlockTicks | 496919 | 480779 | -3.2 |

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
