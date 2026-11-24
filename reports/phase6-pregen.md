# Audit comparison: `pregen-vanilla` (baseline) vs `phase6-pregen-modded`

Seed 1234567890 / 1234567890; chunks 15876 / 15876


## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 20.84 | 21.13 | +1.4% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| unsafe reads / far writes by this mod's features | 0 | 0 |
| ERROR lines (excluding known dev noise) | 22607 | 20341 |
| exception signatures | 0 | 0 |

Worldgen boundary violations by feature (candidate; only this mod's are gated): `occultism:grove_underground` x20339

New exception signatures in candidate: 0

## Summary

| gate | result |
|---|:-:|
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
