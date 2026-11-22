# Audit comparison: `pregen-vanilla` (baseline) vs `phase0-pregen-modded`

Seed 1234567890 / 1234567890; chunks 15876 / 15876


## Gate 3 - performance (<= +30%)

| | baseline | candidate | change |
|---|---:|---:|---:|
| ms per chunk (wall, parallel batch) | 32.36 | 29.75 | -8.1% |

## Gate 4 - log check

| | baseline | candidate |
|---|---:|---:|
| far-chunk warnings | 0 | 0 |
| ERROR lines (excluding known dev noise) | 0 | 0 |
| exception signatures | 0 | 0 |

New exception signatures in candidate: 0

## Summary

| gate | result |
|---|:-:|
| 3 Performance | PASS |
| 4 Stability (this run) | PASS |
