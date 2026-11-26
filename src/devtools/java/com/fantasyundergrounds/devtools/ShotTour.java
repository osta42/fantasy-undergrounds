package com.fantasyundergrounds.devtools;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev-only client screenshot tour. Inert unless {@code fu-shots.properties} exists in the game
 * directory. Once a singleplayer world is loaded it puts the player in spectator mode with night
 * vision and no HUD, then for each shot teleports, waits until the visible chunks are rendered,
 * saves {@code screenshots/<name>.png} and finally quits the game.
 *
 * <p>Shot lines ({@code shot.NN}, processed in key order):
 * <pre>
 *   pos|name|x|y|z|yaw|pitch
 *   env|environment|fromX|fromZ|yaw1,yaw2,...|pitch    (nearest open spot of a Fantasy Undergrounds
 *                                                     environment; for grand_canyons the yaws are
 *                                                     relative to the canyon's heading)
 * </pre>
 * Environments are resolved through {@code com.fantasyundergrounds.FuCommands#nearestOpen} by
 * reflection, so this dev mod keeps no compile-time dependency on the cave mod.
 */
@EventBusSubscriber(modid = FuDevTools.MODID, value = Dist.CLIENT)
public final class ShotTour {
    private record Shot(String name, double x, double y, double z, float yaw, float pitch) {}

    private static List<String> specs;
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static int specIndex;
    private static int shotIndex;
    private static int phase;
    private static int ticks;
    private static int worldTicks;
    private static int stable;
    private static CompletableFuture<double[]> pending;
    private static String pendingSpec;

    private ShotTour() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (specs == null) {
            specs = load(mc.gameDirectory.toPath().resolve("fu-shots.properties"));
        }
        if (specs.isEmpty() || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (++worldTicks == 60) {
            for (String cmd : new String[] {"gamemode spectator @a", "effect give @a minecraft:night_vision infinite 0 true",
                "time set 6000", "weather clear", "gamerule doDaylightCycle false"}) {
                command(server, cmd);
            }
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            FuDevTools.LOGGER.info("[fu-shots] tour starting: {} entries", specs.size());
        }
        if (worldTicks < 100) {
            return;
        }
        // Resolve the next spec into shots when the queue is empty.
        if (shotIndex >= SHOTS.size()) {
            if (pending != null) {
                if (!pending.isDone()) {
                    return;
                }
                addEnvShots(pendingSpec, pending.join());
                pending = null;
                return;
            }
            if (specIndex >= specs.size()) {
                FuDevTools.LOGGER.info("[fu-shots] tour complete: {} screenshots", SHOTS.size());
                specs = List.of();
                mc.stop();
                return;
            }
            String spec = specs.get(specIndex++);
            String[] f = spec.split("\\|");
            if (f[0].equals("pos")) {
                SHOTS.add(new Shot(f[1], d(f[2]), d(f[3]), d(f[4]), (float) d(f[5]), (float) d(f[6])));
            } else if (f[0].equals("env")) {
                pendingSpec = spec;
                pending = server.submit(() -> nearestOpen(server.overworld(), f[1], d(f[2]), d(f[3])));
            }
            return;
        }
        Shot s = SHOTS.get(shotIndex);
        switch (phase) {
            case 0 -> {
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", s.x(), s.y(), s.z(), s.yaw(), s.pitch()));
                ticks = 0;
                stable = 0;
                phase = 1;
            }
            case 1 -> {
                // Ready when the teleport has arrived, every chunk within 3 of the player is
                // loaded, and the render queue has stayed empty for 2 seconds.
                ticks++;
                boolean arrived = mc.player.distanceToSqr(s.x(), s.y(), s.z()) < 4.0;
                boolean chunksIn = arrived;
                int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
                for (int dx = -3; dx <= 3 && chunksIn; dx++) {
                    for (int dz = -3; dz <= 3 && chunksIn; dz++) {
                        chunksIn = mc.level.getChunkSource().hasChunk(pcx + dx, pcz + dz);
                    }
                }
                stable = chunksIn && mc.levelRenderer.hasRenderedAllSections() ? stable + 1 : 0;
                boolean ready = ticks > 60 && stable > 40;
                if (ready || ticks > 2400) {
                    FuDevTools.LOGGER.info("[fu-shots] {} ready after {} ticks (all sections rendered: {})", s.name(), ticks,
                        mc.levelRenderer.hasRenderedAllSections());
                    ticks = 0;
                    phase = 2;
                }
            }
            case 2 -> {
                if (++ticks > 20) {
                    Screenshot.grab(mc.gameDirectory, s.name() + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                        msg -> FuDevTools.LOGGER.info("[fu-shots] {}: {}", s.name(), msg.getString()));
                    ticks = 0;
                    phase = 3;
                }
            }
            default -> {
                if (++ticks > 10) {
                    shotIndex++;
                    phase = 0;
                }
            }
        }
    }

    private static void addEnvShots(String spec, double[] pos) {
        String[] f = spec.split("\\|");
        if (pos == null) {
            FuDevTools.LOGGER.warn("[fu-shots] no {} found near {}, {}", f[1], f[2], f[3]);
            return;
        }
        FuDevTools.LOGGER.info("[fu-shots] {} at {} {} {}", f[1], (int) pos[0], (int) pos[1], (int) pos[2]);
        float base = pos.length > 3 ? (float) pos[3] : 0.0F;
        for (String yaw : f[4].split(",")) {
            float y = base + Float.parseFloat(yaw);
            SHOTS.add(new Shot(f[1] + "_" + specIndex + "_" + yaw.replace('-', 'm'), pos[0], pos[1], pos[2], y, (float) d(f[5])));
        }
    }

    private static double[] nearestOpen(ServerLevel level, String env, double x, double z) {
        try {
            Method m = Class.forName("com.fantasyundergrounds.FuCommands")
                .getMethod("nearestOpen", ServerLevel.class, String.class, double.class, double.class);
            return (double[]) m.invoke(null, level, env, x, z);
        } catch (ReflectiveOperationException e) {
            FuDevTools.LOGGER.error("[fu-shots] cannot resolve {}", env, e);
            return null;
        }
    }

    private static void command(IntegratedServer server, String cmd) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
    }

    private static double d(String s) {
        return Double.parseDouble(s.trim());
    }

    private static List<String> load(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        } catch (IOException e) {
            FuDevTools.LOGGER.error("[fu-shots] cannot read {}", file, e);
            return List.of();
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        p.stringPropertyNames().stream().filter(k -> k.startsWith("shot.")).forEach(k -> sorted.put(k, p.getProperty(k)));
        return new ArrayList<>(sorted.values());
    }
}
