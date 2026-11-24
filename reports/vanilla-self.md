# Audit comparison: `vanilla-a` (baseline) vs `vanilla-a`

Seed 1234567890 / 1234567890; chunks 4096 / 4096


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 254724 | 254724 | 0.0 | yes | PASS |
| minecraft:copper_ore | 240702 | 240702 | 0.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 2516 | 2516 | 0.0 | yes | PASS |
| minecraft:deepslate_copper_ore | 21655 | 21655 | 0.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 91429 | 91429 | 0.0 | yes | PASS |
| minecraft:deepslate_emerald_ore | 7 | 7 | 0.0 | no | PASS |
| minecraft:deepslate_gold_ore | 79986 | 79986 | 0.0 | yes | PASS |
| minecraft:deepslate_iron_ore | 106208 | 106208 | 0.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 49570 | 49570 | 0.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 126976 | 126976 | 0.0 | yes | PASS |
| minecraft:diamond_ore | 1427 | 1427 | 0.0 | yes | PASS |
| minecraft:emerald_ore | 278 | 278 | 0.0 | yes | PASS |
| minecraft:gold_ore | 13634 | 13634 | 0.0 | yes | PASS |
| minecraft:iron_ore | 169048 | 169048 | 0.0 | yes | PASS |
| minecraft:lapis_ore | 33883 | 33883 | 0.0 | yes | PASS |
| minecraft:redstone_ore | 8851 | 8851 | 0.0 | yes | PASS |
| occultism:silver_ore | 44884 | 44884 | 0.0 | yes | PASS |
| occultism:silver_ore_deepslate | 262772 | 262772 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore | 29754 | 29754 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 4086 | 4086 | 0.0 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 40771 | 40771 | 0.0 | yes | PASS |
| powah:uraninite_ore | 6587 | 6587 | 0.0 | yes | PASS |
| powah:uraninite_ore_dense | 23 | 23 | 0.0 | no | PASS |
| powah:uraninite_ore_poor | 29564 | 29564 | 0.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 49.23 | 49.23 | 0.0% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 6756 | 6756 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x6756

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

Strict wording (all finished chunks identical): FAIL (fails in vanilla too; see BASELINES.md)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 2552 / 4096 (62.3%) | n/a |
| FULL sections mismatching | 6653 / 98304 | n/a |

5c (reported only): finished chunks differing - candidate 62.3%

Mismatching sections by Y band (candidate): {-64: 98, -48: 215, -32: 454, -16: 692, 0: 946, 16: 862, 32: 1106, 48: 946, 64: 877, 80: 347, 96: 86, 112: 23, 128: 1}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63896 | 63896 | +1.000 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 55015 | 55015 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25308 | 25308 | +1.000 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5216 | 5216 | +1.000 | PASS |

## Gate 7 - feature registration

- problems: 0

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 7.28 | 7.28 | 0.0 |
| waterSourcesBelowZero | 322723 | 322723 | 0.0 |
| lavaSourcesBelowZero | 184911 | 184911 | 0.0 |
| darkSpawnableFloor | 618615 | 618615 | 0.0 |
| scheduledFluidTicks | 66171 | 66171 | 0.0 |
| scheduledBlockTicks | 497270 | 497270 | 0.0 |

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
