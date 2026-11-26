package com.fantasyundergrounds.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings ({@code fantasyundergrounds-client.toml}). Not part of world generation. */
public final class FuClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENVIRONMENT_FOG;
    public static final ModConfigSpec.DoubleValue FOG_STRENGTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENVIRONMENT_FOG = b
            .comment("Environment fog: each underground environment gets its own fog colour and density.",
                "Only works on servers (or single-player worlds) running this mod; others keep vanilla fog.")
            .define("environmentFog", true);
        FOG_STRENGTH = b
            .comment("Fog strength. 1.0 = default, 0.5 = thinner and subtler, 2.0 = twice as dense.")
            .defineInRange("fogStrength", 1.0, 0.0, 3.0);
        SPEC = b.build();
    }

    private FuClientConfig() {}
}
