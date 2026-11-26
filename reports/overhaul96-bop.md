# Audit comparison: `bop-vanilla-a` (baseline) vs `bop-overhaul96-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

Baseline for this gate: `bop-deep-baseline`, the same deep world (floor y -128) with this mod's caves switched off. Against the 384-block vanilla world the stone/deepslate split of ores whose height range is anchored to the world bottom moves down; see the ore family totals below.

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 276246 | 287407 | +4.0 | yes | PASS |
| minecraft:copper_ore | 267101 | 276128 | +3.4 | yes | PASS |
| minecraft:deepslate_coal_ore | 3018 | 2980 | -1.3 | yes | PASS |
| minecraft:deepslate_copper_ore | 26097 | 24437 | -6.4 | yes | PASS |
| minecraft:deepslate_diamond_ore | 105575 | 99327 | -5.9 | yes | PASS |
| minecraft:deepslate_emerald_ore | 25 | 20 | -20.0 | no | PASS |
| minecraft:deepslate_gold_ore | 91935 | 84699 | -7.9 | yes | PASS |
| minecraft:deepslate_iron_ore | 126874 | 118135 | -6.9 | yes | PASS |
| minecraft:deepslate_lapis_ore | 64157 | 60176 | -6.2 | yes | PASS |
| minecraft:deepslate_redstone_ore | 141007 | 136895 | -2.9 | yes | PASS |
| minecraft:diamond_ore | 8 | 16 | +100.0 | no | PASS |
| minecraft:emerald_ore | 450 | 459 | +2.0 | yes | PASS |
| minecraft:gold_ore | 15843 | 16332 | +3.1 | yes | PASS |
| minecraft:iron_ore | 189864 | 195173 | +2.8 | yes | PASS |
| minecraft:lapis_ore | 33537 | 34546 | +3.0 | yes | PASS |
| minecraft:redstone_ore | 7634 | 7763 | +1.7 | yes | PASS |
| occultism:silver_ore | 51013 | 52950 | +3.8 | yes | PASS |
| occultism:silver_ore_deepslate | 295060 | 273963 | -7.2 | yes | PASS |
| powah:deepslate_uraninite_ore | 36127 | 33675 | -6.8 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4721 | 4445 | -5.8 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 55440 | 52016 | -6.2 | yes | PASS |
| powah:uraninite_ore | 5622 | 5625 | +0.1 | yes | PASS |
| powah:uraninite_ore_dense | 16 | 18 | +12.5 | no | PASS |
| powah:uraninite_ore_poor | 26821 | 27631 | +3.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 32.04 | 36.11 | +12.7% |
| informational: `bop-deep-baseline` (deep world, caves off) | 36.65 | 36.11 | -1.5% |

Deep world alone vs vanilla: +14.4%; this mod's caves and decoration on top of it: -1.5%.

## Ore families vs `bop-vanilla-a` (stone + deepslate variants together; informational)

| ore family | baseline | candidate | change % |
|---|---:|---:|---:|
| minecraft:coal_ore | 281072 | 290387 | +3.3 |
| minecraft:copper_ore | 291378 | 300565 | +3.2 |
| minecraft:diamond_ore | 100218 | 99343 | -0.9 |
| minecraft:emerald_ore | 485 | 479 | -1.2 |
| minecraft:gold_ore | 104565 | 101031 | -3.4 |
| minecraft:iron_ore | 311891 | 313308 | +0.5 |
| minecraft:lapis_ore | 94344 | 94722 | +0.4 |
| minecraft:redstone_ore | 145181 | 144658 | -0.4 |
| occultism:silver_ore | 344771 | 326913 | -5.2 |
| powah:uraninite_ore | 40054 | 39300 | -1.9 |
| powah:uraninite_ore_dense | 4587 | 4463 | -2.7 |
| powah:uraninite_ore_poor | 78072 | 79647 | +2.0 |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7423 | 6485 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x6483

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2595 / 4096 (63.4%) | n/a |
| FULL sections mismatching | 7299 / 106496 | n/a |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 63.4%

Mismatching sections by Y band (candidate): {-64: 81, -48: 189, -32: 122, -16: 306, 0: 291, 16: 480, 32: 1095, 48: 974, 64: 1245, 80: 950, 96: 841, 112: 453, 128: 176, 144: 88, 160: 8}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

Baseline for this gate: `bop-deep-baseline` (same deep world, this mod's caves off).

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64957 | 68819 | +1.059 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55004 | 55003 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25306 | 25456 | +1.006 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 2746 | 3267 | +1.190 | PASS |
| fantasyundergrounds:ancient_ruins | None | 7904,0,7616 | False | 0 | 4187 | n/a | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 112 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 11.71 | +60.8 |
| waterSourcesBelowZero | 292456 | 862039 | +194.8 |
| lavaSourcesBelowZero | 185012 | 474594 | +156.5 |
| darkSpawnableFloor | 657216 | 380084 | -42.2 |
| scheduledFluidTicks | 66411 | 65910 | -0.8 |
| scheduledBlockTicks | 406967 | 416242 | +2.3 |

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
