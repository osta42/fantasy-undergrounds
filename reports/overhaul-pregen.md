# Audit comparison: `pregen-vanilla` (baseline) vs `overhaul-pregen-modded`

Seed 1234567890 / 1234567890; chunks 15876 / 15876


## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 23.50 | 31.72 | +34.9% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 23050 | 19877 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x19875

New exception signatures in candidate: 0

## Summary

| gate | result |
|---|:-:|
| 3 Performance | FAIL |
| 4 Stability (this run) | PASS |
