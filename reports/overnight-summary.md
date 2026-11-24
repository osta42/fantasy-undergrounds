# Overnight summary (Phases 2 re-check → 6)

> Biomes O' Plenty results: pending (the run was still in progress when this draft was committed).

All work ran in the cloud container (4 CPUs, Linux, OpenJDK 25.0.5.1). Every phase was gated against **vanilla baselines recorded on this machine** (`BASELINES.md`, cloud section), with Occultism and Powah loaded. No phase moved on until all of its gates passed.

## Gate results per phase

| phase | build | gated ores vs vanilla | perf, 4,096-chunk audit | perf, 15,876-chunk pre-generation | all Section 11 gates |
|---|---|---|---:|---:|:-:|
| 2 (re-check) | Phases 0–2 | +2.0 … +8.6% | −9.5% | **−7.7%** | PASS |
| 3 Grand Canyons, Floating Islands | `e2f0224` | +2.0 … +9.1% | −7.8% | −5.9% | PASS |
| 4 Forests, Crystal Hollows, Root Systems | `cb91080` | +2.1 … +8.0% | −3.5% | +2.3% | PASS |
| 5 Ancient Depths ruins | `929c759` | +2.1 … +8.2% | −3.0% | −6.5% | PASS |
| 6 compatibility and polish (final) | `06332fb` | +2.1 … +7.3% | −7.3% | +1.4% | PASS |

In every phase:
* **Determinism:** 5a, the terrain hashes before features, matched (0 / 256 chunks differ). 5b, the purity self-test, had 0 mismatches over 200,000 positions, and every FU feature produced identical output on identical input.
* **Ore safety:** the self-test planted ores in a synthetic cave, and no FU feature touched any of them.
* **Stability:** 0 far-chunk warnings, 0 boundary violations by this mod's features, no new exception signatures. The only boundary violations in the logs come from `occultism:grove_underground`, and they occur in vanilla runs too.
* **Structures:** the ancient city, trial chambers, stronghold and mineshaft are at the same positions with ≥ 1.000 of their vanilla blocks.
* **Registration:** 0 problems.

Reports: `reports/phase2-cloud*.md`, `phase3*.md`, `phase4*.md`, `phase5*.md`, `phase6*.md`, `phase6-bop*.md`.

## Open item from before: closed

The Phase 2 pre-generation result (+29.7% on the throttling laptop) measures **−7.7%** here. The same build also measures −9.5% on the audit.

## What was built

* **Phase 3:** Underground Grand Canyons (a separate rare field, a river from the aquifer, cliff levels, rock strata on the walls) and Floating Stone Islands (inside tall Grand Caverns, moss tops). See `reports/images/phase3-canyon.png` and `phase3-floating-island.png`.
* **Phase 4:** Underground Forests (moss, azalea and oak trees, huge mushrooms, cave vines), Crystal Hollows (calcite and amethyst, crystal spires with buds) and Ancient Root Systems (giant winding roots, hanging roots, soil). New features: `crystal` and `root`.
* **Phase 5:** Ancient Depths ruins, a structure with three procedural layouts (a colonnade hall, a sunken crypt and a stepped temple), placed from the config (`ancientRuinSpacing`). See `reports/images/phase5-temple-ruin.png`.
* **Phase 6:** a per-world config stamp with a warning when the config changes (promised in `PLAN.md` §2.8 but never implemented until now), the Biomes O' Plenty compatibility audit, the README, and bash ports of the gate scripts.

## Bugs found and fixed along the way

1. **Floating sea-water slabs:** within 64 blocks below a sea floor the aquifer lowers its flood thresholds, so a fixed "lake" value flooded to sea level. Lake values now follow the aquifer's depth factor (`PLAN.md` §2.5). This also affected Phase 2 Drowned and Infernal lakes under oceans.
2. **Lava near canyons:** the canyon's "no lava" override removed vanilla lava lakes at every depth near a canyon. It is now limited to the river's band. Found by the canyon-local comparison (`reports/phase3-canyon-local.md`).
3. **`locate`:** it returned positions in solid rock (zone cells sealed under shallow ground) and chambers whose decoration is suppressed by lush caves and similar biomes. It now returns open positions and skips those biomes.
4. **Build on Linux:** `gradlew` had lost its executable bit, which also broke the GitHub Actions build.

## Decisions to review (made without being able to ask)

These are written up in `PLAN.md` §8. The main ones:
* Canyon and island rarity: canyons in 30% of 1,024-block cells (about one per 1,900 × 1,900 blocks), islands in about 1 in 90 zone cells.
* Ruins use a code-defined structure instead of template pools, because NBT assets are hard to review. They replace only air or base stone, run after the ore step, and use vanilla loot tables `stronghold_corridor` and `simple_dungeon` (not `ancient_city`, whose loot is exclusive).
* Vanilla trees in forests turn the moss block under their trunk into dirt. That block is always this mod's own moss.

## Not verified (honest gaps)

* **No in-game playtest.** There is no client in the container. Shapes were checked with block counts, cross-section images and self-tests only. Hostile mob density is only measured through the dark-spawnable-floor proxy, which is down about 60%.
* **Terrain mods** (Tectonic, Terralith, Lithostitched) and density-compiling performance mods: **untested**.
* Determinism: the strict finished-chunk wording fails in vanilla too. The approved 5a/5b replacement passes, and the 5c rate is reported in each report.

## Environment changes made for this work

* The cloud environment now allows `maven.neoforged.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`, `api.modrinth.com` and `cdn.modrinth.com`. It also has a setup script that installs OpenJDK 25 for sessions that include this repository.
