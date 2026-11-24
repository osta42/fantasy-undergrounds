> **Informational, not a gate.** 1,024 chunks centred on a Grand Canyon (chunk 489, 465) instead of the standard audit region, vanilla vs modded at the same place, to show the local effect of a canyon. The standard Phase 3 gates are in `phase3.md`. Findings: (1) the canyon's "no lava" override applied at every depth instead of only the river's fluid band, which removed nearby deep vanilla lava lakes (lava −93.5% here); fixed in the Phase 4 build and gated with Phase 4. (2) `deepslate_coal_ore` +29.5% (600 → 777 blocks): this vanilla area is unusually cave-heavy (15.9% air below y = 0 against 10.8% with the mod), so more rock is left for ores; every other gated ore is within ±10% here, and the gated value in the standard region is +9.1%.

# Audit comparison: `p3local-vanilla` (baseline) vs `p3local-modded`

Seed 1234567890 / 1234567890; chunks 1024 / 1024


## Gate 2 - ore audit (tolerance +/-15%, ores with >=50 baseline blocks gated)

| ore | baseline | candidate | change % | gated | pass |
|---|---:|---:|---:|:-:|:-:|
| minecraft:coal_ore | 133761 | 136295 | +1.9 | yes | PASS |
| minecraft:copper_ore | 163455 | 167844 | +2.7 | yes | PASS |
| minecraft:deepslate_coal_ore | 600 | 777 | +29.5 | yes | FAIL |
| minecraft:deepslate_copper_ore | 9988 | 10261 | +2.7 | yes | PASS |
| minecraft:deepslate_diamond_ore | 21364 | 22863 | +7.0 | yes | PASS |
| minecraft:deepslate_emerald_ore | 19 | 18 | -5.3 | no | PASS |
| minecraft:deepslate_gold_ore | 19711 | 21276 | +7.9 | yes | PASS |
| minecraft:deepslate_iron_ore | 32876 | 34303 | +4.3 | yes | PASS |
| minecraft:deepslate_lapis_ore | 12869 | 13724 | +6.6 | yes | PASS |
| minecraft:deepslate_redstone_ore | 30004 | 32007 | +6.7 | yes | PASS |
| minecraft:diamond_ore | 472 | 483 | +2.3 | yes | PASS |
| minecraft:emerald_ore | 520 | 526 | +1.2 | yes | PASS |
| minecraft:gold_ore | 3901 | 3952 | +1.3 | yes | PASS |
| minecraft:iron_ore | 53269 | 54883 | +3.0 | yes | PASS |
| minecraft:lapis_ore | 10263 | 10676 | +4.0 | yes | PASS |
| minecraft:redstone_ore | 2760 | 2893 | +4.8 | yes | PASS |
| occultism:silver_ore | 16166 | 16639 | +2.9 | yes | PASS |
| occultism:silver_ore_deepslate | 64255 | 68494 | +6.6 | yes | PASS |
| powah:deepslate_uraninite_ore | 7090 | 7531 | +6.2 | yes | PASS |
| powah:deepslate_uraninite_ore_dense | 1063 | 1104 | +3.9 | yes | PASS |
| powah:deepslate_uraninite_ore_poor | 9947 | 10536 | +5.9 | yes | PASS |
| powah:uraninite_ore | 1885 | 1992 | +5.7 | yes | PASS |
| powah:uraninite_ore_dense | 5 | 7 | +40.0 | no | PASS |
| powah:uraninite_ore_poor | 9491 | 9909 | +4.4 | yes | PASS |

## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 30.77 | 30.04 | -2.4% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 801 | 1265 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x1264

New exception signatures in candidate: 0

## Gate 6 - structures (found, and >= 95% of baseline structure blocks present)

| structure | baseline pos | candidate pos | same pos | baseline blocks | candidate blocks | ratio | pass |
|---|---|---|:-:|---:|---:|---:|:-:|
| minecraft:ancient_city | 7888,0,7328 | 7888,0,7328 | True | 64160 | 64242 | +1.001 | PASS |
| minecraft:trial_chambers | 7856,0,7280 | 7856,0,7280 | True | 71014 | 70860 | +0.998 | PASS |
| minecraft:stronghold | 8640,0,7248 | 8640,0,7248 | True | 25306 | 25507 | +1.008 | PASS |
| minecraft:mineshaft | 7872,0,7520 | 7872,0,7520 | True | 1871 | 2550 | +1.363 | PASS |

## Metrics (not gates)

| metric | baseline | candidate | change % |
|---|---:|---:|---:|
| airBelowZeroPct | 15.93 | 10.75 | -32.5 |
| waterSourcesBelowZero | 20348 | 80737 | +296.8 |
| lavaSourcesBelowZero | 118432 | 7715 | -93.5 |
| darkSpawnableFloor | 196488 | 91786 | -53.3 |
| scheduledFluidTicks | 723 | 746 | +3.2 |
| scheduledBlockTicks | 186459 | 189313 | +1.5 |

## Summary

| gate | result |
|---|:-:|
| 2 Ore audit | FAIL |
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
| 6 Structures | PASS |
