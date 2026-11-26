package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuNoise;
import com.fantasyundergrounds.world.Hash;
import com.fantasyundergrounds.world.IslandField;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Cavern field. Negative = open, positive = rock, clamped to [-1, 1].
 *
 * <p>It replaces vanilla's {@code cave_cheese} noise inside the Overworld router (PLAN.md 2.2).
 * Vanilla still adds its top slide on top, so caverns close up under the real surface.
 * Evaluated only at interpolation cell corners (every 4 x 8 x 4 blocks).
 *
 * <p>"Monumental" chamber shape (one chamber per zone slot, see {@link ZoneField}):
 * <ul>
 *   <li>footprint: wide and smooth, with a rock wall kept at the zone border; giant neighbours merge</li>
 *   <li>ceiling: a flat, high vault that only curves down near the walls</li>
 *   <li>floor: concentric cliff terraces (near-vertical risers, flat treads) descending to a central
 *       basin, where the lake or the chasm into the chamber below is</li>
 *   <li>walls: low-amplitude, large-scale relief, so they read as eroded cliffs</li>
 *   <li>rare, thick hourglass pillars and rock spikes, kept out of the open middle</li>
 *   <li>sweeping arches that rise towards the middle and flare into the walls</li>
 *   <li>floating islands in tall Grand Caverns ({@link IslandField}); vertical shafts between tiers</li>
 * </ul>
 */
public final class CavernDensity implements DensityFunction.SimpleFunction {
    public static final MapCodec<CavernDensity> CODEC = MapCodec.unit(() -> {
        throw new IllegalStateException("fantasyundergrounds:caverns is created by the mod at runtime, not from data");
    });
    private static final KeyDispatchDataCodec<CavernDensity> KD_CODEC = KeyDispatchDataCodec.of(CODEC);

    private static final double WALL = 0.20;          // footprint threshold in normalised cell units
    private static final double WALL_GIANT = 0.10;    // giant chambers reach closer to the zone border
    private static final double PILLAR_CELL = 60.0;   // pillar grid spacing, blocks
    private static final double SPIKE_CELL = 36.0;    // stalactite / stalagmite grid spacing, blocks

    private final ZoneField zones;
    private final FuNoise wallNoise;
    private final FuNoise floorNoise;
    private final FuNoise ceilNoise;
    private final FuNoise detail;
    private final FuNoise detailWarp;
    private final FuNoise bridgePath;
    private final FuNoise shaftNoise;
    private final IslandField islands;
    private final int minY;
    private final int maxY;

    public CavernDensity(ZoneField zones) {
        this.zones = zones;
        long seed = zones.seed();
        this.wallNoise = new FuNoise(seed, 0xC1L, 3);
        this.floorNoise = new FuNoise(seed, 0xC2L, 2);
        this.ceilNoise = new FuNoise(seed, 0xC3L, 2);
        this.detail = new FuNoise(seed, 0xC4L, 2);
        this.detailWarp = new FuNoise(seed, 0xC5L, 2);
        this.bridgePath = new FuNoise(seed, 0xC6L, 2);
        this.shaftNoise = new FuNoise(seed, 0xC7L, 2);
        this.islands = new IslandField(seed);
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
        double d = s.slot().isCavern() ? chamber(s, x, y, z) : 1.0;
        // Vertical shafts joining stacked chambers (and the chasm in a mid chamber's floor).
        if (!s.column().shafts().isEmpty()) {
            d = Math.min(d, shafts(s, x, y, z));
        }
        d = Math.max(d, Math.max((this.minY - y) / 4.0, (y - this.maxY) / 4.0));
        return Math.max(-1.0, Math.min(1.0, d));
    }

