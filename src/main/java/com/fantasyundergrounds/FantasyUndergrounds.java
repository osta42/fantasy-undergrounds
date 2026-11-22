package com.fantasyundergrounds;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FantasyUndergrounds.MODID)
public final class FantasyUndergrounds {
    public static final String MODID = "fantasyundergrounds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasyUndergrounds(IEventBus modBus, ModContainer container) {
        LOGGER.info("Fantasy Undergrounds loading (Phase 0 skeleton)");
    }
}
