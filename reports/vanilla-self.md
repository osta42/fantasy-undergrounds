# Audit comparison: `vanilla-a` (baseline) vs `vanilla-a`

Seed 1234567890 / 1234567890; chunks 1024 / 1024


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 48887 | 48887 | 0.0 | yes | PASS |
| minecraft:copper_ore | 54926 | 54926 | 0.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 768 | 768 | 0.0 | yes | PASS |
| minecraft:deepslate_copper_ore | 5664 | 5664 | 0.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 23973 | 23973 | 0.0 | yes | PASS |
| minecraft:deepslate_gold_ore | 21094 | 21094 | 0.0 | yes | PASS |
| minecraft:deepslate_iron_ore | 29340 | 29340 | 0.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 13239 | 13239 | 0.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 32632 | 32632 | 0.0 | yes | PASS |
| minecraft:diamond_ore | 382 | 382 | 0.0 | yes | PASS |
| minecraft:gold_ore | 3751 | 3751 | 0.0 | yes | PASS |
| minecraft:iron_ore | 44175 | 44175 | 0.0 | yes | PASS |
| minecraft:lapis_ore | 8689 | 8689 | 0.0 | yes | PASS |
| minecraft:redstone_ore | 2367 | 2367 | 0.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 35.95 | 35.95 | 0.0% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| ERROR lines (excluding known dev noise) | 0 | 0 |
| exception signatures | 0 | 0 |

New exception signatures in candidate: 0

## Gate 5 - determinism (two fresh worlds, same seed and config)

| | candidate pair | vanilla pair |
|---|---:|---:|
| terrain-only chunks (noise+surface+carvers) mismatching | 0 / 256 | n/a |
| FULL chunks mismatching | 455 / 1024 (44.4%) | n/a |
| FULL sections mismatching | 1045 / 24576 | n/a |

Mismatching sections by Y band (candidate): {-64: 11, -48: 45, -32: 115, -16: 135, 0: 154, 16: 121, 32: 193, 48: 190, 64: 54, 80: 27}

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 63537 | 63537 | +1.000 | PASS |
| minecraft:trial_chambers | 8464,0,8432 | 8464,0,8432 | True | 54985 | 54985 | +1.000 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25308 | 25308 | +1.000 | PASS |
| minecraft:mineshaft | 8224,0,8080 | 8224,0,8080 | True | 5083 | 5083 | +1.000 | PASS |

## Gate 7 - feature registration

- problems: 0

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 4.90 | 4.90 | 0.0 |
| waterSourcesBelowZero | 122607 | 122607 | 0.0 |
| lavaSourcesBelowZero | 24562 | 24562 | 0.0 |
| darkSpawnableFloor | 94594 | 94594 | 0.0 |
| scheduledFluidTicks | 31039 | 31039 | 0.0 |
| scheduledBlockTicks | 30898 | 30898 | 0.0 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | PASS |
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
| 5 Determinism - strict, as written (full chunks identical) | FAIL |
| 5a Determinism - terrain before features identical | PASS |
| 6 Structures | PASS |
| 7 Registration | PASS |
