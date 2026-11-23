package com.fantasyundergrounds;

import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator-only helpers for modpack makers and testing:
 * <ul>
 *   <li>{@code /fantasyundergrounds zone} - environment and chamber parameters at your position</li>
 *   <li>{@code /fantasyundergrounds locate <environment>} - nearest chamber of that environment</li>
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
                    ZoneField.Slot slot = col.deep().env() == env ? col.deep() : (col.shallow().env() == env ? col.shallow() : null);
                    if (slot == null) {
                        continue;
                    }
                    double[] c = zones.cellPoint(bx + dx, bz + dz);
                    double[] found = refine(zones, env, c[0], slot.centerY(), c[1]);
                    if (found == null) {
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

    /** The cell point is in warped space; search around it for a position that really is in the zone. */
    private static double[] refine(ZoneField zones, Environment env, double x, double y, double z) {
        for (int r = 0; r <= 96; r += 8) {
            for (int dx = -r; dx <= r; dx += 8) {
                for (int dz = -r; dz <= r; dz += 8) {
                    if (zones.sample(x + dx, y, z + dz).slot().env() == env) {
                        return new double[] {x + dx, y, z + dz};
                    }
                }
            }
        }
        return null;
    }
}
