# Fantasy Undergrounds — Phase 0 plan

Minecraft **26.2**, NeoForge **26.2.0.95**, ModDevGradle 2.0.148, Gradle 9.2.1, Java 25 (Temurin 25.0.5).
Project started from the official `NeoForgeMDKs/MDK-26.2-ModDevGradle` template.

Every API named below was checked against the decompiled 26.2 sources (`build/moddev/artifacts/minecraft-patched-26.2.0.95-sources.jar`, unpacked to `.mcsrc/`, which git ignores) or by `javap` on the NeoForge 26.2.0.95 jar. Places where 26.2 differs from older versions and from my training data are marked **(26.2)**.

---

## 1. Facts verified in 26.2 that the design depends on

| Fact | Where verified |
|---|---|
| `NoiseRouter` is a record with 15 fields: `barrier`, `fluid_level_floodedness`, `fluid_level_spread`, `lava`, `temperature`, `vegetation`, `continents`, `erosion`, `depth`, `ridges`, `preliminary_surface_level`, `final_density`, `vein_toggle`, `vein_ridged`, `vein_gap`. | `NoiseRouter.java` |
| Overworld caves are still built in `NoiseRouterData.underground()`: `cave_cheese` and `cave_layer` noises inline, plus references to the registered functions `minecraft:overworld/caves/{spaghetti_2d, spaghetti_roughness_function, entrances, pillars, noodle}`. The whole underground branch is selected by `range_choice(sloped_cheese, -1e6, 1.5625, surface, underground)`. | `NoiseRouterData.java:297-359` |
| The only place a server builds the router used for generation is `ChunkMap.<init>`, which calls `RandomState.create(NoiseGeneratorSettings, HolderGetter<NoiseParameters>, long)`. The `ServerLevel` is known at that point. | `ChunkMap.java:181-186` |
| `RandomState` wires the router with a bottom-up `DensityFunction.Visitor`. By the time the visitor sees a node, `HolderHolder` has already become `Holder.direct(...)`, so **registry keys are lost inside a visitor**. Any key-based rewriting has to run on the unwired router, before `RandomState.create`. | `RandomState.java`, `DensityFunction.mapAll`, `DensityFunctions.HolderHolder.mapChildren` |
| The structure beardifier is applied **after** the router: `NoiseChunk` computes `final_density + beardifier`. Anything we put inside `final_density` is therefore adapted around structures (`bury`, `beard_thin`, `beard_box`, `encapsulate`). | `NoiseChunk.java:157`, `TerrainAdjustment.java` |
| NeoForge 26.2 has **no** event or registry hook that modifies a noise router or `NoiseGeneratorSettings`. | grep of NeoForge sources and the universal jar |
| NeoForge ships MixinExtras 0.5.5 (`@WrapOperation`, `@Local`). | Gradle cache `io.github.llamalad7:mixinextras-neoforge:0.5.5` |
| `ModConfig.Type` has `COMMON`, `CLIENT`, `SERVER`, `STARTUP`. **`STARTUP` is read at registration time**, before any world loads, on both sides, and is not synced. | `javap net.neoforged.fml.config.ModConfig$Type`; NeoForge docs (26.1 page) |
| Biome modifiers: `neoforge:add_features`, `remove_features`, `add_carvers`, `remove_carvers`, and others. `Biome#getGenerationSettings()` returns the post-modifier settings. | NeoForge jar, `Biome.java:202` |
| Vanilla Overworld biomes carry the carvers `minecraft:cave`, `minecraft:cave_extra_underground`, `minecraft:canyon` (55 of 66 vanilla biome files mention `canyon`). | `data/minecraft/worldgen/biome/*.json` |
| Block tags `minecraft:stone_ore_replaceables`, `minecraft:deepslate_ore_replaceables`, `minecraft:base_stone_overworld` exist. NeoForge tag `c:ores` = `Tags.Blocks.ORES`. Biome tags `c:is_overworld`, `c:is_cave`, `c:is_underground` and `minecraft:is_overworld` exist. | `BlockTags.java:118-120`, NeoForge jar |
| **(26.2)** Entity type constants moved from `EntityType` to `EntityTypes`. `ResourceLocation` is now `Identifier`. `ChunkPos` is a record (`x()`, `z()`). New density function `IntervalSelect`. Minecraft ships unobfuscated. | compile errors and sources |
| The far-chunk warning text is `"Detected setBlock in a far chunk ["`. | `WorldGenRegion.java:277` |

