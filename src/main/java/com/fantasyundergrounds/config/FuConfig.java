package com.fantasyundergrounds.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * STARTUP config (config/fantasyundergrounds-startup.toml). STARTUP is the only NeoForge config
 * type that is read before registries and worlds load (PLAN.md 2.8). World generation reads an
 * immutable {@link FuSettings} snapshot of these values, never the live values.
 *
 * Changing these only affects newly generated chunks; seams can appear at old/new borders.
 */
public final class FuConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.DoubleValue INTENSITY;
    public static final ModConfigSpec.IntValue MIN_Y;
    public static final ModConfigSpec.IntValue MAX_Y;
    public static final ModConfigSpec.BooleanValue REMOVE_VANILLA_CARVERS;
    public static final ModConfigSpec.DoubleValue ENTRANCE_FREQUENCY;

    public static final ModConfigSpec.DoubleValue CAVERN_SIZE;
    public static final ModConfigSpec.IntValue MAX_CAVERN_HEIGHT;
    public static final ModConfigSpec.DoubleValue TUNNEL_FREQUENCY;
    public static final ModConfigSpec.DoubleValue TUNNEL_WIDTH;

    public static final EnvValues GRAND_CAVERNS;
    public static final EnvValues DROWNED_ABYSSES;
    public static final EnvValues INFERNAL_CHASMS;
    public static final EnvValues UNDERGROUND_FORESTS;
    public static final EnvValues CRYSTAL_HOLLOWS;
    public static final EnvValues ROOT_SYSTEMS;
    public static final EnvValues GRAND_CANYONS;
    public static final EnvValues FLOATING_ISLANDS;
    public static final EnvValues ANCIENT_RUINS;
    public static final ModConfigSpec.IntValue RUIN_SPACING;

    public static final ModConfigSpec.IntValue MAX_WATER_SPRINGS;
    public static final ModConfigSpec.IntValue MAX_LAVA_SPRINGS;

    /** enabled flag + frequency multiplier for one environment. */
    public record EnvValues(ModConfigSpec.BooleanValue enabled, ModConfigSpec.DoubleValue frequency) {}

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("General settings. Only newly generated chunks are affected by changes;",
                "visible seams can appear at the border between old and new chunks.").push("general");
        ENABLED = b.comment("Master switch. false = vanilla cave generation (vanilla carvers are kept too).")
            .define("enabled", true);
        DIMENSIONS = b.comment("Dimensions whose terrain is overhauled. Only dimensions using a noise-based generator",
                "with Overworld-style cave inputs get the full overhaul; others fall back (see README).")
            .defineListAllowEmpty("dimensions", List.of("minecraft:overworld"), () -> "minecraft:overworld", o -> o instanceof String);
        INTENSITY = b.comment("Overall cave generation intensity. Scales how often every environment appears.")
            .defineInRange("intensity", 1.0, 0.0, 3.0);
        MIN_Y = b.comment("Lowest Y that caverns and tunnels may carve (bedrock layers are always protected).")
            .defineInRange("minimumY", -58, -64, 0);
        MAX_Y = b.comment("Highest Y that caverns and tunnels may carve. Vanilla's surface protection also applies.")
            .defineInRange("maximumY", 48, 0, 256);
        REMOVE_VANILLA_CARVERS = b.comment("Remove the vanilla carvers minecraft:cave, minecraft:cave_extra_underground and",
                "minecraft:canyon from Overworld biomes (replaced by this mod's tunnels and canyons).",
                "Carvers added by other mods are never touched.")
            .define("removeVanillaCarvers", true);
        ENTRANCE_FREQUENCY = b.comment("Surface cave entrance / sinkhole frequency. 1.0 = vanilla, 0 = none.")
            .defineInRange("surfaceEntranceFrequency", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Shape of caverns and tunnels.").push("shape");
        CAVERN_SIZE = b.comment("Scale of the environment zones and the caverns inside them (1.0 = ~190-block zones).")
            .defineInRange("cavernSizeScale", 1.0, 0.5, 2.0);
        MAX_CAVERN_HEIGHT = b.comment("Maximum cavern height in blocks.")
            .defineInRange("maxCavernHeight", 80, 16, 120);
        TUNNEL_FREQUENCY = b.comment("Frequency of ordinary tunnel networks. 0 disables them.")
            .defineInRange("tunnelFrequency", 1.0, 0.0, 3.0);
        TUNNEL_WIDTH = b.comment("Width multiplier for ordinary tunnels.")
            .defineInRange("tunnelWidth", 1.0, 0.25, 3.0);
        b.pop();

        b.comment("Environments. 'frequency' multiplies the default chance of each environment.").push("environments");
        GRAND_CAVERNS = env(b, "grandCaverns", "Grand Caverns: enormous chambers with pillars and bridges (uncommon).");
        DROWNED_ABYSSES = env(b, "drownedAbysses", "Drowned Abysses: flooded caverns (uncommon).");
        INFERNAL_CHASMS = env(b, "infernalChasms", "Infernal Chasms: lava caverns, more common deeper.");
        UNDERGROUND_FORESTS = env(b, "undergroundForests", "Underground Forests (rare).");
        CRYSTAL_HOLLOWS = env(b, "crystalHollows", "Crystal Hollows (rare).");
        ROOT_SYSTEMS = env(b, "ancientRootSystems", "Ancient Root Systems (rare).");
        GRAND_CANYONS = env(b, "grandCanyons", "Underground Grand Canyons (very rare).");
        FLOATING_ISLANDS = env(b, "floatingIslands", "Floating Stone Islands, only in the tallest Grand Caverns (extremely rare).");
        ANCIENT_RUINS = env(b, "ancientRuins", "Ancient Depths ruins (very rare).");
        RUIN_SPACING = b.comment("Average spacing of Ancient Depths ruins, in chunks.")
            .defineInRange("ancientRuinSpacing", 64, 16, 512);
        b.pop();

        b.comment("Placed springs and falls. Large lakes and lava seas come from the aquifer and are not capped here.").push("fluids");
        MAX_WATER_SPRINGS = b.comment("Maximum placed water springs/falls per chunk.")
            .defineInRange("maxWaterSpringsPerChunk", 4, 0, 64);
        MAX_LAVA_SPRINGS = b.comment("Maximum placed lava springs/falls per chunk.")
            .defineInRange("maxLavaSpringsPerChunk", 2, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    private static EnvValues env(ModConfigSpec.Builder b, String name, String comment) {
        b.comment(comment).push(name);
        EnvValues v = new EnvValues(b.define("enabled", true), b.defineInRange("frequency", 1.0, 0.0, 5.0));
        b.pop();
        return v;
    }

    private FuConfig() {}
}
