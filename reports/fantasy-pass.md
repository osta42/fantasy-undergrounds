# Fantasy pass (after "those look vanilla as hell")

Goal: match the user's reference sheet (huge, self-lit, set-piece-filled underground) in the vanilla renderer, with environments "rather common". Shaders come later on the user's side.

## Gate results, final build (`reports/fantasy-final.md`, `reports/fantasy-final-pregen.md`)

All Section 11 gates pass against the cloud vanilla baselines (Occultism and Powah loaded):

| | result |
|---|---|
| ores, gated | −10.0 … +2.7% (limit ±15%) |
| speed, 4,096-chunk audit | +5.0% |
| speed, 15,876-chunk pre-generation | +6.2% (limit +30%) |
| stability | 0 far-chunk warnings, 0 new exceptions |
| determinism | terrain hashes identical (0/256 chunks differ), purity 0 mismatches, all features identical on reruns |
| structures | ancient city, trial chambers, stronghold, mineshaft intact |
| registration | 0 problems |

Air below y = 0 is now 12.7% (vanilla 7.3%), concentrated in large spaces. Water sources are about 3.3× vanilla and lava sources about 2.9×.

## What changed

1. **Scale and frequency:**
   * About 37% of deep and 36% of shallow zone slots hold an environment. Chambers are up to 96 tall, and giant chambers merge for every deep environment.
   * There are towering pillars, broad arched natural bridges, and rock stalactites and stalagmites carved into the terrain.
   * Canyons cover 75% of their cells, are wider, and have stone bridges.
   * Most tall Grand Caverns hold clusters of 3–8 floating islands.
   * Ordinary tunnels were reduced to protect ores.
2. **Light and decoration:**
   * Grand Caverns: strata walls, froglight glow veins, glowstone and froglight ceiling clusters, glow-berry vines, hanging roots.
   * Forests: shroomlight. Crystal Hollows: froglight crystal cores and end-rod light shards.
   * Infernal Chasms: crying obsidian. Drowned Abysses: sea pickles. Root Systems: sap nodes. Canyons: glow veins and ceiling lights.
   * Decoration now also appears inside vanilla cave biomes; only the deep dark keeps its own.
3. **Giant formations (structures):** `giant_mushroom`, `giant_crystal`, `giant_root` and `infernal_spire`. Each spawns only in its own environment, is positioned from the level's final density, and is built per chunk from a pure shape function.
4. **Ruins:** complexes made of a central room and 1–3 wings, joined by collapsed corridors or broken bridges over chasms, every 28 chunks on average.
5. **Fluids:**
   * Forests are dry except shallow water ponds.
   * Crystal Hollows are dry.
   * Canyons keep their river and stay dry even where they cross a zone.
   * Forest, crystal and root chamber floors stay above y = −48, because vanilla fills every cave below −54 with lava.

## Known limits

* Canyons are still the darkest environment without shaders.
* Vanilla 26.2 sulfur caves (yellow and red) often appear inside chambers. Their features can't be removed under the brief's rules.
* The screenshots come from a software-rendered client (`runs/shots`, `runShotsClient`, devtools `ShotTour`): `reports/screenshots/`, with no shaders and no night vision.
