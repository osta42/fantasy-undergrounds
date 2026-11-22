# Vanilla baselines (Phase 0)

* Seed `1234567890`, Overworld, region = 32 × 32 chunks centred on chunk (512, 512), i.e. blocks 7936…8447 on X and Z.
* Minecraft 26.2, NeoForge 26.2.0.95, Java 25.0.5 (Temurin), dedicated dev server with `-Xmx3G`.
* Machine: AMD Ryzen 5 7520U (8 logical CPUs, 7 worldgen workers), 7.3 GB RAM, Windows 11.
* Each run uses a fresh world. Raw data: `runs/audit-vanilla/fu-audit/*.json` (git-ignored, regenerate with `tools/run_gates.ps1`).
* "Skeleton" = this mod as of Phase 0: it loads but changes no worldgen. Comparing against it self-tests the tooling.

## Ores, 1,024 chunks (gate 2 baseline)

| ore | vanilla | skeleton | change |
|---|---:|---:|---:|
| coal_ore | 48,887 | 48,711 | −0.4% |
| copper_ore | 54,926 | 54,887 | −0.1% |
| deepslate_coal_ore | 768 | 767 | −0.1% |
| deepslate_copper_ore | 5,664 | 5,664 | 0.0% |
| deepslate_diamond_ore | 23,973 | 23,981 | +0.0% |
| deepslate_gold_ore | 21,094 | 21,061 | −0.2% |
| deepslate_iron_ore | 29,340 | 29,328 | −0.0% |
| deepslate_lapis_ore | 13,239 | 13,261 | +0.2% |
| deepslate_redstone_ore | 32,632 | 32,626 | −0.0% |
| diamond_ore | 382 | 382 | 0.0% |
| gold_ore | 3,751 | 3,750 | −0.0% |
| iron_ore | 44,175 | 44,179 | +0.0% |
| lapis_ore | 8,689 | 8,679 | −0.1% |
| redstone_ore | 2,367 | 2,367 | 0.0% |

Vanilla-vs-vanilla noise between two fresh worlds is up to about 3% for small counts (see the determinism section below), so ±15% leaves real headroom.

## Performance (gate 3 baseline)

| run | chunks | ms per chunk (wall clock, parallel batches) |
|---|---:|---:|
| vanilla, 1,024-chunk audit | 1,024 | 35.9 |
| skeleton, 1,024-chunk audit | 1,024 | 35.1 (−2.3%) |
| vanilla, 2,016 × 2,016 pre-generation | 15,876 | 32.4 (total 8.6 min) |
| skeleton, 2,016 × 2,016 pre-generation | 15,876 | 29.8 (−8.1%) |

Run-to-run noise on this laptop is about ±8%. The +30% gate will be judged on the pre-generation run, which is the larger sample.

## Stability (gate 4 baseline)

2,016 × 2,016 blocks (15,876 chunks), vanilla and skeleton: **0 exceptions, 0 "far chunk" warnings, 0 ERROR lines**. The only warnings are OSHI failing to read Windows process counters at startup, which vanilla also produces and the tooling ignores.

## Structures (gate 6 baseline)

| structure | position | pieces | structure blocks in piece boxes | key blocks |
|---|---|---:|---:|---|
| ancient_city | 7888, 7328 | 86 | 63,537 | 56 reinforced deepslate |
| trial_chambers | 8464, 8432 | 222 | 54,985 | 13 trial spawners |
| stronghold | 8640, 7248 | 164 | 25,308 | 12 end portal frames |
| mineshaft | 8224, 8080 | 178 | 5,083 | 400 rails |

Skeleton: same positions, ratio 1.000 for all four.

## Registration (gate 7 baseline)

66 biomes and 262 placed features, with per-biome, per-step feature order recorded. Skeleton: 0 differences.

## Metrics baseline (1,024 chunks)

| metric | vanilla |
|---|---:|
| air below y = 0 | 4.90% (821,449 of 16,777,216 blocks) |
| water sources below y = 0 | 122,607 |
| lava sources below y = 0 | 24,562 |
| dark spawnable floor positions (y ≤ 40) | 94,594 |
| scheduled fluid ticks after generation | 31,039 |
| scheduled block ticks after generation | 30,898 |

## Determinism (gate 5): **vanilla does not pass gate 5 as written**

| comparison (two fresh worlds, same seed) | terrain only (noise + surface + carvers, 256 chunks) | finished chunks |
|---|---:|---:|
| vanilla A vs vanilla B (7 worker threads) | **0** mismatching | **455 / 1,024** mismatching (1,045 of 24,576 sections) |
| skeleton A vs skeleton B (7 worker threads) | **0** | 589 / 1,024 (1,517 sections) |
| vanilla A vs B, **single worldgen thread** (`-Dmax.bg.threads=1`) | **0** | 192 / 256 |

* The differences are in **decoration**: ore blobs, diorite/andesite/granite/tuff blobs, clay, moss, grass. Total block counts move by about 0.1–1%. They show up at every height. Features that write into neighbouring chunks produce different results depending on which chunk decorates first, and that order is not reproducible in 26.2, **even with one worker thread**.
* Terrain generation (everything before the feature step) is **bit-for-bit reproducible** in vanilla. That is where this mod's caverns, tunnels, canyons, islands and aquifer lakes live.
* The variation in finished chunks is itself noisy: the vanilla pair and the skeleton pair run identical code but differ by 30%. So a relative "no worse than vanilla" gate on finished chunks would pass or fail at random.

**Proposed replacement for gate 5 (needs your approval):**

* **5a (strict, exact):** terrain-stage hashes (noise → surface → carvers) must match 100% between two fresh worlds. Measured on 256 chunks, and on more if you prefer.
* **5b (strict, exact):** each FU decoration feature must be a pure function of (seed, position, config). Tested by running each FU feature twice on identical synthetic input in a unit test and comparing the output block for block. This needs no full-world comparison, so vanilla's order dependence doesn't interfere.
* **5c (reported, not gated):** finished-chunk mismatch rate next to vanilla's own rate, so any large regression is visible.
