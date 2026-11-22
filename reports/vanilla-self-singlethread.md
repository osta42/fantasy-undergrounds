# Audit comparison: `st-vanilla-a` (baseline) vs `st-vanilla-a`

Seed 1234567890 / 1234567890; chunks 256 / 256


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 12064 | 12064 | 0.0 | yes | PASS |
| minecraft:copper_ore | 13225 | 13225 | 0.0 | yes | PASS |
| minecraft:deepslate_coal_ore | 261 | 261 | 0.0 | yes | PASS |
| minecraft:deepslate_copper_ore | 1483 | 1483 | 0.0 | yes | PASS |
| minecraft:deepslate_diamond_ore | 6199 | 6199 | 0.0 | yes | PASS |
| minecraft:deepslate_gold_ore | 5755 | 5755 | 0.0 | yes | PASS |
| minecraft:deepslate_iron_ore | 8774 | 8774 | 0.0 | yes | PASS |
| minecraft:deepslate_lapis_ore | 3557 | 3557 | 0.0 | yes | PASS |
| minecraft:deepslate_redstone_ore | 8524 | 8524 | 0.0 | yes | PASS |
| minecraft:diamond_ore | 105 | 105 | 0.0 | yes | PASS |
| minecraft:gold_ore | 1002 | 1002 | 0.0 | yes | PASS |
| minecraft:iron_ore | 11071 | 11071 | 0.0 | yes | PASS |
| minecraft:lapis_ore | 2237 | 2237 | 0.0 | yes | PASS |
| minecraft:redstone_ore | 486 | 486 | 0.0 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 74.72 | 74.72 | 0.0% |

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
| FULL chunks mismatching | 192 / 256 (75.0%) | n/a |
| FULL sections mismatching | 498 / 6144 | n/a |

Mismatching sections by Y band (candidate): {-64: 9, -48: 16, -32: 63, -16: 62, 0: 88, 16: 76, 32: 105, 48: 79}

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 5.20 | 5.20 | 0.0 |
| waterSourcesBelowZero | 16266 | 16266 | 0.0 |
| lavaSourcesBelowZero | 4742 | 4742 | 0.0 |
| darkSpawnableFloor | 33211 | 33211 | 0.0 |
| scheduledFluidTicks | 1269 | 1269 | 0.0 |
| scheduledBlockTicks | 231 | 231 | 0.0 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | PASS |
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
| 5 Determinism - strict, as written (full chunks identical) | FAIL |
| 5a Determinism - terrain before features identical | PASS |
