package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuNoise;
import com.fantasyundergrounds.world.Hash;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Cavern field. Negative = open, positive = rock, clamped to [-1, 1].
 *
 * <p>It replaces vanilla's {@code cave_cheese} noise inside the Overworld router (PLAN.md 2.2).
 * Vanilla still adds its top slide on top, so caverns close up under the real surface.
 * Evaluated only at interpolation cell corners (every 4 × 8 × 4 blocks), because it sits inside
 * vanilla's {@code interpolated} post-processing.
 *
 * <p>Shape per chamber (one chamber per zone slot, see {@link ZoneField}):
 * <ul>
 *   <li>footprint: distance to the warped zone border, with noisy walls; giant neighbours merge</li>
 *   <li>vertical: per-column floor and ceiling heights from 2D fBm, domed towards the walls,
 *       floors terraced into shelves for multiple levels</li>
 *   <li>detail: warped 3D fBm for irregular walls and ceilings</li>
 *   <li>pillars: sparse 2D cellular columns, hourglass-shaped (wider at floor and ceiling)</li>
 *   <li>bridges: ridged-noise paths of rock crossing tall Grand Caverns</li>
 * </ul>
 */
public final class CavernDensity implements DensityFunction.SimpleFunction {
    public static final MapCodec<CavernDensity> CODEC = MapCodec.unit(() -> {
        throw new IllegalStateException("fantasyundergrounds:caverns is created by the mod at runtime, not from data");
    });
    private static final KeyDispatchDataCodec<CavernDensity> KD_CODEC = KeyDispatchDataCodec.of(CODEC);

    private static final double WALL = 0.22;          // footprint threshold in normalised cell units
    private static final double WALL_GIANT = 0.12;    // giant chambers reach closer to the zone border
    private static final double PILLAR_CELL = 40.0;   // pillar grid spacing, blocks

    private final ZoneField zones;
    private final FuNoise wallNoise;
    private final FuNoise floorNoise;
    private final FuNoise ceilNoise;
    private final FuNoise detail;
    private final FuNoise detailWarp;
    private final FuNoise bridgePath;
    private final int minY;
    private final int maxY;

    public CavernDensity(ZoneField zones) {
        this.zones = zones;
        long seed = zones.seed();
        this.wallNoise = new FuNoise(seed, 0xC1L, 4);
        this.floorNoise = new FuNoise(seed, 0xC2L, 3);
        this.ceilNoise = new FuNoise(seed, 0xC3L, 3);
        this.detail = new FuNoise(seed, 0xC4L, 3);
        this.detailWarp = new FuNoise(seed, 0xC5L, 2);
        this.bridgePath = new FuNoise(seed, 0xC6L, 2);
        this.minY = zones.settings().minY();
        this.maxY = zones.settings().maxY();
    }

