package com.fantasyundergrounds.world.density;

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

    public FluidBias(DensityFunction original, Mode mode, ZoneField zones) {
        this.original = original;
        this.mode = mode;
        this.zones = zones;
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
        ZoneField.Slot s = this.zones.sample(ctx.blockX(), ctx.blockY(), ctx.blockZ()).slot();
        return switch (s.env()) {
            case DROWNED_ABYSSES -> s.submerged() ? 1.0 : 0.6;
            case INFERNAL_CHASMS -> 0.6;
            default -> this.original.compute(ctx);
        };
    }

    private double spread(FunctionContext ctx) {
        int bx = ctx.blockX() * 16 + 8;
        int bz = ctx.blockZ() * 16 + 8;
        int mid = ctx.blockY() * 40 + 20;
        ZoneField.Slot s = lakeSlot(bx, mid, bz);
        if (s == null) {
            return this.original.compute(ctx);
        }
        double target = s.env() == Environment.DROWNED_ABYSSES
            ? s.floorY() + 0.45 * s.height()
            : s.floorY() + 0.22 * s.height();
        return Math.max(-1.0, Math.min(1.0, (target - mid) / 10.0));
    }

    private ZoneField.Slot lakeSlot(int x, int mid, int z) {
        for (int dy : new int[] {0, -15, 15}) {
            ZoneField.Slot s = this.zones.sample(x, mid + dy, z).slot();
            if ((s.env() == Environment.DROWNED_ABYSSES && !s.submerged()) || s.env() == Environment.INFERNAL_CHASMS) {
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
            drowned |= env == Environment.DROWNED_ABYSSES;
        }
        return drowned ? 0.0 : this.original.compute(ctx);
    }

    @Override
    public void fillArray(double[] output, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(output, this);
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        return new FluidBias(visitor.apply(this.original), this.mode, this.zones);
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