    private double chamber(ZoneField.Sample s, int x, int y, int z) {
        ZoneField.Slot slot = s.slot();
        double height = slot.height();
        double cell = this.zones.cellSize();

        // Footprint: smooth lobed radial distance, plus a minimum rock wall at the zone border.
        double wallT = slot.giant() ? WALL_GIANT : WALL;
        double lobes = this.wallNoise.sample(x / 110.0, y / 60.0, z / 110.0);
        double dist = s.f1();
        if (slot.env() == Environment.INFERNAL_CHASMS) {
            // Rift: an elongated chamber along a per-cell direction.
            double a = slot.angle();
            double u = s.dx() * Math.cos(a) + s.dz() * Math.sin(a);
            double v = -s.dx() * Math.sin(a) + s.dz() * Math.cos(a);
            dist = Math.sqrt(u * u / 1.6 + v * v * 3.2);
        }
        double radial = (dist + 0.12 * lobes - slot.radius()) / slot.radius();
        double edge = s.mergedWithNeighbour() ? 1.0 : s.edge() + 0.04 * lobes;
        double border = (wallT - edge) / wallT;
        double wall = Math.max(radial, border);
        double inside = clamp01(-wall);                     // 0 at the wall, towards 1 in the middle

        // Ceiling: a flat high vault, curving down only near the walls.
        double vault = Math.pow(1.0 - clamp01(inside * 2.4), 2.0);
        double ceilY = slot.ceilY() - vault * height * 0.42 + 5.0 * this.ceilNoise.sample2d(x / 80.0, z / 80.0);

        // Floor: concentric cliff terraces descending to the central basin.
        double rise = 1.0 - clamp01(inside * 1.45);
        double raw = height * 0.40 * Math.pow(rise, 1.15) + 3.5 * this.floorNoise.sample2d(x / 90.0, z / 90.0);
        double step = slot.env() == Environment.UNDERGROUND_FORESTS ? 6.0 : 9.0;
        double q = raw / step;
        double level = Math.floor(q);
        double frac = q - level;
        double terr = (level + smoothstep(clamp01((frac - 0.82) / 0.18))) * step;   // flat tread, steep riser
        double floorY = slot.floorY() + terr;
        switch (slot.env()) {
            case DROWNED_ABYSSES -> floorY -= inside * height * 0.18;               // deep basin
            case INFERNAL_CHASMS -> floorY -= inside * inside * height * 0.30;      // V-shaped drop to the lava
            default -> { }
        }
        double vertical = Math.max(floorY - y, y - ceilY) / 6.0;
        double d = Math.max(wall * 2.5, vertical);

        // Walls as massive eroded cliffs: low-amplitude, large-scale relief.
        double wx = x + 18.0 * this.detailWarp.sample(x / 70.0, y / 50.0, z / 70.0);
        double wz = z + 18.0 * this.detailWarp.sample(z / 70.0 + 31.7, y / 50.0, x / 70.0);
        double amp = slot.env() == Environment.CRYSTAL_HOLLOWS ? 0.24 : 0.15;
        d += amp * this.detail.sample(wx / 48.0, y / 30.0, wz / 48.0);

        double[] cp = this.zones.cellPoint(s.cellX(), s.cellZ());
        double footprint = slot.radius() * cell;
        if (slot.spikeChance() > 0) {
            d = Math.max(d, spikes(x, y, z, slot, floorY, ceilY, cp, footprint));
        }
        if (slot.pillarChance() > 0) {
            d = Math.max(d, pillar(x, y, z, slot, cp, footprint));
        }
        if (slot.bridge()) {
            d = Math.max(d, bridge(x, y, z, slot, inside));
        }
        if (slot.islands() > 0) {
            d = Math.max(d, this.islands.density(slot, cell, s.dx() * cell, s.dz() * cell, x, y, z));
        }
        return d;
    }

    /** Vertical shafts of the column: open (negative) inside a wobbling cylinder between two chambers. */
    private double shafts(ZoneField.Sample s, int x, int y, int z) {
        double cell = this.zones.cellSize();
        double best = 1.0;
        for (ZoneField.Shaft sh : s.column().shafts()) {
            if (y < sh.bottomY() - 4 || y > sh.topY() + 4) {
                continue;
            }
            double dist = Math.hypot(s.dx() - sh.ox(), s.dz() - sh.oz()) * cell;
            double r = sh.radius() * (1.0 + 0.25 * this.shaftNoise.sample(x / 20.0, y / 16.0, z / 20.0));
            double v = Math.max((dist - r) / 3.0, Math.max((sh.bottomY() - y) / 3.0, (y - sh.topY()) / 3.0));
            best = Math.min(best, v);
        }
        return best;
    }

