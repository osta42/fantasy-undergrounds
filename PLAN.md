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
| Grand Canyons | separate 2D field (Phase 3) | |
| Floating Islands | inside tall Grand Caverns only (Phase 3) | |
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
| `fluid_level_floodedness` | block coordinates of each aquifer grid point; > 0.8 floods fully, 0.4–0.8 gives a lake | 1.0 for "submerged" chambers (~55%), else 0.6 (lake) | 0.6 (lake) |
| `fluid_level_spread` | fluid-level **cell** coordinates (16 × 40 × 16); level = `cellY*40+20 + quantize(spread*10, 3)` | steered to floor + 45% of height | steered to floor + 22% of height |
| `lava` | fluid-type **cell** coordinates (64 × 40 × 64); \|v\| > 0.3 = lava where level ≤ −10 | 0 (always water) | 1 (lava) |

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
* Canyons: warped 2D ridged noise path, terraced width profile for cliff levels, aquifer-fed river at the floor.
* Islands: density blobs with a flattened top and a noise-perturbed tapered cone underneath.
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
