package com.fantasyundergrounds;

import com.fantasyundergrounds.config.FuConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(FantasyUndergrounds.MODID)
public final class FantasyUndergrounds {
    public static final String MODID = "fantasyundergrounds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasyUndergrounds(IEventBus modBus, ModContainer container) {
        // STARTUP: read immediately, before registries and worlds load (PLAN.md 2.8).
        container.registerConfig(ModConfig.Type.STARTUP, FuConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.fantasyundergrounds.config.FuClientConfig.SPEC);
        // Optional environment-fog channel (clients without the mod are unaffected).
        modBus.addListener(com.fantasyundergrounds.net.FuNetwork::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.fantasyundergrounds.net.FuNetwork::onPlayerTick);
        FuRegistries.register(modBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FuCommands::register);
        // Warn when an existing world is opened with a different worldgen config (PLAN.md 2.8).
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.fantasyundergrounds.config.FuConfigStamp::onServerStarted);
    }
}
