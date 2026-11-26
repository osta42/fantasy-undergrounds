# Audit comparison: `bop-vanilla-a` (baseline) vs `bop-overhaul-modded-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

Baseline for this gate: `bop-deep-baseline`, the same deep world (floor y -128) with this mod's caves switched off. Against the 384-block vanilla world the stone/deepslate split of ores whose height range is anchored to the world bottom moves down; see the ore family totals below.

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 277390 | 290994 | +4.9 | yes | PASS |
| minecraft:copper_ore | 267487 | 279392 | +4.5 | yes | PASS |
| minecraft:deepslate_coal_ore | 3200 | 3129 | -2.2 | yes | PASS |
| minecraft:deepslate_copper_ore | 26179 | 24715 | -5.6 | yes | PASS |
| minecraft:deepslate_diamond_ore | 105715 | 102293 | -3.2 | yes | PASS |
| minecraft:deepslate_emerald_ore | 25 | 21 | -16.0 | no | PASS |
| minecraft:deepslate_gold_ore | 92597 | 84644 | -8.6 | yes | PASS |
| minecraft:deepslate_iron_ore | 132680 | 123371 | -7.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 69565 | 65372 | -6.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 143115 | 141324 | -1.3 | yes | PASS |
| minecraft:diamond_ore | 12 | 18 | +50.0 | no | PASS |
| minecraft:emerald_ore | 452 | 461 | +2.0 | yes | PASS |
| minecraft:gold_ore | 16277 | 17096 | +5.0 | yes | PASS |
| minecraft:iron_ore | 186840 | 195665 | +4.7 | yes | PASS |
| minecraft:lapis_ore | 29990 | 31393 | +4.7 | yes | PASS |
| minecraft:redstone_ore | 6273 | 6519 | +3.9 | yes | PASS |
| occultism:silver_ore | 51102 | 53586 | +4.9 | yes | PASS |
| occultism:silver_ore_deepslate | 297475 | 276003 | -7.2 | yes | PASS |
| powah:deepslate_uraninite_ore | 37858 | 36004 | -4.9 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4828 | 4554 | -5.7 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 61447 | 58084 | -5.5 | yes | PASS |
| powah:uraninite_ore | 4310 | 4467 | +3.6 | yes | PASS |
| powah:uraninite_ore_dense | 13 | 12 | -7.7 | no | PASS |
| powah:uraninite_ore_poor | 22542 | 23429 | +3.9 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 32.04 | 42.11 | +31.4% |
| informational: `bop-deep-baseline` (deep world, caves off) | 39.78 | 42.11 | +5.9% |

Deep world alone vs vanilla: +24.2%; this mod's caves and decoration on top of it: +5.9%.

## Ore families vs `bop-vanilla-a` (stone + deepslate variants together; informational)

| ore family | baseline | candidate | change % |
|---|---:|---:|---:|
| minecraft:coal_ore | 281072 | 294123 | +4.6 |
| minecraft:copper_ore | 291378 | 304107 | +4.4 |
| minecraft:diamond_ore | 100218 | 102311 | +2.1 |
| minecraft:emerald_ore | 485 | 482 | -0.6 |
| minecraft:gold_ore | 104565 | 101740 | -2.7 |
| minecraft:iron_ore | 311891 | 319036 | +2.3 |
| minecraft:lapis_ore | 94344 | 96765 | +2.6 |
| minecraft:redstone_ore | 145181 | 147843 | +1.8 |
| occultism:silver_ore | 344771 | 329589 | -4.4 |
| powah:uraninite_ore | 40054 | 40471 | +1.0 |
| powah:uraninite_ore_dense | 4587 | 4566 | -0.5 |
| powah:uraninite_ore_poor | 78072 | 81513 | +4.4 |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 7423 | 6838 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x6836

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2336 / 4096 (57.0%) | n/a |
| FULL sections mismatching | 6239 / 114688 | n/a |
| FU purity self-test: mismatches over 200000 positions | 0 | n/a |
| feature `skin_air` on synthetic cave: identical / changed / illegal / ores touched | True / 30 / 0 / 0 | n/a |
| feature `skin_lava` on synthetic cave: identical / changed / illegal / ores touched | True / 8 / 0 / 0 | n/a |
| feature `skin_water` on synthetic cave: identical / changed / illegal / ores touched | True / 5 / 0 / 0 | n/a |
| feature `skin_strata` on synthetic cave: identical / changed / illegal / ores touched | True / 71 / 0 / 0 | n/a |
| feature `crystal` on synthetic cave: identical / changed / illegal / ores touched | True / 51 / 0 / 0 | n/a |
| feature `root` on synthetic cave: identical / changed / illegal / ores touched | True / 258 / 0 / 0 | n/a |
| feature `pillar` on synthetic cave: identical / changed / illegal / ores touched | True / 205 / 0 / 0 | n/a |

5c (reported only): finished chunks differing - candidate 57.0%

Mismatching sections by Y band (candidate): {-64: 32, -48: 81, -32: 138, -16: 132, 0: 135, 16: 187, 32: 199, 48: 371, 64: 940, 80: 813, 96: 1147, 112: 837, 128: 637, 144: 370, 160: 150, 176: 65, 192: 5}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

Baseline for this gate: `bop-deep-baseline` (same deep world, this mod's caves off).

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64777 | 67828 | +1.047 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 54899 | 54918 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 24874 | 25445 | +1.023 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 2953 | 3663 | +1.240 | PASS |
| fantasyundergrounds:ancient_ruins | None | 7904,0,7616 | False | 0 | 4195 | n/a | PASS |

## Gate 7 - feature registration

- problems: 0
- carvers removed (informational; allowed by design, PLAN.md 2.2): ['minecraft:canyon', 'minecraft:cave', 'minecraft:cave_extra_underground'] in 112 biomes

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 11.29 | +54.9 |
| waterSourcesBelowZero | 292456 | 728018 | +148.9 |
| lavaSourcesBelowZero | 185012 | 852895 | +361.0 |
| darkSpawnableFloor | 657216 | 429011 | -34.7 |
| scheduledFluidTicks | 66411 | 66327 | -0.1 |
| scheduledBlockTicks | 406967 | 416453 | +2.3 |

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
