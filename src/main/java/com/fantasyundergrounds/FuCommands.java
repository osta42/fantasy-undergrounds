package com.fantasyundergrounds;

import com.fantasyundergrounds.world.CanyonField;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.IslandField;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator-only helpers for modpack makers and testing:
 * <ul>
 *   <li>{@code /fantasyundergrounds zone} - environment and chamber parameters at your position</li>
 *   <li>{@code /fantasyundergrounds locate <environment>} - nearest chamber of that environment, skipping
 *       chambers inside cave biomes that keep their own decoration (lush caves, dripstone caves, deep dark)</li>
 * </ul>
 * Read-only: they never modify the world.
 */
public final class FuCommands {
    private static final int SEARCH_CELLS = 48;

    private FuCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fantasyundergrounds")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("zone").executes(FuCommands::zone))
            .then(Commands.literal("locate")
                .then(Commands.argument("environment", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Environment.values()).filter(e -> e != Environment.ORDINARY).map(Environment::getSerializedName), b))
                    .executes(FuCommands::locate))));
    }

    private static boolean check(CommandSourceStack src) {
        if (!FuWorldgen.appliesTo(src.getLevel().dimension())) {
            src.sendFailure(Component.literal("Fantasy Undergrounds is not active in this dimension."));
            return false;
        }
        return true;
    }

    private static int zone(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!check(src)) {
            return 0;
        }
        Vec3 p = src.getPosition();
        ZoneField.Sample s = FuWorldgen.zones(src.getLevel().getSeed()).sample(p.x, p.y, p.z);
        ZoneField.Slot slot = s.slot();
        src.sendSuccess(() -> Component.literal(String.format(
            "Zone cell (%d, %d): %s%s, chamber centre y=%.0f, height %.0f%s%s",
            s.cellX(), s.cellZ(), slot.env().getSerializedName(), slot.giant() ? " (giant)" : "",
            slot.centerY(), slot.height(), s.mergedWithNeighbour() ? ", merged with neighbour" : "",
            slot.bridge() ? ", has bridges" : "")), false);
        if (slot.islands() > 0) {
            src.sendSuccess(() -> Component.literal(slot.islands() + " floating islands in this chamber"), false);
        }
        CanyonField.Hit h = FuWorldgen.canyons(src.getLevel().getSeed()).nearest(p.x, p.z, 0.0);
        if (h != null) {
            src.sendSuccess(() -> Component.literal(String.format("Grand Canyon: floor y=%.0f, top y=%.0f, river level y=%.0f, %d cliff levels",
                h.floorY(), h.topY(), h.riverLevel(), h.canyon().levels())), false);
        }
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!check(src)) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "environment");
        Environment env = Arrays.stream(Environment.values()).filter(e -> e.getSerializedName().equals(name)).findFirst().orElse(null);
        if (env == null || env == Environment.ORDINARY) {
            src.sendFailure(Component.literal("Unknown environment: " + name));
            return 0;
        }
        ServerLevel level = src.getLevel();
        ZoneField zones = FuWorldgen.zones(level.getSeed());
        Vec3 p = src.getPosition();
        if (env == Environment.GRAND_CANYONS) {
            return locateCanyon(src, FuWorldgen.canyons(level.getSeed()), p);
        }
        DensityFunction terrain = level.getChunkSource().randomState().router().finalDensity();
        int bx = (int) Math.floor(p.x / zones.cellSize());
        int bz = (int) Math.floor(p.z / zones.cellSize());
        for (int r = 0; r <= SEARCH_CELLS; r++) {
            double bestDist = Double.MAX_VALUE;
            double[] best = null;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    ZoneField.Column col = zones.column(bx + dx, bz + dz);
                    ZoneField.Slot slot;
                    if (env == Environment.FLOATING_ISLANDS) {
                        slot = col.deep().islands() > 0 ? col.deep() : null;
                    } else {
                        slot = col.deep().env() == env ? col.deep() : (col.shallow().env() == env ? col.shallow() : null);
                    }
                    if (slot == null) {
                        continue;
                    }
                    double[] c = zones.cellPoint(bx + dx, bz + dz);
                    double[] found;
                    if (env == Environment.FLOATING_ISLANDS) {
                        // Island 0, approximately (its offset is in warped space; the warp is <= ~55 blocks).
                        IslandField.Island is = IslandField.island(slot, zones.cellSize(), 0);
                        found = new double[] {c[0] + is.ox(), is.topY() + 2, c[1] + is.oz()};
                    } else {
                        found = refine(zones, terrain, env, c[0], slot.centerY(), c[1]);
                    }
                    // Skip zones inside cave biomes that keep their own decoration (lush caves,
                    // dripstone caves, deep dark...): there the zone only changes the cave's shape.
                    if (found == null || level.getBiome(BlockPos.containing(found[0], found[1], found[2])).is(InZoneFilter.KEEPS_OWN_DECORATION)) {
                        continue;
                    }
                    double d = Math.hypot(found[0] - p.x, found[2] - p.z);
                    if (d < bestDist) {
                        bestDist = d;
                        best = found;
                    }
                }
            }
            if (best != null) {
                int x = (int) best[0], y = (int) best[1], z = (int) best[2];
                int dist = (int) bestDist;
                src.sendSuccess(() -> Component.literal(String.format(
                    "Nearest %s: %d %d %d (about %d blocks away). /tp @s %d %d %d", name, x, y, z, dist, x, y, z)), false);
                return 1;
            }
        }
        src.sendFailure(Component.literal("No " + name + " within " + (int) (SEARCH_CELLS * zones.cellSize()) + " blocks."));
        return 0;
    }

    private static int locateCanyon(CommandSourceStack src, CanyonField canyons, Vec3 p) {
        int bx = (int) Math.floor(p.x / canyons.cellSize());
        int bz = (int) Math.floor(p.z / canyons.cellSize());
        int rings = (int) Math.ceil(SEARCH_CELLS * 192.0 / canyons.cellSize());
        for (int r = 0; r <= rings; r++) {
            CanyonField.Canyon best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    CanyonField.Canyon c = canyons.canyon(bx + dx, bz + dz);
                    if (c == null) {
                        continue;
                    }
                    double d = Math.hypot(c.cx() - p.x, c.cz() - p.z);
                    if (d < bestDist) {
                        bestDist = d;
                        best = c;
                    }
                }
            }
            if (best != null) {
                CanyonField.Hit h = canyons.nearest(best.cx(), best.cz(), 64.0);
                // Step across to the meandering centre line at the canyon's middle.
                double x = best.cx(), z = best.cz(), y = best.floorY() + 6;
                if (h != null) {
                    x -= h.dv() * -best.az();
                    z -= h.dv() * best.ax();
                    y = h.riverLevel() + 3;
                }
                int ix = (int) x, iy = (int) y, iz = (int) z, dist = (int) bestDist;
                src.sendSuccess(() -> Component.literal(String.format(
                    "Nearest grand_canyons: %d %d %d (about %d blocks away). /tp @s %d %d %d", ix, iy, iz, dist, ix, iy, iz)), false);
                return 1;
            }
        }
        src.sendFailure(Component.literal("No grand_canyons within " + (int) (rings * canyons.cellSize()) + " blocks."));
        return 0;
    }

    /**
     * The cell point is in warped space; search around it for a position that really is in the
     * zone AND open in the level's final terrain density (which includes vanilla's top slide and
     * this mod's surface guard, so chambers sealed under shallow ground are skipped), at the
     * chamber's centre height first, then a little above and below it.
     */
    private static double[] refine(ZoneField zones, DensityFunction terrain, Environment env, double x, double y, double z) {
        for (int dy : new int[] {0, -6, 6, -12, 12}) {
            for (int r = 0; r <= 112; r += 8) {
                for (int dx = -r; dx <= r; dx += 8) {
                    for (int dz = -r; dz <= r; dz += 8) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        double px = x + dx, py = y + dy, pz = z + dz;
                        if (zones.sample(px, py, pz).slot().env() == env
                            && terrain.compute(new DensityFunction.SinglePointContext((int) px, (int) py, (int) pz)) < -0.05) {
                            return new double[] {px, py, pz};
                        }
                    }
                }
            }
        }
        return null;
    }
}
