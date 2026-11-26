# Fantasy Undergrounds: notes for Claude Code

A cave-generation overhaul for **Minecraft 26.2**, **NeoForge 26.2.0.95**, ModDevGradle 2.0.148, Gradle 9.2.1, **Java 25**. Read `README.md` (what the mod does), `PLAN.md` (design, incl. the "Overhaul" section) and `reports/overhaul-summary.md` (latest test results) before changing world generation.

## Hard rules (from the project brief; do not break them)

- **Vanilla blocks only.** No custom blocks, items, fluids, textures or entities. Density functions, features, placement modifiers, structures, block-state providers and tags are fine.
- **No new biomes**, no changes to the biome source. Environments are noise zones (`ZoneField`), not biomes.
- **No mandatory dependencies.** Stay compatible with Biomes O' Plenty and third-party ore mods (tested with Occultism and Powah).
- **Never remove, replace or reorder other mods' (or vanilla's) features, or the ore step.** Only the three vanilla carvers (`cave`, `cave_extra_underground`, `canyon`) are removed, by config.
- Themed blocks may only replace `#fantasyundergrounds:replaceable_base` or air.
- Large lakes and lava seas come from the vanilla aquifer (`FluidBias`); placed springs are capped by config.
- Overworld only by default. Generation must be **deterministic**: a pure function of (seed, position, config). Config is a NeoForge **STARTUP** config.
- Two Mixins only: `ChunkMapMixin` (noise router wiring) and `DeepLavaMixin` (user-approved engine patch for the deeper floor). Ask before adding more.
- **Test gates** (Section 11), all must pass before moving on: ores within ±15%, chunk generation at most +30% slower than vanilla, a 2,016 × 2,016-block pre-generation with no exceptions or far-chunk warnings, determinism (5a/5b), structures intact, feature registration preserved.
- Never claim compatibility you have not tested. Report stubs and failures honestly. Ask the user when requirements conflict.

## Current state

- World floor **y −96** (overrides `data/minecraft/dimension_type/overworld.json` and `worldgen/noise_settings/overworld.json`, height 416). New worlds only. An earlier −128 version missed the speed gate; the user chose −96.
- "Monumental" stacked chambers (`ZoneField` tiers, `CavernDensity`), macro rock strata (`StrataFeature`), focal lighting, giant formations as structures, ruins built into chamber walls, optional client fog (`FuNetwork`, `FuFogClient`).
- All gates pass, with and without Biomes O' Plenty (`reports/overhaul96*.md`).
- Known issues: in vanilla dripstone/lush cave biomes overlapping a zone, the biome's own decoration still runs (by the rules) and dripstone columns can look crowded in tall chambers; one buried ruin was seen with a shallow water floor.

## Build and install into the user's game

- Build: `gradlew.bat build` on Windows (`./gradlew build` elsewhere). Needs a JDK 25 (`JAVA_HOME`); the first build downloads Minecraft and NeoForge.
- Output: `build\libs\fantasyundergrounds-<version>.jar` (only the mod; the dev-only `fudevtools` code is not in it).
- Install: the user plays with the **Modrinth app**. Copy the jar into the instance's `mods` folder (ask the user for the instance path if you don't know it; instances live in `<Modrinth app directory>\profiles\<instance name>`), deleting any older `fantasyundergrounds-*.jar` first. The instance must be Minecraft 26.2 on NeoForge 26.2.0.95 or newer. Worldgen changes only show in **new worlds**.
- `dist/` holds a prebuilt jar for download; rebuild rather than reuse it after code changes.

## Tests (dev only)

- Gate suite: `tools\run_gates.ps1 -Tag <name>` (Windows) or `tools/run_gates.sh --tag <name>`. It runs vanilla and modded audits on fresh worlds through the dev-only `fudevtools` mod, a `deep-baseline` (the same deep world with this mod's caves off, `-FuDisabled` / `--fu-disabled`) for the ore and structure gates, and writes `reports\<name>.md` via `tools\compare_audit.py` (needs Python 3). Takes about an hour.
- Single audit: `tools\run_audit.ps1 -Variant modded -Label <x> -OreMods` (add `-BopMods` for Biomes O' Plenty, `-FuDisabled` for the deep baseline).
- The PowerShell scripts were updated for `-FuDisabled` / the deep baseline but were only run on Linux (bash versions); check them on first use on Windows.
- `runs/` (worlds, logs, audit JSON) is not in git; reruns recreate it.

## Git

- Work on branch `claude/nice-albattani-9u3e5e` of `osta42/fantasy-undergrounds` unless the user says otherwise. Commit with clear messages; don't open pull requests unless asked.
