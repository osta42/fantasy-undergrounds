package com.fantasyundergrounds.devtools;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Dev-only worldgen audit tooling (PLAN.md section "Test tooling").
 * Completely inert unless a file named fu-audit.properties exists in the server directory.
 */
@Mod(FuDevTools.MODID)
public final class FuDevTools {
    public static final String MODID = "fudevtools";
    static final Logger LOGGER = LogUtils.getLogger();

    private AuditRunner runner;

    public FuDevTools(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarted(ServerStartedEvent event) {
        Path propsFile = event.getServer().getServerDirectory().resolve("fu-audit.properties");
        if (!Files.isRegularFile(propsFile)) {
            return;
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(propsFile)) {
            props.load(r);
        } catch (IOException e) {
            LOGGER.error("[fu-audit] cannot read {}", propsFile, e);
            return;
        }
        this.runner = new AuditRunner(event.getServer(), AuditConfig.from(props));
        LOGGER.info("[fu-audit] armed: {}", this.runner.config());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (this.runner != null) {
            this.runner.tick();
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        this.runner = null;
    }
}
