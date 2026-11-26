# Fantasy Undergrounds

A cave-generation overhaul for **Minecraft 26.2** on **NeoForge 26.2.0.95**. It replaces the Overworld's swiss-cheese caves with fewer, much larger and more varied underground spaces, while keeping the total amount of open space, the ore yield and every other mod's world generation close to vanilla.

* **Vanilla blocks only.** No new blocks, items, fluids, textures or entities. The mod adds density functions, features, placement modifiers, a block-state provider, a structure and tags.
* **No new biomes**, and the biome source is never touched. Environments are noise *zones* that live alongside whatever biomes the world has, vanilla or modded.
* **No dependencies** besides NeoForge.
* **Server-side worldgen.** Clients don't need it to join, though they need it to see the `/fantasyundergrounds` command suggestions.

## Underground environments

| Environment | What it is | Frequency (per zone slot, default settings) |
|---|---|---|
| Ordinary tunnels | Winding worm tunnels linking everything | between environments |
| Grand Caverns | Huge chambers up to ~96 blocks tall: towering pillars, broad natural bridges, rock stalactites and stalagmites, banded strata walls, glowing froglight veins and ceiling clusters, glow-berry vines; giant ones merge into caverns hundreds of blocks across | 11% deep / 8% shallow |
| Drowned Abysses | Huge flooded caverns from the vanilla aquifer: lakes or fully submerged chambers, lit sea-pickle beds | 5% / 6% |
| Infernal Chasms | Long rifts with lava seas, blackstone/basalt/obsidian **spires** (giant formations), crying-obsidian veins, lava falls | 6.5% / 2% |
| Underground Forests | Dry moss-covered chambers with ponds, **giant mushrooms** with shroomlight, azalea and oak trees, cave vines, glowing fungus | 4.5% / 9% |
| Crystal Hollows | Geode-like chambers lined with calcite and amethyst, **giant crystal clusters** with glowing cores, crystal spires with light shards | 6% / 5% |
| Ancient Root Systems | **Giant arching roots** (wood core, mangrove-root shell, glowing sap), hanging roots, rooted-dirt ceilings | 4.5% / 6% |
| Underground Grand Canyons | Long, meandering gorges (360–760 blocks, up to ~70 wide) with cliff levels, natural stone bridges, banded strata, a river and an arched roof | about 3 in 4 of the 1,024-block canyon cells |
| Floating Stone Islands | Clusters of 3–8 moss-topped islands in almost every tall Grand Cavern | most tall Grand Caverns |
| Ancient Depths ruins | Ruin complexes: a hall, crypt or stepped temple with 1–3 wings, joined by collapsed corridors or broken bridges over chasms; vanilla loot | one per 28 × 28 chunks on average |

Giant mushrooms, crystals, roots and spires are structures (`fantasyundergrounds:giant_mushroom`, `giant_crystal`, `giant_root`, `infernal_spire`), so they can be found with `/locate structure`. Only the deep dark (`#fantasyundergrounds:keeps_own_cave_decoration`) keeps its own decoration; everywhere else this mod's decoration is added next to the cave biome's own. Screenshots: `reports/screenshots/` (no shaders, no night vision).

## Configuration

`config/fantasyundergrounds-startup.toml` is a NeoForge **STARTUP** config: it is read once at game start, before worlds load, and applies to every world. Changing it only affects chunks generated afterwards, so seams can appear between old and new chunks. The mod records the config in each world and logs a warning when a world is opened with a different one.

| Section | Option | Default | Effect |
|---|---|---|---|
| general | `enabled` | true | Master switch. `false` gives vanilla caves (vanilla carvers kept). |
| general | `dimensions` | `minecraft:overworld` | Dimensions to overhaul. |
| general | `intensity` | 1.0 | Scales how often every environment appears (0–3). |
| general | `minimumY` / `maximumY` | −58 / 48 | Vertical range caverns and tunnels may carve. |
| general | `removeVanillaCarvers` | true | Removes only `minecraft:cave`, `cave_extra_underground` and `canyon` from Overworld biomes. Modded carvers are never touched. |
| general | `surfaceEntranceFrequency` | 1.0 | Surface cave entrances and sinkholes (1.0 = vanilla). |
| shape | `cavernSizeScale` | 1.0 | Size of zones and caverns (0.5–2). |
| shape | `maxCavernHeight` | 96 | Tallest cavern. Floating islands need ≥ 56. |
| shape | `tunnelFrequency` / `tunnelWidth` | 1.0 / 1.0 | Ordinary tunnels. |
| environments | `<environment>.enabled` / `.frequency` | true / 1.0 | Per-environment switch and frequency multiplier (0–5). |
| environments | `ancientRuinSpacing` | 28 | Average spacing of ruin complexes, in chunks. |
| fluids | `maxWaterSpringsPerChunk` / `maxLavaSpringsPerChunk` | 6 / 4 | Cap on placed springs and falls. Big lakes and lava seas come from the aquifer and are not capped. |

## Commands (operators)

* `/fantasyundergrounds zone` shows the environment, chamber size, islands and canyon at your position.
* `/fantasyundergrounds locate <environment>` finds the nearest open chamber of an environment, skipping chambers inside decoration-keeping cave biomes, and prints a `/tp` command.
* Ruins are a normal structure: `/locate structure fantasyundergrounds:ancient_ruins`.

## Compatibility

* **Ores:** this mod never removes, replaces or reorders any feature, including the ore step. Its own decoration only replaces base stone (`#fantasyundergrounds:replaceable_base`: stone, deepslate, tuff, granite, diorite, andesite) or air, and runs after the ores, so ores from any mod stay where their mod put them. Tested with **Occultism** and **Powah**: every ore is within ±15% of vanilla (see `reports/`).
* **Biome mods:** tested with **Biomes O' Plenty** 26.2.0.0.28 (with TerraBlender and GlitchCore). All test gates pass against a vanilla + BoP baseline: ores within +0.4 … +7.8%, no slowdown, no worldgen warnings, BoP's biomes and blocks unchanged (`reports/phase6-bop.md`).
* **Terrain mods (Tectonic, Terralith, Lithostitched): untested.** The mod rewrites the Overworld noise router by registry key, through one Mixin in `ChunkMap`. If a terrain mod's router no longer references the vanilla cave inputs, the mod falls back to adding its caverns on top of that terrain mod's own caves, and logs a warning.
* **Mods that compile or replace density functions** (C2ME-style): untested.

## How it works (short version)

Caves live in the terrain density function itself. The mod swaps vanilla's cave inputs (`cave_cheese`, `cave_layer`, the spaghetti and noodle caves) for its own zone caverns, canyons and tunnels. So it inherits vanilla's surface protection, interpolation and structure adaptation, and big lakes and lava seas come from the vanilla aquifer. Decoration is added with NeoForge biome modifiers next to the existing features. Everything is a pure function of (seed, position, config). See `PLAN.md` for the design, and `BASELINES.md` and `reports/` for the measurements behind every phase.

## Development

* Build: `./gradlew build` (Java 25).
* Section 11 test gates, run by the dev-only `fudevtools` mod: `tools/run_gates.sh --tag <name>` on Linux/macOS, `tools\run_gates.ps1 -Tag <name>` on Windows. Single audits use `tools/run_audit.sh` / `run_audit.ps1`, and `tools/compare_audit.py` writes the comparison tables. Add `--ore-mods` / `--bop-mods` to load the third-party test mods; these are dev-only and never dependencies of the mod.

## License

All Rights Reserved (see `gradle.properties`). Based on the NeoForge MDK template (`TEMPLATE_LICENSE.txt`).
