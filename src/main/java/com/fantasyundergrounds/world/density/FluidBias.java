package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.world.CanyonField;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Wraps one of the router's aquifer inputs so Drowned Abysses and Infernal Chasms get their
 * large water and lava bodies from the vanilla aquifer instead of placed fluid blocks
 * (PLAN.md 2.5). Outside those zones the original input is returned unchanged.
 *
 * <p>The aquifer calls these inputs with different coordinate spaces (verified in 26.2
 * {@code Aquifer.NoiseBasedAquifer}):
 * <ul>
 *   <li>{@code fluid_level_floodedness}: block coordinates of the aquifer grid point</li>
 *   <li>{@code fluid_level_spread}: fluid-level cell coordinates (16 x 40 x 16 blocks);
 *       the level becomes {@code cellY*40+20 + quantize(spread*10, 3)}</li>
 *   <li>{@code lava}: fluid-type cell coordinates (64 x 40 x 64 blocks); |value| > 0.3 = lava,
 *       applied only where the fluid level is at or below y=-10</li>
 * </ul>
 * Floodedness above 0.8 floods fully; 0.4-0.8 gives a lake at the spread-controlled level.
 *
 * <p>Grand Canyons get a river: inside the fluid band that can hold the river level the canyon
 * is a lake steered to {@link CanyonField.Hit#riverLevel()}; everywhere else in the canyon it is
 * forced dry, so a canyon never floods up to some other band's level. Rivers are always water.
 * Drowned Abysses and Infernal Chasms take precedence where they overlap a canyon.
 *
 * <p>Lake mode is not a fixed value: within 64 blocks below a sea floor the aquifer lowers both
 * flooding thresholds, and a fixed 0.6 would read as "fully flooded to sea level" there (a slab
 * of sea water floating in the chamber). {@link #lakeValue} recomputes the aquifer's depth
 * factor f from the same surface samples and returns 0.45 - 1.2 f, which lies between the
 * partial (0.4 - 1.2 f) and full (0.8 - 1.1 f) thresholds for every f.
 */
public final class FluidBias implements DensityFunction {
    public static final MapCodec<FluidBias> CODEC = MapCodec.unit(() -> {
        throw new IllegalStateException("fantasyundergrounds:fluid_bias is created by the mod at runtime, not from data");
    });
    private static final KeyDispatchDataCodec<FluidBias> KD_CODEC = KeyDispatchDataCodec.of(CODEC);

    public enum Mode { FLOODEDNESS, SPREAD, LAVA }

    private final DensityFunction original;
    private final Mode mode;
    private final ZoneField zones;
    private final CanyonField canyons;
    private final DensityFunction surface;
    private final int seaLevel;

    /** Chunk offsets the aquifer samples the preliminary surface at (Aquifer.SURFACE_SAMPLING_OFFSETS_IN_CHUNKS). */
    private static final int[][] SURFACE_OFFSETS = {
        {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
    };

    public FluidBias(DensityFunction original, Mode mode, ZoneField zones, CanyonField canyons, DensityFunction surface, int seaLevel) {
        this.original = original;
        this.mode = mode;
        this.zones = zones;
        this.canyons = canyons;
        this.surface = surface;
        this.seaLevel = seaLevel;
    }

    private int preliminarySurface(int x, int z) {
        // Same quantisation as NoiseChunk.preliminarySurfaceLevel.
        return (int) Math.floor(this.surface.compute(new SinglePointContext((x >> 2) << 2, 0, (z >> 2) << 2)));
    }

    /** Floodedness that the aquifer reads as a lake (never dry, never fully flooded) at this grid point. */
    private double lakeValue(int x, int y, int z) {
        if (preliminarySurface(x, z) + 8 >= this.seaLevel) {
            return 0.45;
        }
        int lowest = Integer.MAX_VALUE;
        for (int[] o : SURFACE_OFFSETS) {
            lowest = Math.min(lowest, preliminarySurface(x + o[0] * 16, z + o[1] * 16));
        }
        double f = Math.max(0.0, Math.min(1.0, 1.0 - (lowest + 8 - y) / 64.0));
        return 0.45 - 1.2 * f;
    }

    @Override
    public double compute(FunctionContext ctx) {
        return switch (this.mode) {
            case FLOODEDNESS -> floodedness(ctx);
            case SPREAD -> spread(ctx);
            case LAVA -> lava(ctx);
        };
    }

    private double floodedness(FunctionContext ctx) {
        // Inside a canyon's own outline the canyon decides (river band lake, dry above), even where
        // it crosses a zone, so the gorge keeps its river and stays open.
        CanyonField.Hit ch = this.canyons.nearest(ctx.blockX(), ctx.blockZ(), 0.0);
        if (ch != null && ch.near(0.0) && ctx.blockY() >= ch.floorY() - 6 && ctx.blockY() <= ch.topY() + 4) {
            return canyonFloodedness(ctx);
        }
        ZoneField.Slot s = this.zones.sample(ctx.blockX(), ctx.blockY(), ctx.blockZ()).slot();
        return switch (s.env()) {
            case DROWNED_ABYSSES -> s.submerged() ? 1.0 : lakeValue(ctx.blockX(), ctx.blockY(), ctx.blockZ());
            case INFERNAL_CHASMS -> lakeValue(ctx.blockX(), ctx.blockY(), ctx.blockZ());
            // Forests stay dry apart from shallow ponds in the hollows of the floor.
            case UNDERGROUND_FORESTS -> {
                int mid = Math.floorDiv(ctx.blockY(), 40) * 40 + 20;
                yield Math.abs(pondLevel(s) - mid) <= 10.0 ? lakeValue(ctx.blockX(), ctx.blockY(), ctx.blockZ()) : -1.0;
            }
            // Crystal Hollows are always dry.
            case CRYSTAL_HOLLOWS -> -1.0;
            default -> canyonFloodedness(ctx);
        };
    }

    private double canyonFloodedness(FunctionContext ctx) {
        int y = ctx.blockY();
        CanyonField.Hit h = this.canyons.nearest(ctx.blockX(), ctx.blockZ(), 16.0);
        if (h == null || !h.near(16.0) || y < h.floorY() - 12 || y > h.topY() + 8) {
            return this.original.compute(ctx);
        }
        int mid = Math.floorDiv(y, 40) * 40 + 20;
        return Math.abs(h.riverLevel() - mid) <= 10.0 ? lakeValue(ctx.blockX(), y, ctx.blockZ()) : -1.0;
    }

    private double spread(FunctionContext ctx) {
        int bx = ctx.blockX() * 16 + 8;
        int bz = ctx.blockZ() * 16 + 8;
        int mid = ctx.blockY() * 40 + 20;
        ZoneField.Slot s = lakeSlot(bx, mid, bz);
        if (s == null) {
            CanyonField.Hit h = this.canyons.nearest(bx, bz, 16.0);
            if (h != null && h.near(16.0) && Math.abs(h.riverLevel() - mid) <= 10.0) {
                return Math.max(-1.0, Math.min(1.0, (h.riverLevel() - mid) / 10.0));
            }
            return this.original.compute(ctx);
        }
        double target = switch (s.env()) {
            case DROWNED_ABYSSES -> s.floorY() + 0.45 * s.height();
            case UNDERGROUND_FORESTS -> pondLevel(s);
            default -> s.floorY() + 0.16 * s.height();
        };
        return Math.max(-1.0, Math.min(1.0, (target - mid) / 10.0));
    }

    /** Forest ponds: a little above the chamber's nominal floor, so only the floor's hollows fill. */
    private static double pondLevel(ZoneField.Slot s) {
        return s.floorY() + 3.0;
    }

    private ZoneField.Slot lakeSlot(int x, int mid, int z) {
        for (int dy : new int[] {0, -15, 15}) {
            ZoneField.Slot s = this.zones.sample(x, mid + dy, z).slot();
            if ((s.env() == Environment.DROWNED_ABYSSES && !s.submerged()) || s.env() == Environment.INFERNAL_CHASMS
                || s.env() == Environment.UNDERGROUND_FORESTS) {
                return s;
            }
        }
        return null;
    }

    private double lava(FunctionContext ctx) {
        int bx = ctx.blockX() * 64 + 32;
        int bz = ctx.blockZ() * 64 + 32;
        int mid = ctx.blockY() * 40 + 20;
        boolean drowned = false;
        for (int dy : new int[] {0, -15, 15}) {
            Environment env = this.zones.sample(bx, mid + dy, bz).slot().env();
            if (env == Environment.INFERNAL_CHASMS) {
                return 1.0;
            }
            // Drowned lakes and forest ponds are always water, never aquifer lava.
            drowned |= env == Environment.DROWNED_ABYSSES || env == Environment.UNDERGROUND_FORESTS;
        }
        // The fluid-type cell is 64 blocks wide: a forest or abyss away from its centre must
        // still get water, so also check four points around the centre.
        for (int[] o : new int[][] {{-20, -20}, {20, -20}, {-20, 20}, {20, 20}}) {
            if (drowned) {
                break;
            }
            for (int dy : new int[] {0, -15, 15}) {
                Environment env = this.zones.sample(bx + o[0], mid + dy, bz + o[1]).slot().env();
                drowned |= env == Environment.DROWNED_ABYSSES || env == Environment.UNDERGROUND_FORESTS;
            }
        }
        if (drowned) {
            return 0.0;
        }
        // Only the fluid band that holds the river: lava lakes above or below a canyon stay vanilla.
        CanyonField.Hit h = this.canyons.nearest(bx, bz, 48.0);
        return h != null && h.near(48.0) && Math.abs(h.riverLevel() - mid) <= 10.0 ? 0.0 : this.original.compute(ctx);
    }

    @Override
    public void fillArray(double[] output, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(output, this);
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        return new FluidBias(visitor.apply(this.original), this.mode, this.zones, this.canyons, visitor.apply(this.surface), this.seaLevel);
    }

    @Override
    public double minValue() {
        return Math.min(this.original.minValue(), -1.0);
    }

    @Override
    public double maxValue() {
        return Math.max(this.original.maxValue(), 1.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KD_CODEC;
    }
}