    /** Rare, thick hourglass pillars, only in the outer part of the chamber (the middle stays open). */
    private double pillar(int x, int y, int z, ZoneField.Slot slot, double[] cp, double footprint) {
        int gx = (int) Math.floor(x / PILLAR_CELL);
        int gz = (int) Math.floor(z / PILLAR_CELL);
        double best = -1.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = Hash.cell(slot.hash(), 0x9111A2L, gx + dx, 0, gz + dz);
                if (Hash.unit(h) >= slot.pillarChance()) {
                    continue;
                }
                long h2 = Hash.fmix(h);
                double px = (gx + dx + 0.2 + 0.6 * Hash.unit(h2)) * PILLAR_CELL;
                double pz = (gz + dz + 0.2 + 0.6 * Hash.unit(Hash.fmix(h2))) * PILLAR_CELL;
                if (Math.hypot(px - cp[0], pz - cp[1]) < 0.5 * footprint) {
                    continue;
                }
                double r = 8.0 + 9.0 * Hash.unit(Hash.fmix(h2 + 7));
                double dist = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
                if (dist > r * 2.2) {
                    continue;
                }
                // Hourglass: flares to ~1.9x towards floor and ceiling.
                double t = (y - slot.centerY()) / (slot.height() * 0.5);
                double rr = r * (0.8 + 1.1 * t * t);
                best = Math.max(best, (rr - dist) / 3.0);
            }
        }
        return best;
    }

    /** Broad arches across the chamber: rising towards the middle, flaring into the walls. */
    private double bridge(int x, int y, int z, ZoneField.Slot slot, double inside) {
        double path = Math.abs(this.bridgePath.sample2d(x / 150.0, z / 150.0));
        double halfWidth = (0.06 - path) * 150.0;          // up to ~9 blocks either side
        if (halfWidth < -6) {
            return -1.0;
        }
        double by = slot.centerY() + slot.bridgeOffset() + slot.height() * 0.12 * inside
            + 4.0 * this.bridgePath.sample2d(x / 55.0 + 50, z / 55.0);
        double top = by + 3.5;
        double bottom = by - 3.0 - 12.0 * (1.0 - inside) * (1.0 - inside);
        double thick = Math.min(top - y, y - bottom);
        return Math.min(halfWidth / 2.0, thick / 2.5);
    }

    /** Rare, big rock stalactites and stalagmites framing the edges of the chamber. */
    private double spikes(int x, int y, int z, ZoneField.Slot slot, double floorY, double ceilY, double[] cp, double footprint) {
        int gx = (int) Math.floor(x / SPIKE_CELL);
        int gz = (int) Math.floor(z / SPIKE_CELL);
        double best = -1.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = Hash.cell(slot.hash(), 0x5719EL, gx + dx, 0, gz + dz);
                if (Hash.unit(h) >= slot.spikeChance()) {
                    continue;
                }
                long h2 = Hash.fmix(h);
                double px = (gx + dx + 0.15 + 0.7 * Hash.unit(h2)) * SPIKE_CELL;
                double pz = (gz + dz + 0.15 + 0.7 * Hash.unit(h2 = Hash.fmix(h2))) * SPIKE_CELL;
                if (Math.hypot(px - cp[0], pz - cp[1]) < 0.4 * footprint) {
                    continue;
                }
                double base = 5.0 + 7.0 * Hash.unit(h2 = Hash.fmix(h2));
                double length = Math.min(slot.height() * 0.45, 14.0 + 34.0 * Hash.unit(h2 = Hash.fmix(h2)));
                boolean hanging = Hash.unit(Hash.fmix(h2)) < 0.65;
                double dist = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
                if (dist > base + 1.0) {
                    continue;
                }
                double t = hanging ? (y - (ceilY - length)) / length : ((floorY + length) - y) / length;
                if (t <= 0) {
                    continue;
                }
                double r = base * Math.pow(Math.min(t, 1.0), 0.75);
                best = Math.max(best, (r - dist) / 2.0);
            }
        }
        return best;
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
