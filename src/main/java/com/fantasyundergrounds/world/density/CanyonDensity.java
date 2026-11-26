package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.world.CanyonField;
import com.fantasyundergrounds.world.FuNoise;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Underground Grand Canyons as a density field (negative = open, clamped to [-1, 1]). The shape
 * comes from {@link CanyonField}; this adds rough walls. It is combined with the cavern field
 * with {@code min} in the slot that replaces vanilla {@code cave_cheese} (PLAN.md 2.2), so it
 * inherits vanilla interpolation, the top slide, the surface guard and the structure beardifier.
 */
public final class CanyonDensity implements DensityFunction.SimpleFunction {
    public static final MapCodec<CanyonDensity> CODEC = MapCodec.unit(() -> {
        throw new IllegalStateException("fantasyundergrounds:canyons is created by the mod at runtime, not from data");
    });
    private static final KeyDispatchDataCodec<CanyonDensity> KD_CODEC = KeyDispatchDataCodec.of(CODEC);

    private final CanyonField field;
    private final FuNoise wallNoise;
    private final FuNoise detail;
    private final int minY;
    private final int maxY;

    public CanyonDensity(CanyonField field, long seed, int minY, int maxY) {
        this.field = field;
        this.wallNoise = new FuNoise(seed, 0xCA3L, 3);
        this.detail = new FuNoise(seed, 0xCA4L, 2);
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public double compute(FunctionContext ctx) {
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();
        if (y < this.minY - 8 || y > this.maxY + 8) {
            return 1.0;
        }
        CanyonField.Hit h = this.field.nearest(x, z, 12.0);
        if (h == null || y < h.floorY() - 12 || y > h.topY() + 12) {
            return 1.0;
        }
        // Rough walls: widen/narrow by up to ~20%, plus small-scale relief.
        double hw = h.halfWidth(y) * (1.0 + 0.2 * this.wallNoise.sample(x / 34.0, y / 22.0, z / 34.0));
        double d = (Math.abs(h.dv()) - hw) / 5.0;
        double vertical = Math.max(h.floorY() - y, y - h.topY()) / 6.0;
        d = Math.max(d, vertical);
        d += 0.22 * this.detail.sample(x / 15.0, y / 11.0, z / 15.0);
        // Natural stone bridges spanning the canyon.
        d = Math.max(d, this.field.bridge(h, y));
        d = Math.max(d, Math.max((this.minY - y) / 4.0, (y - this.maxY) / 4.0));
        return Math.max(-1.0, Math.min(1.0, d));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KD_CODEC;
    }
}
