# Audit comparison: `vanilla-a` (baseline) vs `overhaul96-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

Baseline for this gate: `deep-baseline`, the same deep world (floor y -128) with this mod's caves switched off. Against the 384-block vanilla world the stone/deepslate split of ores whose height range is anchored to the world bottom moves down; see the ore family totals below.

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 252802 | 266806 | +5.5 | yes | PASS |
| minecraft:copper_ore | 242191 | 252660 | +4.3 | yes | PASS |
| minecraft:deepslate_coal_ore | 2631 | 2547 | -3.2 | yes | PASS |
| minecraft:deepslate_copper_ore | 21770 | 20742 | -4.7 | yes | PASS |
| minecraft:deepslate_diamond_ore | 101426 | 95567 | -5.8 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 3 | -57.1 | no | PASS |
| minecraft:deepslate_gold_ore | 83305 | 77221 | -7.3 | yes | PASS |
| minecraft:deepslate_iron_ore | 114849 | 107789 | -6.1 | yes | PASS |
| minecraft:deepslate_lapis_ore | 58494 | 55282 | -5.5 | yes | PASS |
| minecraft:deepslate_redstone_ore | 136183 | 132640 | -2.6 | yes | PASS |
| minecraft:diamond_ore | 9 | 16 | +77.8 | no | PASS |
| minecraft:emerald_ore | 286 | 287 | +0.3 | yes | PASS |
| minecraft:gold_ore | 13270 | 14155 | +6.7 | yes | PASS |
| minecraft:iron_ore | 166077 | 173763 | +4.6 | yes | PASS |
| minecraft:lapis_ore | 29168 | 30588 | +4.9 | yes | PASS |
| minecraft:redstone_ore | 6429 | 6714 | +4.4 | yes | PASS |
| occultism:silver_ore | 45193 | 47598 | +5.3 | yes | PASS |
| occultism:silver_ore_deepslate | 263648 | 246225 | -6.6 | yes | PASS |
| powah:deepslate_uraninite_ore | 33852 | 32160 | -5.0 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4419 | 4161 | -5.8 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 52339 | 49148 | -6.1 | yes | PASS |
| powah:uraninite_ore | 4840 | 4943 | +2.1 | yes | PASS |
| powah:uraninite_ore_dense | 12 | 15 | +25.0 | no | PASS |
| powah:uraninite_ore_poor | 23147 | 24160 | +4.4 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 26.61 | 26.65 | +0.1% |
| informational: `deep-baseline` (deep world, caves off) | 25.93 | 26.65 | +2.8% |

Deep world alone vs vanilla: -2.5%; this mod's caves and decoration on top of it: +2.8%.

## Ore families vs `vanilla-a` (stone + deepslate variants together; informational)

| ore family | baseline | candidate | change % |
|---|---:|---:|---:|
| minecraft:coal_ore | 256892 | 269353 | +4.9 |
| minecraft:copper_ore | 262431 | 273402 | +4.2 |
| minecraft:diamond_ore | 92870 | 95583 | +2.9 |
| minecraft:emerald_ore | 285 | 290 | +1.8 |
| minecraft:gold_ore | 93700 | 91376 | -2.5 |
| minecraft:iron_ore | 275222 | 281552 | +2.3 |
| minecraft:lapis_ore | 83468 | 85870 | +2.9 |
| minecraft:redstone_ore | 135845 | 139354 | +2.6 |
| occultism:silver_ore | 307608 | 293823 | -4.5 |
| powah:uraninite_ore | 36348 | 37103 | +2.1 |
| powah:uraninite_ore_dense | 4107 | 4176 | +1.7 |
| powah:uraninite_ore_poor | 70327 | 73308 | +4.2 |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 5653 | 5944 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x5943

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | 0 / 256 |
| FULL chunks mismatching | 2756 / 4096 (67.3%) | 2604 / 4096 (63.6%) |
| FULL sections mismatching | 7853 / 106496 | 6842 / 98304 |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 67.3%, vanilla 63.6%

Mismatching sections by Y band (candidate): {-64: 60, -48: 189, -32: 191, -16: 482, 0: 552, 16: 680, 32: 1144, 48: 1079, 64: 1201, 80: 944, 96: 874, 112: 351, 128: 87, 144: 17, 160: 2}

Mismatching sections by Y band (vanilla): {-64: 85, -48: 232, -32: 453, -16: 736, 0: 1036, 16: 945, 32: 1111, 48: 940, 64: 878, 80: 349, 96: 65, 112: 12}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

Baseline for this gate: `deep-baseline` (same deep world, this mod's caves off).

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64658 | 69647 | +1.077 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55099 | 55167 | +1.001 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25306 | 25435 | +1.005 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 2744 | 3179 | +1.159 | PASS |
| fantasyundergrounds:ancient_ruins | None | 7904,0,7616 | False | 0 | 4300 | n/a | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 55 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.29 | 11.72 | +60.8 |
| waterSourcesBelowZero | 323122 | 883209 | +173.3 |
| lavaSourcesBelowZero | 184912 | 474659 | +156.7 |
| darkSpawnableFloor | 619028 | 382779 | -38.2 |
| scheduledFluidTicks | 66205 | 65822 | -0.6 |
| scheduledBlockTicks | 495703 | 474950 | -4.2 |

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
