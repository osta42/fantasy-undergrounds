# Audit comparison: `vanilla-a` (baseline) vs `phase2-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254724 | 265348 | +4.2 | yes | PASS |
| minecraft:copper_ore | 240702 | 250294 | +4.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2516 | 2697 | +7.2 | yes | PASS |
| minecraft:deepslate_copper_ore | 21655 | 22630 | +4.5 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91429 | 94004 | +2.8 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 6 | -14.3 | no | PASS |
| minecraft:deepslate_gold_ore | 79986 | 82592 | +3.3 | yes | PASS |
| minecraft:deepslate_iron_ore | 106208 | 110301 | +3.9 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49570 | 51268 | +3.4 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126976 | 129622 | +2.1 | yes | PASS |
| minecraft:diamond_ore | 1427 | 1535 | +7.6 | yes | PASS |
| minecraft:emerald_ore | 278 | 285 | +2.5 | yes | PASS |
| minecraft:gold_ore | 13634 | 14453 | +6.0 | yes | PASS |
| minecraft:iron_ore | 169048 | 176430 | +4.4 | yes | PASS |
| minecraft:lapis_ore | 33883 | 35552 | +4.9 | yes | PASS |
| minecraft:redstone_ore | 8851 | 9270 | +4.7 | yes | PASS |
| occultism:silver_ore | 44884 | 47120 | +5.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262772 | 268087 | +2.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29754 | 30413 | +2.2 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4086 | 4183 | +2.4 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40771 | 41637 | +2.1 | yes | PASS |
| powah:uraninite_ore | 6587 | 6925 | +5.1 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29564 | 30739 | +4.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 49.23 | 46.13 | -6.3% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 6756 | 6640 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x6640

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2387 / 4096 (58.3%) | 2552 / 4096 (62.3%) |
| FULL sections mismatching | 6036 / 98304 | 6653 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 58.3%, vanilla 62.3%

Mismatching sections by Y band (candidate): {-64: 92, -48: 197, -32: 335, -16: 469, 0: 908, 16: 816, 32: 1045, 48: 857, 64: 847, 80: 376, 96: 77, 112: 13, 128: 4}

Mismatching sections by Y band (vanilla): {-64: 98, -48: 215, -32: 454, -16: 692, 0: 946, 16: 862, 32: 1106, 48: 946, 64: 877, 80: 347, 96: 86, 112: 23, 128: 1}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63896 | 65306 | +1.022 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55015 | 55025 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25308 | 25508 | +1.008 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5216 | 5293 | +1.015 | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 5.40 | -25.8 |
| waterSourcesBelowZero | 322723 | 249428 | -22.7 |
| lavaSourcesBelowZero | 184911 | 192315 | +4.0 |
| darkSpawnableFloor | 618615 | 229805 | -62.9 |
| scheduledFluidTicks | 66171 | 66267 | +0.1 |
| scheduledBlockTicks | 497270 | 480503 | -3.4 |

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
