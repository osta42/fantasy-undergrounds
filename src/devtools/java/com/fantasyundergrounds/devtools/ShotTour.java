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
import net.minecraft.world.level.levelgen.DensityFunction;
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
 *   env|environment|fromX|fromZ|yaw1,yaw2,...|pitch[|look]   (nearest open spot of a Fantasy
 *       Undergrounds environment, moved to the most open viewpoint nearby; yaws are relative to
 *       the longest sightline, or to the direction of the found spot in "look" mode)
 * </pre>
 * Environments are resolved through {@code com.fantasyundergrounds.FuCommands#nearestOpen} by
 * reflection, so this dev mod keeps no compile-time dependency on the cave mod.
 */
@EventBusSubscriber(modid = FuDevTools.MODID, value = Dist.CLIENT)
public final class ShotTour {
    private record Shot(String name, double x, double y, double z, float yaw, float pitch) {}

    private static List<String> specs;
    private static final Properties options = new Properties();
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
            for (String cmd : new String[] {"gamemode spectator @a", "time set 6000", "weather clear"}) {
                command(server, cmd);
            }
            // nightvision=false shows the caves with their own light only.
            if (!"false".equals(options.getProperty("nightvision"))) {
                command(server, "effect give @a minecraft:night_vision infinite 0 true");
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
            } else if (f[0].equals("structure")) {
                // structure|id|fromX|fromZ|yaws|pitch : camera inside a ruin's first room, or on a
                // ring around a formation, facing it.
                pendingSpec = "env|" + f[1].replace(':', '_') + "|" + f[2] + "|" + f[3] + "|" + f[4] + "|" + f[5];
                pending = server.submit(() -> structureView(server.overworld(), f[1], d(f[2]), d(f[3])));
            } else if (f[0].equals("env")) {
                pendingSpec = spec;
                boolean look = f.length > 6 && f[6].equals("look");
                pending = server.submit(() -> {
                    double[] base = nearestOpen(server.overworld(), f[1], d(f[2]), d(f[3]));
                    return base == null ? null : viewpoint(server.overworld(), base, look);
                });
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

    /**
     * Best camera position near {@code base}, judged on the level's final terrain density: open
     * at the camera and with the longest sightlines (8 horizontal rays). Returns {x, y, z, yaw}
     * facing along the longest sightline. In {@code look} mode the camera sits on a ring 30-50
     * blocks out and faces the base point (e.g. a floating island).
     */
    private static double[] viewpoint(ServerLevel level, double[] base, boolean look) {
        DensityFunction f = level.getChunkSource().randomState().router().finalDensity();
        double bestScore = -1;
        double[] best = new double[] {base[0], base[1], base[2], base.length > 3 ? base[3] : 0.0};
        for (int dy = 0; dy <= 18; dy += 6) {
            for (int dx = -60; dx <= 60; dx += 12) {
                for (int dz = -60; dz <= 60; dz += 12) {
                    double ring = Math.hypot(dx, dz);
                    if (look && (ring < 30 || ring > 52)) {
                        continue;
                    }
                    double px = base[0] + dx, py = base[1] + dy, pz = base[2] + dz;
                    if (open(f, px, py, pz) > -0.2 || open(f, px, py - 3, pz) > 0) {
                        continue;
                    }
                    double score = 0, bestLen = -1, bestYaw = 0;
                    for (int k = 0; k < 8; k++) {
                        double yaw = k * 45.0;
                        double ddx = -Math.sin(Math.toRadians(yaw)), ddz = Math.cos(Math.toRadians(yaw));
                        int len = 0;
                        for (int step = 3; step <= 90; step += 3) {
                            if (open(f, px + ddx * step, py, pz + ddz * step) > 0) {
                                break;
                            }
                            len = step;
                        }
                        score += len;
                        if (len > bestLen) {
                            bestLen = len;
                            bestYaw = yaw;
                        }
                    }
                    if (look) {
                        bestYaw = Math.toDegrees(Math.atan2(-(base[0] - px), base[2] - pz));
                        score = 1000 - Math.abs(ring - 40) + score * 0.01;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = new double[] {px, py, pz, bestYaw};
                    }
                }
            }
        }
        return best;
    }

    private static double[] structureView(ServerLevel level, String id, double x, double z) {
        var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder = registry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.Identifier.parse(id)));
        if (holder.isEmpty()) {
            return null;
        }
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder.get()),
            net.minecraft.core.BlockPos.containing(x, 0, z), 64, false);
        if (found == null) {
            return null;
        }
        var chunk = level.getChunk(found.getFirst().getX() >> 4, found.getFirst().getZ() >> 4,
            net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, true);
        var start = chunk.getStartForStructure(holder.get().value());
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            return null;
        }
        var b = start.getPieces().get(0).getBoundingBox();
        double cx = (b.minX() + b.maxX()) / 2.0, cz = (b.minZ() + b.maxZ()) / 2.0;
        if (id.contains("ancient_ruins")) {
            // Inside the central room, in a corner, looking across it.
            double px = b.minX() + 2.5, pz = b.minZ() + 2.5, py = b.minY() + 4;
            return new double[] {px, py, pz, Math.toDegrees(Math.atan2(-(cx - px), cz - pz))};
        }
        // A formation: look at it from the most open point on a ring around it.
        double baseY = b.minY() + 3 + Math.min(14, (b.maxY() - b.minY()) / 3.0);
        return viewpoint(level, new double[] {cx, baseY, cz}, true);
    }

    private static double open(DensityFunction f, double x, double y, double z) {
        return f.compute(new DensityFunction.SinglePointContext((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
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
            options.putAll(p);
        } catch (IOException e) {
            FuDevTools.LOGGER.error("[fu-shots] cannot read {}", file, e);
            return List.of();
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        p.stringPropertyNames().stream().filter(k -> k.startsWith("shot.")).forEach(k -> sorted.put(k, p.getProperty(k)));
        return new ArrayList<>(sorted.values());
    }
}