    @Override
    public double compute(FunctionContext ctx) {
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();
        if (y < this.minY - 8 || y > this.maxY + 8) {
            return 1.0;
        }
        ZoneField.Sample s = this.zones.sample(x, y, z);
        ZoneField.Slot slot = s.slot();
        if (!slot.isCavern()) {
            return 1.0;
        }
        double height = slot.height();

        // Footprint: noisy distance to the zone border. Merged giants ignore the shared wall.
        double wallT = slot.giant() ? WALL_GIANT : WALL;
        // Lobed, rounded footprint: radial distance from the zone centre (perturbed by
        // multi-octave noise) and, separately, a minimum rock wall at the zone border.
        double lobes = this.wallNoise.sample(x / 70.0, y / 40.0, z / 70.0);
        double dist = s.f1();
        if (slot.env() == Environment.INFERNAL_CHASMS) {
            // Rift: an elongated chamber along a per-cell direction, 2.4x longer than wide.
            double a = slot.angle();
            double u = s.dx() * Math.cos(a) + s.dz() * Math.sin(a);
            double v = -s.dx() * Math.sin(a) + s.dz() * Math.cos(a);
            dist = Math.sqrt(u * u / 1.6 + v * v * 3.8);
        }
        double radial = (dist + 0.16 * lobes - slot.radius()) / slot.radius();
        double edge = s.mergedWithNeighbour() ? 1.0 : s.edge() + 0.05 * lobes;
        double border = (wallT - edge) / wallT;
        double wall = Math.max(radial, border);             // > 0 outside the chamber footprint
        // How far inside the footprint we are (0 at the wall, 1 deep inside), for the dome.
        double inside = clamp01(-wall);

        // Vertical profile: dome towards the walls, irregular floor and ceiling.
        double domeSqrt = Math.sqrt(clamp01(inside * (slot.env() == Environment.INFERNAL_CHASMS ? 5.0 : 1.6)));
        double floorN = this.floorNoise.sample2d(x / 90.0, z / 90.0);
        // Terraced floor: smooth steps of ~6 blocks give ledges and multiple floor levels.
        double terr = floorN * height * 0.22;
        terr = Math.floor(terr / 6.0) * 6.0 + smoothstep(clamp01((terr % 6.0 + 6.0) % 6.0 / 6.0)) * 6.0 - 3.0;
        double floorY = slot.floorY() + (1.0 - domeSqrt) * height * 0.30 + terr;
        switch (slot.env()) {
            // Basins: lake floors sink towards the middle; fewer terraces.
            case DROWNED_ABYSSES -> floorY -= terr * 0.6 + inside * height * 0.25;
            // Deep vertical drops: a V-shaped trench along the rift's axis.
            case INFERNAL_CHASMS -> floorY -= inside * inside * height * 0.45;
            default -> { }
        }
        double ceilY = slot.ceilY() - (1.0 - domeSqrt) * height * 0.45
            + 9.0 * this.ceilNoise.sample2d(x / 60.0, z / 60.0);
        double vertical = Math.max(floorY - y, y - ceilY) / 6.0;

        double d = Math.max(wall * 2.5, vertical);

        // Irregular rock surfaces: domain-warped 3D fBm.
        double wx = x + 14.0 * this.detailWarp.sample(x / 40.0, y / 40.0, z / 40.0);
        double wz = z + 14.0 * this.detailWarp.sample(z / 40.0 + 31.7, y / 40.0, x / 40.0);
        d += 0.38 * this.detail.sample(wx / 26.0, y / 18.0, wz / 26.0);

        // Giant pillars (rock re-added inside the chamber).
        if (slot.pillarChance() > 0) {
            d = Math.max(d, pillar(x, y, z, slot));
        }
        // Natural bridges across tall Grand Caverns.
        if (slot.bridge()) {
            d = Math.max(d, bridge(x, y, z, slot));
        }

        // Global vertical envelope (configurable minimum/maximum depth).
        d = Math.max(d, Math.max((this.minY - y) / 4.0, (y - this.maxY) / 4.0));
        return Math.max(-1.0, Math.min(1.0, d));
    }

    private double pillar(int x, int y, int z, ZoneField.Slot slot) {
        int gx = (int) Math.floor(x / PILLAR_CELL);
        int gz = (int) Math.floor(z / PILLAR_CELL);
        double best = Double.MAX_VALUE;
        double bestR = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = Hash.cell(slot.hash(), 0x9111A2L, gx + dx, 0, gz + dz);
                if (Hash.unit(h) >= slot.pillarChance()) {
                    continue;
                }
                long h2 = Hash.fmix(h);
                double px = (gx + dx + 0.2 + 0.6 * Hash.unit(h2)) * PILLAR_CELL;
                double pz = (gz + dz + 0.2 + 0.6 * Hash.unit(Hash.fmix(h2))) * PILLAR_CELL;
                double r = 3.0 + 6.0 * Hash.unit(Hash.fmix(h2 + 7));
                double dist = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz)) - r;
                if (dist < best) {
                    best = dist;
                    bestR = r;
                }
            }
        }
        if (best == Double.MAX_VALUE) {
            return -1.0;
        }
        // Hourglass: radius grows towards floor and ceiling of the chamber.
        double t = (y - slot.centerY()) / (slot.height() * 0.5);
        double flare = bestR * 0.9 * t * t;
        return (flare - best) / 3.0;
    }

    private double bridge(int x, int y, int z, ZoneField.Slot slot) {
        double path = Math.abs(this.bridgePath.sample2d(x / 110.0, z / 110.0));
        double halfWidthBlocks = (0.045 - path) * 110.0;   // ~3-5 blocks either side of the path
        double by = slot.centerY() + slot.bridgeOffset() + 4.0 * this.bridgePath.sample2d(x / 37.0 + 50, z / 37.0);
        double thick = 3.0 - Math.abs(y - by);
        return Math.min(halfWidthBlocks / 2.0, thick / 2.0);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
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

    /** Environment at a block position, for decoration filters. */
    public static Environment environmentAt(ZoneField zones, int x, int y, int z) {
        return zones.sample(x, y, z).slot().env();
    }
}
