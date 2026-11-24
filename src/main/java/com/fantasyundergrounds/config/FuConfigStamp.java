package com.fantasyundergrounds.config;

import com.fantasyundergrounds.FantasyUndergrounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Remembers, per world, the worldgen config the world was generated with (PLAN.md 2.8). The
 * config is global and STARTUP-only, so changing it later only affects newly generated chunks
 * and seams can appear at the border between old and new chunks. When a world is opened with a
 * different config than it was last opened with, a warning is logged; the stamp then follows the
 * new config, so the warning appears once per change.
 */
public final class FuConfigStamp extends SavedData {
    public static final Codec<FuConfigStamp> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("fingerprint", "").forGetter(s -> s.fingerprint)
    ).apply(i, FuConfigStamp::new));
    public static final SavedDataType<FuConfigStamp> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(FantasyUndergrounds.MODID, "config_stamp"), FuConfigStamp::new, CODEC);

    private String fingerprint;

    public FuConfigStamp() {
        this("");
    }

    private FuConfigStamp(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        check(event.getServer());
    }

    static void check(MinecraftServer server) {
        String current = FuSettings.get().fingerprint();
        FuConfigStamp stamp = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (stamp.fingerprint.isEmpty()) {
            FantasyUndergrounds.LOGGER.info("[Fantasy Undergrounds] Recording the worldgen config for this world.");
        } else if (!stamp.fingerprint.equals(current)) {
            FantasyUndergrounds.LOGGER.warn("[Fantasy Undergrounds] The worldgen config (config/fantasyundergrounds-startup.toml) differs from the "
                + "one this world was last opened with. Only newly generated chunks use the new settings; visible seams can appear "
                + "at the border between old and new chunks.\n  was: {}\n  now: {}", stamp.fingerprint, current);
        } else {
            return;
        }
        stamp.fingerprint = current;
        stamp.setDirty();
    }
}
