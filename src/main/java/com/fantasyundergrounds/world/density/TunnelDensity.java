package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.FuNoise;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Ordinary cave networks: worm tunnels where two independent, domain-warped 3D noise fields are
 * both near zero (the intersection of two isosurfaces is a winding curve). Two scales: main
 * tunnels and thinner "noodle" passages. A low-frequency mask leaves large regions solid.
 * Width grows with depth. Negative = open, clamped to [-1, 1].
 *
 * <p>It replaces vanilla {@code overworld/caves/spaghetti_2d} (and the roughness term) inside the
 * router, so it sits in the same min() as vanilla spaghetti, inside vanilla interpolation.
 */
public final class TunnelDensity implements DensityFunction.SimpleFunction {
    public static final MapCodec<TunnelDensity> CODEC = MapCodec.unit(() -> {
        throw new IllegalStateException("fantasyundergrounds:tunnels is created by the mod at runtime, not from data");
    });
    private static final KeyDispatchDataCodec<TunnelDensity> KD_CODEC = KeyDispatchDataCodec.of(CODEC);

    private final FuNoise a1, b1, a2, b2;
    private final FuNoise warp;
    private final FuNoise mask;
    private final FuNoise mainMask;
    private final FuNoise widthNoise;
    private final double frequency;
    private final double width;
    private final int minY;
    private final int maxY;

    public TunnelDensity(long seed, FuSettings s) {
        this.a1 = new FuNoise(seed, 0x71L, 2);
        this.b1 = new FuNoise(seed, 0x72L, 2);
        this.a2 = new FuNoise(seed, 0x73L, 1);
        this.b2 = new FuNoise(seed, 0x74L, 1);
        this.warp = new FuNoise(seed, 0x75L, 2);
        this.mask = new FuNoise(seed, 0x76L, 2);
        this.mainMask = new FuNoise(seed, 0x78L, 2);
        this.widthNoise = new FuNoise(seed, 0x77L, 1);
        this.frequency = s.tunnelFrequency();
        this.width = s.tunnelWidth();
        this.minY = s.minY();
        this.maxY = s.maxY();
    }

    @Override
    public double compute(FunctionContext ctx) {
        if (this.frequency <= 0) {
            return 1.0;
        }
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();
        if (y < this.minY - 6 || y > this.maxY + 6) {
            return 1.0;
        }
        // Regional masks. Main tunnels cover most of the world ("very common"); the thin maze
        // system clusters in patches, leaving large stretches of solid rock between them.
        double fb = 0.25 * (this.frequency - 1.0);
        double mainCoverage = clamp01((this.mainMask.sample2d(x / 400.0, z / 400.0) + 0.55 + fb) * 3.0);
        double mazeCoverage = clamp01((this.mask.sample2d(x / 280.0, z / 280.0) + 0.05 + fb) * 3.0);
        if (mainCoverage <= 0 && mazeCoverage <= 0) {
            return 1.0;
        }
        double depth = clamp01((40.0 - y) / 100.0);                 // 0 at y=40, ~1 at y=-60
        double w = this.width * (0.85 + 0.3 * this.widthNoise.sample(x / 120.0, y / 60.0, z / 120.0)) * (1.0 + 0.35 * depth);

        double wx = x + 9.0 * this.warp.sample(x / 50.0, y / 50.0, z / 50.0);
        double wy = y + 5.0 * this.warp.sample(z / 50.0 + 11.3, y / 50.0, x / 50.0);
        double wz = z + 9.0 * this.warp.sample(y / 50.0 + 23.9, z / 50.0, x / 50.0);

        double f1 = 1.0 / 78.0;
        double n1 = this.a1.sample(wx * f1, wy * f1 * 1.0, wz * f1);
        double n2 = this.b1.sample(wx * f1, wy * f1 * 1.0, wz * f1);
        double t1 = (Math.sqrt(n1 * n1 + n2 * n2) - 0.130 * w) * 9.0;

        double f2 = 1.0 / 42.0;
        double m1 = this.a2.sample(wx * f2, wy * f2 * 0.9, wz * f2);
        double m2 = this.b2.sample(wx * f2, wy * f2 * 0.9, wz * f2);
        double t2 = (Math.sqrt(m1 * m1 + m2 * m2) - 0.115 * w) * 9.0;

        t1 = Math.max(t1, (1.0 - mainCoverage) * 2.0);
        t2 = Math.max(t2, (1.0 - mazeCoverage) * 2.0);
        double t = Math.min(t1, t2);
        t = Math.max(t, Math.max((this.minY + 2 - y) / 3.0, (y - this.maxY) / 3.0));
        return Math.max(-1.0, Math.min(1.0, t));
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
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