---

## 2. Architecture decisions (Section 2 of the brief)

### 2.1 Environments are zones, not biomes
*(Revised in Phase 1: 2D cells with two vertical slots instead of full 3D cells. This gives direct control of the vertical budget and stacked levels.)*

Zones come from this mod's own deterministic field (`ZoneField`), independent of the biome source:

* The horizontal plane is split into jittered cells, about **192 blocks** wide at `cavernSizeScale = 1.0`. Cell borders are domain-warped by 3D noise (amplitude ~55 blocks), so walls curve and lean with height.
* Each cell has two **slots**: a deep one (centre about y −42…−26) and a shallow one (about y 9…23). Each slot rolls its environment from a **depth-dependent table** by hashing `(seed, cell, slot)`, so rarity is a direct per-slot probability that config can scale.
* A tall deep chamber (≥ 56 blocks) claims the whole column and leaves the shallow slot solid. This keeps the 128-block vertical budget, and two shorter chambers stacked give "multiple vertical levels".
* Chamber footprint: rounded and lobed (radial distance from the cell centre, perturbed by multi-octave noise), always keeping a rock wall at the cell border. **Giant** Grand Caverns (35% of deep ones) reach close to the border, and two adjacent giants of the same environment drop their shared wall and merge into one cavern **hundreds of blocks across**.
* No biomes are registered; the biome source and climate router fields are never touched.
* The same `ZoneField` instance per seed answers both the density functions and the `fantasyundergrounds:in_zone` placement filter.

