package com.fantasyundergrounds.config;

import com.fantasyundergrounds.world.Environment;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of {@link FuConfig} taken once, the first time world generation needs it.
 * Every worldgen thread reads this snapshot, so generation cannot change mid-world if the file
 * is edited while the game runs (determinism, PLAN.md 2.8).
 */
public record FuSettings(
    boolean enabled,
    Set<String> dimensions,
    double intensity,
    int minY,
    int maxY,
    boolean removeVanillaCarvers,
    double entranceFrequency,
    double cavernSize,
    int maxCavernHeight,
    double tunnelFrequency,
    double tunnelWidth,
    Map<Environment, Double> envFrequency,
    int ruinSpacing,
    int maxWaterSprings,
    int maxLavaSprings
) {
    private static volatile FuSettings current;

    public static FuSettings get() {
        FuSettings s = current;
        if (s == null) {
            synchronized (FuSettings.class) {
                s = current;
                if (s == null) {
                    s = fromConfig();
                    current = s;
                }
            }
        }
        return s;
    }

    /** Effective frequency multiplier for an environment (0 when disabled). */
    public double frequency(Environment env) {
        return this.envFrequency.getOrDefault(env, 0.0);
    }

    private static FuSettings fromConfig() {
        Map<Environment, Double> f = new EnumMap<>(Environment.class);
        put(f, Environment.GRAND_CAVERNS, FuConfig.GRAND_CAVERNS);
        put(f, Environment.DROWNED_ABYSSES, FuConfig.DROWNED_ABYSSES);
        put(f, Environment.INFERNAL_CHASMS, FuConfig.INFERNAL_CHASMS);
        put(f, Environment.UNDERGROUND_FORESTS, FuConfig.UNDERGROUND_FORESTS);
        put(f, Environment.CRYSTAL_HOLLOWS, FuConfig.CRYSTAL_HOLLOWS);
        put(f, Environment.ROOT_SYSTEMS, FuConfig.ROOT_SYSTEMS);
        put(f, Environment.GRAND_CANYONS, FuConfig.GRAND_CANYONS);
        put(f, Environment.FLOATING_ISLANDS, FuConfig.FLOATING_ISLANDS);
        put(f, Environment.ANCIENT_RUINS, FuConfig.ANCIENT_RUINS);
        int minY = FuConfig.MIN_Y.getAsInt();
        int maxY = Math.max(minY + 16, FuConfig.MAX_Y.getAsInt());
        return new FuSettings(
            FuConfig.ENABLED.getAsBoolean(),
            Set.copyOf(FuConfig.DIMENSIONS.get().stream().map(String::valueOf).collect(Collectors.toSet())),
            FuConfig.INTENSITY.getAsDouble(),
            minY,
            maxY,
            FuConfig.REMOVE_VANILLA_CARVERS.getAsBoolean(),
            FuConfig.ENTRANCE_FREQUENCY.getAsDouble(),
            FuConfig.CAVERN_SIZE.getAsDouble(),
            FuConfig.MAX_CAVERN_HEIGHT.getAsInt(),
            FuConfig.TUNNEL_FREQUENCY.getAsDouble(),
            FuConfig.TUNNEL_WIDTH.getAsDouble(),
            Map.copyOf(f),
            FuConfig.RUIN_SPACING.getAsInt(),
            FuConfig.MAX_WATER_SPRINGS.getAsInt(),
            FuConfig.MAX_LAVA_SPRINGS.getAsInt()
        );
    }

    private static void put(Map<Environment, Double> map, Environment env, FuConfig.EnvValues v) {
        map.put(env, v.enabled().getAsBoolean() ? v.frequency().getAsDouble() : 0.0);
    }

    /** Stable text form, hashed into level data to warn when an existing world's config changes. */
    public String fingerprint() {
        return List.of(this.enabled, this.intensity, this.minY, this.maxY, this.removeVanillaCarvers, this.entranceFrequency,
            this.cavernSize, this.maxCavernHeight, this.tunnelFrequency, this.tunnelWidth, new java.util.TreeMap<>(this.envFrequency),
            this.ruinSpacing, this.maxWaterSprings, this.maxLavaSprings).toString();
    }
}
