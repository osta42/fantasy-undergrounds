package com.fantasyundergrounds.devtools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Gate 7: dump every biome's (post-biome-modifier) carver list and per-step placed feature
 * lists, plus the full placed feature registry. compare_audit.py checks that every entry of
 * the baseline still exists in the modded run, in the same relative order.
 */
final class RegistryDump {
    private RegistryDump() {}

    static Map<String, Object> run(MinecraftServer server) {
        Map<String, Object> out = new LinkedHashMap<>();
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Map<String, Object> perBiome = new TreeMap<>();
        biomes.listElements().forEach(ref -> {
            Biome biome = ref.value();
            BiomeGenerationSettings gen = biome.getGenerationSettings();
            Map<String, Object> b = new LinkedHashMap<>();
            List<String> carvers = new ArrayList<>();
            gen.getCarvers().forEach(h -> carvers.add(key(h)));
            b.put("carvers", carvers);
            Map<String, List<String>> steps = new LinkedHashMap<>();
            List<HolderSet<PlacedFeature>> features = gen.features();
            GenerationStep.Decoration[] names = GenerationStep.Decoration.values();
            for (int i = 0; i < features.size(); i++) {
                List<String> ids = new ArrayList<>();
                features.get(i).forEach(h -> ids.add(key(h)));
                steps.put(i < names.length ? names[i].getSerializedName() : "step_" + i, ids);
            }
            b.put("features", steps);
            perBiome.put(ref.key().identifier().toString(), b);
        });
        out.put("biomes", perBiome);
        List<String> placed = new ArrayList<>();
        server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).listElementIds()
            .forEach(k -> placed.add(k.identifier().toString()));
        placed.sort(String::compareTo);
        out.put("placedFeatures", placed);
        return out;
    }

    private static String key(Holder<?> h) {
        return h.unwrapKey().map(k -> k.identifier().toString()).orElse("<inline>");
    }
}