Default per-slot chances (multiplied by `intensity` × the environment's `frequency`; the remaining ~80% of slots are ordinary rock with tunnels):

| Environment | Deep slot | Shallow slot |
|---|---:|---:|
| Grand Caverns | 6.5% | 4.2% |
| Drowned Abysses | 2.9% | 3.4% |
| Infernal Chasms | 5.2% | 0.8% |
| Underground Forest | 1.3% | 2.1% |
| Crystal Hollows | 2.1% | 1.7% |
| Ancient Root Systems | 0.9% | 1.7% |
| Grand Canyons | separate 2D field (Phase 3): 1,024-block cells, one canyon per cell with 30% × intensity × frequency | |
| Floating Islands | deep Grand Caverns ≥ 60 blocks tall only (Phase 3): 50% × frequency of those, 1–4 islands each (≈1.1% of zone cells) | |
| Ancient Depths ruins | structure placement (Phase 5) | |

### 2.2 Vanilla caves: **replaced**, not layered
Inside the Overworld `final_density`, nodes are swapped **by registry key**:

| Vanilla node | Becomes |
|---|---|
| noise `minecraft:cave_cheese` (sits in the slot that also carries vanilla's top slide) | **FU cavern field**: zone-driven chambers |
| noise `minecraft:cave_layer` | constant `0`. The horizontal layering is removed; FU caverns define their own floors and ceilings |
| `minecraft:overworld/caves/spaghetti_2d` + `spaghetti_roughness_function` | **FU tunnel network** (configurable frequency and width), plus roughness constant `0` |
| `minecraft:overworld/caves/noodle` | **FU noodle tunnels**, or `+∞` when disabled |
| `minecraft:overworld/caves/entrances` | kept **only within ~40 blocks of the surface** (it also carries vanilla 3D spaghetti tunnels everywhere underground, which would otherwise layer under FU caves), plus a bias from the entrance-frequency config |
| `minecraft:overworld/caves/pillars` | kept (it only adds solid rock) |

Vanilla **carvers** `minecraft:cave`, `minecraft:cave_extra_underground` and `minecraft:canyon` are removed from biomes in `#minecraft:is_overworld` / `#c:is_overworld` with `neoforge:remove_carvers`. Only those three ids are removed. Modded carvers and every feature list stay untouched. A config switch restores the vanilla carvers.

Why: keeping cheese caves and carvers under new caverns is exactly the swiss-cheese result the brief forbids. Replacing the nodes in place, rather than adding a `min()` on top, means FU caves inherit vanilla's top slide, bottom slide, old-chunk blending, interpolation, `squeeze` and the structure beardifier for free.

Air budget: total open volume is targeted near vanilla (Phase 1 result: 5.8% air below y=0 against vanilla's 7.3%, every ore within +1.6…+7.1%). The "fantasy" scale comes from concentrating air into fewer, much larger spaces, not from adding air. Large chambers have far less wall area per unit of air than spaghetti tunnels, which also reduces ore air-exposure discards (Section 4.9).

### 2.3 Integration point: one minimal Mixin
* **Target:** `ChunkMap.<init>`, `@WrapOperation` around the single `RandomState.create(NoiseGeneratorSettings, HolderGetter, long)` call for `NoiseBasedChunkGenerator`.
* **What it does:** if the level's dimension is in the config allowlist (default `minecraft:overworld`), it passes the original call a *copy* of the settings whose `NoiseRouter` has been rewritten by `RouterRewriter`. Otherwise it passes the call through unchanged. Nothing is overwritten. The registry, the datapack and other dimensions stay exactly as they were.
* **Why a Mixin is unavoidable:** NeoForge 26.2 has no noise-router hook. The data-only alternative is overriding `data/minecraft/worldgen/noise_settings/overworld.json`, which is a wholesale replacement that breaks or is broken by every terrain mod. Rewriting inside `RandomState`'s own visitor is impossible because keys are already gone (§1). Wrapping the one call site composes with **whatever** noise settings the world actually uses, vanilla or a terrain mod's.
* **Fallback:** if a terrain mod's `final_density` no longer references any of the vanilla cave keys, `RouterRewriter` finds zero matches. It then wraps `final_density = min(original, FU caverns)` with its own interpolation and depth guard, and logs a clear warning. In that mode the terrain mod's own caves stay and FU only adds zone caverns. This is documented as a limitation.
* A small access transformer is needed for `DensityFunctions$Noise` (a `protected` record) so the rewriter can read its noise key.

**Terrain mods likely to conflict (none tested yet):**

| Mod (has a 26.2 NeoForge build on Modrinth) | Why |
|---|---|
| Tectonic 3.0.28 | Ships its own `minecraft:overworld` noise settings and its own cave toggles. Behaviour depends on whether it still references vanilla cave keys; otherwise fallback mode. |
| Terralith 2.6.4 | Overrides Overworld noise settings and density functions; same question. |
| Lithostitched 1.8.0 | Worldgen composition library used by Tectonic/Terralith; may itself modify routers. |
| Any mod with a Mixin into `ChunkMap.<init>` or `RandomState.<init>` | Competing injection; MixinExtras `@WrapOperation` chains but ordering is not guaranteed. |
| Performance mods that compile density functions (C2ME-style) | Must handle custom `DensityFunction` types; untested. |

### 2.4 Materials
* Density only ever produces the generator's default block (stone; deepslate comes from the vanilla surface rule / deepslate gradient), plus fluids from the aquifer. Bulk walls therefore stay inside `stone_ore_replaceables` / `deepslate_ore_replaceables`, exactly like vanilla.
* Themed materials come only from features in `underground_decoration` or later (after `underground_ores`) and are either a **skin ≤ 2 blocks** or **free-standing formations in air**.
* Every FU feature uses one positive replaceable tag, `fantasyundergrounds:replaceable_base` = `stone, deepslate, tuff, granite, diorite, andesite, air, cave_air`. It never uses "any solid block", so an ore from any mod can never be overwritten. Placing after the ore step also means ores never fail to place because a wall has turned into calcite.

### 2.5 Fluids *(implemented in Phase 2)*
Large water and lava bodies come from the vanilla **aquifer**. The rewriter wraps three aquifer inputs with `FluidBias`, which returns the original value outside Drowned/Infernal zones:

| Router input | How the 26.2 aquifer calls it (verified in `Aquifer.NoiseBasedAquifer`) | Drowned Abysses | Infernal Chasms |
|---|---|---|---|
| `fluid_level_floodedness` | block coordinates of each aquifer grid point; > 0.8 floods fully, 0.4–0.8 gives a lake (both thresholds drop within 64 blocks under a sea floor) | 1.0 for "submerged" chambers (~55%), else lake value | lake value |
| `fluid_level_spread` | fluid-level **cell** coordinates (16 × 40 × 16); level = `cellY*40+20 + quantize(spread*10, 3)` | steered to floor + 45% of height | steered to floor + 22% of height |
| `lava` | fluid-type **cell** coordinates (64 × 40 × 64); \|v\| > 0.3 = lava where level ≤ −10 | 0 (always water) | 1 (lava) |

*Lake value (Phase 3):* within 64 blocks below a sea floor the aquifer lowers both thresholds by a depth factor f, so a fixed 0.6 would flood fully to sea level there. `FluidBias` recomputes f from the same 13 preliminary-surface samples the aquifer uses and returns 0.45 − 1.2 f, which is always between the partial (0.4 − 1.2 f) and full (0.8 − 1.1 f) thresholds.

*Canyon rivers (Phase 3):* inside the fluid band whose middle is within 10 blocks of the river level (floor + 4.5, floored to a multiple of 3 by the aquifer, so 2–4 blocks deep) a canyon is a lake steered to that level; everywhere else in the canyon floodedness is −1 (dry), and the lava input is 0, so rivers are always water. Canyon floors are kept at −31…−21 so the river level is reachable.

Each 40-block fluid band can only reach ±10 blocks around its middle. A chamber spanning two bands therefore gets lakes at two levels, and the aquifer separates them with its own rock dams and spillways. That is where the interconnected water systems and natural waterfalls come from. Inside deep-dark regions vanilla never floods, so ancient cities stay dry.

Placed springs and falls are vanilla `spring_water` / `spring_lava_overworld`, placed with `fantasyundergrounds:config_count`. That count is the per-chunk cap from config (`maxWaterSpringsPerChunk` = 4, `maxLavaSpringsPerChunk` = 2), and each attempt can still fail later filters. Vanilla's own spring features in each biome are untouched and not counted against this cap.

### 2.6 Structures
* Caverns live inside `final_density`, so the vanilla beardifier keeps structure interiors solid and wrapped.
* The climate router is untouched, so deep dark placement and therefore ancient cities are unchanged.
* The gate-6 probe compares structure block counts against vanilla at the same positions.
* Geodes and dungeons are features that validate their own placement (`invalid_blocks_threshold`, air checks).

### 2.7 Vertical budget (Overworld: y = −64…319, sea level 63)

| Band | Use |
|---|---|
| −64 … −59 | bedrock and floor buffer; no carving below −58 |
| −58 … −20 | deep band: Infernal, deep Grand Caverns, lava aquifer, tallest chambers |
| −20 … 20 | middle band: most environments, tallest chambers up to ~80 blocks (−50 … +30) |
| 20 … 40 | shallow band: tunnels, small forests and drowned caves; caverns shrink |
| > 40, or < ~20 blocks under the actual surface | solid buffer, enforced by vanilla's top slide in the cheese slot plus an explicit `maxY` (config). Only entrances and sinkholes break through. |

Floating islands appear only in "tall" Grand Cavern cells with at least 60 blocks of clear height.

### 2.8 Config timing
* NeoForge **`STARTUP`** config, file `config/fantasyundergrounds-startup.toml`. It is the only type read before registries and worlds load, and worldgen is server-only, so the lack of sync does not matter.
* It is global, not per world. Changing it only affects **newly generated chunks**, and seams can appear at old/new chunk borders. A hash of the config is stored in level `SavedData` and a warning is logged when it changes for an existing world.

---

## 3. Generation pipeline (which stage does what)

| Chunk status | FU contribution | Ore safety |
|---|---|---|
| `structure_starts` | Ancient Depths structures (Phase 5), vanilla structure system with structure sets and template pools | — |
| `noise` | Rewritten `final_density`: caverns, tunnels, canyons, islands, pillars, bridges. Wrapped aquifer inputs give lakes and lava seas. Vanilla ore veins (`vein_*` untouched) fill solid rock. | Walls are default stone/deepslate |
| `surface` | unchanged | — |
| `carvers` | vanilla Overworld carvers removed (configurable); modded carvers untouched | — |
| `features`: `underground_ores` | **untouched**: every mod's ores run here, into the finished caves | ✔ |
| `features`: `underground_decoration`, `fluid_springs`, `vegetal_decoration` | FU skins, formations, crystals, roots, mushrooms, vines, capped springs. Added via `neoforge:add_features` alongside existing features. Each placed feature has an `fantasyundergrounds:in_zone` placement modifier and is suppressed in `#c:is_cave` biomes plus a configurable data tag (lush caves, dripstone caves, deep dark). | Positive replaceable tag only |

Cave biome overlap rule: a zone still changes **shape** inside lush caves, dripstone caves, deep dark or modded cave biomes, but FU **decoration is suppressed there**, so the cave biome keeps its own identity. An Underground Forest overlapping lush caves just looks like a very large lush cave.

All FU features stay inside the 3×3 chunk write area the generator allows, and the far-chunk warning is gated.

---

## 4. Procedural techniques (Section 7)
* `ZoneField`: domain-warped 3D jittered cells (F1 identity, F2−F1 falloff).
* Caverns: per-environment shape functions built from warped multi-octave simplex noise (fBm), ridged noise, 2D floor and ceiling height fields per cell (in `flat_cache`), and cellular noise for pillars. Thin noise sheets make natural bridges.
* Tunnels: worm-style tunnels from `|n1| + |n2| < width` on two warped 3D noises, at two scales (main tunnels and noodles).
* Canyons *(as built in Phase 3)*: an explicit path per large cell instead of ridged noise, because it gives direct control of rarity and length: a straight axis with a 3-octave meander (±38 blocks), a stepped width profile (river channel, 2–3 cliff levels whose width steps outwards over short ledges, `pow(smoothstep, 4)`), an arched roof over the top 28%, tapered ends, rough walls from 3D noise. Aquifer-fed river at the floor (§2.5).
* Islands *(as built in Phase 3)*: density blobs in the chamber's warped space with a gently domed, noise-perturbed top and a cone tapering to a point underneath (depth 0.9–1.4 × radius, radius 9–21), kept ≥ 14 blocks above the floor and ≥ 16 below the ceiling.
* Everything is pure functions of `(seed, x, y, z, config)`, with no shared mutable state, so it is thread-safe and deterministic. Expensive inner functions run only at interpolation cell corners (vanilla `interpolated`), about 1.2k evaluations per chunk.

---

## 5. Main risks

1. **Ore gate (±15%)** is the hardest constraint. Removing vanilla caves and adding huge chambers changes air exposure. Mitigation: air budget near vanilla per Y band, measured by the audit each phase.
2. **Determinism gate: confirmed conflict.** Vanilla 26.2 finished chunks are *not* reproducible between fresh worlds, even with a single worldgen thread (455/1,024 chunks differ, all in decoration). Terrain before features *is* exactly reproducible. Gate 5 as written cannot be passed by vanilla or by any mod. A replacement (5a/5b/5c) is proposed in `BASELINES.md` and awaits approval.
3. **Terrain mod composition** (Tectonic/Terralith) is untested and may land in fallback mode.
4. **Hostile mob density** in huge dark caverns. The audit measures a dark-spawnable-floor proxy every phase. Real mob counts need a player present and will be measured in-game in Phase 1.
5. **Machine limits:** 7.3 GB RAM and an 8-thread Ryzen 5 7520U. Perf numbers are relative (vanilla vs FU on the same machine). The 2,000 × 2,000 pre-generation gate takes a long time here.
6. 26.2 is new: the NeoForge docs site has no 26.2 page yet (latest is 26.1). All APIs are verified against sources instead.

---

## 6. Test tooling (Section 11)

A separate dev-only mod, **`fudevtools`** (`src/devtools`), never ships. It is inert unless `fu-audit.properties` exists in the server directory. Two run configs: `auditVanilla` loads only `fudevtools`, `auditModded` loads `fantasyundergrounds` and `fudevtools`. Same seed, same region, fresh world each run.

* `tools/run_audit.ps1 -Variant vanilla|modded -Label X [-RadiusChunks 16] [-NoScan] [-NoStructures]`
  (Linux/macOS: `tools/run_audit.sh --variant vanilla|modded --label X [--radius-chunks 16] [--no-scan] [--no-structures]`; the full suite is `tools/run_gates.ps1` / `tools/run_gates.sh`)
* `tools/compare_audit.py BASELINE.json CANDIDATE.json [--candidate2 B.json]` prints the gate table.

How it measures:
* **Generation:** a 32×32-chunk region (1,024 chunks) centred on chunk (512, 512), in 16×16-chunk tiles. Each tile's FULL futures are requested together and the server thread blocks in a `pollTask` loop until all are done, so the worldgen pool works in parallel and **no ticks run** before scanning (the scanned state is pure worldgen).
* **Gate 2:** a histogram of every block in every scanned chunk; `c:ores` subset; ±15% per ore with ≥ 50 baseline blocks (rarer ores reported, not gated).
* **Gate 3:** wall-clock ms per chunk, vanilla vs FU.
* **Gate 4:** `-RadiusChunks 63 -NoScan` → 126×126 chunks = 2,016 × 2,016 blocks; the log is checked for far-chunk warnings and new exception signatures versus the vanilla log.
* **Gate 5:** FNV-1a 64-bit hash of every block state per chunk and per 16-block section (finished chunks), plus a separate 16×16-chunk region generated only to the `carvers` status (terrain without decoration). Two fresh worlds are compared. `-SingleThread` runs worldgen on one worker thread.
* **Gate 6:** locate ancient city, trial chambers, stronghold and mineshaft from the region centre; generate their chunks; count non-air, non-fluid, non-base-stone blocks inside their piece boxes; compare against vanilla (≥ 95% required).
* **Gate 7:** every biome's post-modifier carver list and per-step feature lists, plus the placed-feature registry. Every baseline entry must survive in order (additions allowed; carver removal reported as by-design).
* **Metrics:** % air below y = 0, water/lava source counts below 0, scheduled fluid and block ticks after generation, and a dark-spawnable-floor count (2-high air over a zombie-valid floor with block and sky light 0, y ≤ 40).

Ore mods with 26.2 NeoForge builds, from the Modrinth API (to install from Phase 1): **Occultism** 26.2-1.256.1 (silver ore), **Powah** 8.0.1-alpha (uraninite), **Allthemodium** 3.0.1, **Silent Gear** 4.3.1 (mostly Nether/End ores). Biome mod: **Biomes O' Plenty** 26.2.0.0.28 (needs TerraBlender 26.2.0.0.2). Not on Modrinth for 26.2: Create, Mekanism, Thermal, Immersive Engineering, Mystical Agriculture (CurseForge-only status unverified).

---

## 7. Vanilla baselines

See `BASELINES.md`, generated by the tooling on seed `1234567890`.

---

## 8. Phase notes (as built, Phases 3–6)

Decisions made while the user was away, so they can be reviewed. Each is the most conservative reading of the brief.

### Phase 3: Grand Canyons and Floating Islands
* Canyons use an explicit path per 1,024-block cell (§4) rather than ridged-noise zero lines, which gives direct control over rarity ("very rare": 30% of cells × intensity × frequency) and length.
* Canyon rivers come from the aquifer (§2.5). Canyon floors are kept at −31…−21 so the river level is reachable in the −40…0 fluid band.
* Lake values now follow the aquifer's depth factor near sea floors (§2.5). This also changes Phase 2 terrain slightly, but only where a Drowned or Infernal lake lies within 64 blocks below a sea floor: those used to flood fully to sea level, and are now lakes.
* Islands: 50% × frequency of deep Grand Caverns ≥ 60 blocks tall, 1–3 islands (+1 in giant chambers). They are rolled from a separate hash, so every Phase 1–2 chamber keeps its exact shape.

### Phase 4: Underground Forests, Crystal Hollows, Ancient Root Systems
* Two new features, `crystal` and `root`. Like `pillar`, they only place into open space (air, water, lava), never into solid blocks.
* Forests reuse vanilla configured features (`azalea_tree`, `fancy_oak`, `huge_red/brown_mushroom`, `moss_vegetation`, `cave_vine`, `spore_blossom`) on this mod's moss floors. A vanilla tree turns the one block under its trunk into dirt or rooted dirt; that block is always this mod's own moss, never an ore.
* Shape tweaks are limited to these three environments: flatter forest floors, rounder and spikier crystal hollows. Other environments are bit-identical.
* `locate` now returns a position that is open in the level's final density, and skips chambers in decoration-keeping cave biomes.

### Phase 5: Ancient Depths ruins
* A code-defined structure with one procedural piece (three layouts) instead of template pools. Template pools would need binary NBT assets, which are hard to review; a procedural piece is plain, reviewable Java.
* Stricter than vanilla structures: the piece only replaces air or base stone, and runs in `underground_decoration` (after ores, like the ancient city). Ores, fluids, other structures and other mods' blocks are never overwritten.
* Placement `fantasyundergrounds:config_spread` extends vanilla random spread, so spacing (`ancientRuinSpacing`) and frequency come from the STARTUP config and `/locate structure` works. There is an exclusion zone around ancient cities, and ruins are skipped in the deep dark, Drowned and Infernal zones, and canyons.
* Loot uses vanilla tables only: `chests/stronghold_corridor` (main chest) and `chests/simple_dungeon`. The ancient city table was not used, because it holds loot exclusive to ancient cities.

### Phase 6: compatibility and polish
* Config stamp (`FuConfigStamp`, `SavedData` in the overworld): §2.8 promised it, but it had not been implemented before this phase. It logs a warning when a world is opened with a different worldgen config, once per change.
* Biomes O' Plenty 26.2.0.0.28 + TerraBlender + GlitchCore tested against a vanilla + BoP baseline: all gates pass (`reports/phase6-bop.md`). TerraBlender changes the biome source, not the noise router, so the rewrite stays in full mode.
* Terrain mods (Tectonic, Terralith, Lithostitched) and density-compiling performance mods are still **untested**. None of them was in scope for the gates, and the fallback mode (§2.3) has not been exercised against a real terrain mod.

### Overhaul: monumental chambers (user spec "Scale, Sightlines, and Biome Direction")

* **Deep world (user-approved second engine patch).** `data/minecraft/dimension_type/overworld.json` and `worldgen/noise_settings/overworld.json` override the Overworld to min_y −96 / height 416 (bottom slide moved to −96…−72). `DeepLavaMixin` (`NoiseBasedChunkGenerator.createFluidPicker`, HEAD, cancellable) moves vanilla's hardcoded "lava below y −54" to minY + 10 whenever the noise floor is below −64; it does nothing in a vanilla-height world. New worlds only: an existing world's chunks keep their old height. Ores whose range is anchored to the world bottom (diamond, lower redstone, buried lapis, some modded ores) move down with it; their stone/deepslate split changes, the family totals stay close to vanilla. The first version went to −128 (height 448); measured at +26% generation time for the deeper world alone, it missed the +30% gate, and the user chose −96 instead (+6%).
* **Tiers.** `ZoneField.column(cx, cz)` plans each 192-block cell as either a mega chamber (12% of cells, 76–100 tall, floor 2–14 above the cave floor) or up to three stacked chambers: deep (floor 2–10 above the cave floor, 40–56 tall), mid (floor 34–46 above the cave floor, or 8 above the deep chamber; 40–62 tall) and upper (floor y 2…12, 16–32 tall), with a rock slab between them and shafts (radius 4–8 upper→lower, 9–18 mid→deep) through it. The cave floor is `minimumY` + 4 (default −80). Environment odds per tier follow the spec's vertical progression: forests and roots up high, Grand Caverns everywhere, crystal hollows lower, Infernal and Drowned near the bottom. Below `minimumY` (−84) the rock stays solid: diamonds and lower redstone peak there.
* **Chamber shape** (`CavernDensity.chamber`): lobed footprint with a rock wall kept at the zone border; vault ceiling (flat, curving down only near the walls); floor terraces (9-block steps, 6 in forests) descending to a central basin; Drowned basins and Infernal V-drops; low-amplitude wall relief; rare hourglass pillars (radius 8–17) and stalactites/stalagmites (base 3.5–9.5, 12–46 long) kept out of the open middle; arches rising towards the middle.
* **Fluids.** Grand Cavern lakes at floor + 10% of the height (only the aquifer band holding that level is wet); Root Systems, Crystal Hollows and every span between stacked chambers stay dry, so the vanilla water table never hangs sea water over an open chamber.
* **Macro strata** (`StrataFeature`, once per chunk, underground_decoration step): per environment palette by relative height in the chamber, band borders warped by a 70-block noise; replaces only `#replaceable_base` within 2 blocks of open space, after the ore step, by writing chunk sections directly (same as vanilla's ore feature; all palette blocks are opaque full cubes, so light and heightmaps are unchanged).
* **Focal lighting.** Scattered glow veins, ceiling glow and most glow lichen removed; light comes from lake-bed sea lanterns, soul lanterns, giant crystals (froglight edges, tips and cores), shroomlight, glow berries, lava.
* **Optional client fog.** `FuNetwork` registers an *optional* play payload; the server sends a player's environment when it changes (every 10 ticks check), only if the client has the channel. `FuFogClient` blends fog end distance and colour per environment (`ViewportEvent.RenderFog` for `FogType.ATMOSPHERIC`, `ComputeFogColor`).
* **Gates.** The ore and structure gates compare against `deep-baseline` (`run_audit --fu-disabled`: the same deep world with this mod's caves off). Performance is reported against both vanilla and the deep baseline: the deep world's own cost is dominated by vanilla's noise fill and surface pass over 64 more layers (profiled: `fillFromNoise` 30%, `SurfaceSystem.buildSurface` 23% of worker time; all of this mod's code together ~6%).
