package com.fantasyundergrounds.world;

import com.fantasyundergrounds.FantasyUndergrounds;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.density.RouterRewriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Entry points shared by the Mixin, density functions and placement filters. */
public final class FuWorldgen {
    private static final Map<Long, ZoneField> ZONES = new ConcurrentHashMap<>();

    private FuWorldgen() {}

    /** The one zone field per world seed; density and decoration both use it. */
    public static ZoneField zones(long seed) {
        return ZONES.computeIfAbsent(seed, s -> new ZoneField(s, FuSettings.get()));
    }

    public static boolean appliesTo(ResourceKey<Level> dimension) {
        FuSettings s = FuSettings.get();
        return s.enabled() && s.dimensions().contains(dimension.identifier().toString());
    }

    /** Called from ChunkMapMixin just before vanilla builds the RandomState for a level. */
    public static NoiseGeneratorSettings settingsFor(ResourceKey<Level> dimension, NoiseGeneratorSettings original, long seed) {
        if (!appliesTo(dimension)) {
            return original;
        }
        RouterRewriter.Result r = RouterRewriter.rewrite(original, seed, dimension.identifier().toString());
        FantasyUndergrounds.LOGGER.info("[Fantasy Undergrounds] {}: {}", dimension.identifier(), r.report());
        return r.settings();
    }
}
