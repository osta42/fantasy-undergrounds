package com.fantasyundergrounds.world;

import com.fantasyundergrounds.config.FuSettings;

/**
 * Underground Grand Canyons (PLAN.md 2.1, 4): a separate, very sparse 2D field, independent of
 * the zone cells and of biomes.
 *
 * <p>The plane is divided into large cells (1,024 blocks at cavernSizeScale 1.0). Each cell holds
 * at most one canyon, with a per-cell chance scaled by intensity and the grandCanyons frequency.
 * A canyon is a long, meandering gorge (360-760 blocks) along a per-cell direction, placed in the
 * middle half of its cell so it never reaches beyond the neighbouring cells. Its cross-section is
 * a narrow river channel at the floor, two or three cliff levels separated by ledges that step
 * outwards, and an arched roof; both ends taper shut. The floor sits where the aquifer can hold
 * a river 2-4 blocks deep (PLAN.md 2.5: levels -30..-10 are reachable in the y -40..0 fluid band).
 *
 * <p>Everything here is a pure function of (seed, settings, position): the density function,
 * the fluid bias, the decoration filter and the locate command all agree.
 */
public final class CanyonField {
    private static final long SALT_CELL = 0x243F6A8885A308D3L;
    private static final long SALT_PARAMS = 0x13198A2E03707344L;
    /** Share of the height (from the floor) holding the cliff levels; the rest is the roof arch. */
    private static final double LEVELS_TOP = 0.72;

    private final long seed;
    private final double cell;
    private final double scale;
    private final double chance;
    private final int maxY;
    private final FuNoise meander;
    private final FuNoise floorNoise;

    public CanyonField(long seed, FuSettings settings) {
        this.seed = seed;
        this.scale = settings.cavernSize();
        this.cell = 1024.0 * this.scale;
        this.chance = Math.min(1.0, 0.30 * settings.intensity() * settings.frequency(Environment.GRAND_CANYONS));
        this.maxY = settings.maxY();
        this.meander = new FuNoise(seed, 0xCA1L, 3);
        this.floorNoise = new FuNoise(seed, 0xCA2L, 2);
    }

    public double cellSize() {
        return this.cell;
    }

    /** One canyon. Direction (ax, az) is a unit vector; u runs along it, v across it. */
    public record Canyon(
        int cellX, int cellZ,
        double cx, double cz,
        double ax, double az,
        double halfLength,
        double floorY,
        double height,
        double maxHalfWidth,
        double riverHalfWidth,
        int levels,
        long hash
    ) {}

    /**
     * Where a column lies relative to the nearest canyon. {@code dv} is the signed distance from
     * the meandering centre line, {@code taper} is 1 along the body and falls to 0 at the ends.
     */
    public record Hit(Canyon canyon, double u, double dv, double taper, double floorY, double topY) {
        /** Canyon half width at height y (0 outside the canyon's vertical range). */
        public double halfWidth(double y) {
            double span = this.topY - this.floorY;
            if (span <= 1) {
                return 0;
            }
            double t = (y - this.floorY) / span;
            if (t < 0 || t > 1) {
                return 0;
            }
            Canyon c = this.canyon;
            double wTop = c.maxHalfWidth() * this.taper;
            double wRiver = Math.min(c.riverHalfWidth(), wTop);
            if (t <= LEVELS_TOP) {
                // Cliff levels: the width stays almost constant up each cliff and steps outwards
                // over a short ledge at the top of each level.
                double s = t / LEVELS_TOP * c.levels();
                double level = Math.floor(s);
                double frac = s - level;
                double step = (level + Math.pow(smoothstep(frac), 4.0)) / c.levels();
                return wRiver + (wTop - wRiver) * step;
            }
            // Arched roof closing over the top.
            double r = (t - LEVELS_TOP) / (1.0 - LEVELS_TOP);
            return wTop * Math.sqrt(Math.max(0.0, 1.0 - r * r));
        }

        /** Signed horizontal distance to the canyon wall at height y, in blocks (negative = inside). */
        public double wallDistance(double y) {
            return Math.abs(this.dv) - halfWidth(y);
        }

        /**
         * River surface level the aquifer is steered to. The aquifer floors levels to multiples
         * of 3, so the water ends up 2-4 blocks above the floor.
         */
        public double riverLevel() {
            return this.floorY + 4.5;
        }

        /** True if the column is within {@code margin} blocks of the canyon's widest outline here. */
        public boolean near(double margin) {
            return Math.abs(this.dv) - this.canyon.maxHalfWidth() * this.taper <= margin;
        }
    }

    /** The canyon of one large cell, or null. */
    public Canyon canyon(int gx, int gz) {
        if (this.chance <= 0) {
            return null;
        }
        long h = Hash.cell(this.seed, SALT_CELL, gx, 0, gz);
        if (Hash.unit(h) >= this.chance) {
            return null;
        }
        long p = Hash.cell(this.seed, SALT_PARAMS, gx, 0, gz);
        double r1 = Hash.unit(p);
        double r2 = Hash.unit(p = Hash.fmix(p));
        double r3 = Hash.unit(p = Hash.fmix(p));
        double r4 = Hash.unit(p = Hash.fmix(p));
        double r5 = Hash.unit(p = Hash.fmix(p));
        double r6 = Hash.unit(p = Hash.fmix(p));
        double r7 = Hash.unit(p = Hash.fmix(p));
        double r8 = Hash.unit(p = Hash.fmix(p));
        double cx = (gx + 0.25 + 0.5 * r1) * this.cell;
        double cz = (gz + 0.25 + 0.5 * r2) * this.cell;
        double angle = r3 * Math.PI;
        double halfLength = (180.0 + 200.0 * r4) * this.scale;
        // Floor -31..-21 (+/-2 noise): the river at floor+3 stays inside the aquifer's reachable
        // -30..-10 band for the y -40..0 fluid cells.
        double floorY = -31.0 + 10.0 * r5;
        double height = 48.0 + 22.0 * r6;
        double maxHalfWidth = (14.0 + 12.0 * r7) * this.scale;
        double river = 3.0 + 2.0 * r8;
        int levels = (p & 0x100) == 0 ? 2 : 3;
        return new Canyon(gx, gz, cx, cz, Math.cos(angle), Math.sin(angle), halfLength, floorY, height, maxHalfWidth, river, levels, p);
    }

    /**
     * Nearest canyon to a column, if the column is within {@code margin} blocks of the canyon's
     * widest outline; otherwise null.
     */
    public Hit nearest(double x, double z, double margin) {
        if (this.chance <= 0) {
            return null;
        }
        int bx = (int) Math.floor(x / this.cell);
        int bz = (int) Math.floor(z / this.cell);
        Hit best = null;
        double bestOut = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Canyon c = canyon(bx + dx, bz + dz);
                if (c == null) {
                    continue;
                }
                double rx = x - c.cx();
                double rz = z - c.cz();
                double u = rx * c.ax() + rz * c.az();
                if (Math.abs(u) > c.halfLength() + margin) {
                    continue;
                }
                double v = -rx * c.az() + rz * c.ax();
                double mU = u / (170.0 * this.scale) + (c.hash() & 0xFFFF) * 0.01;
                double centre = 38.0 * this.scale * this.meander.sample2d(mU, 3.1);
                double dv = v - centre;
                double out = Math.abs(dv) - c.maxHalfWidth();
                if (out > margin || out >= bestOut) {
                    continue;
                }
                double taper = clamp01((c.halfLength() - Math.abs(u)) / (0.3 * c.halfLength()));
                taper = smoothstep(taper);
                double floorY = c.floorY() + 2.0 * this.floorNoise.sample2d(u / 90.0, (c.hash() >>> 20 & 0xFFF) * 0.01);
                // Shallower towards the ends; never above maximumY - 4.
                double topY = Math.min(floorY + c.height() * (0.45 + 0.55 * taper), this.maxY - 4.0);
                bestOut = out;
                best = new Hit(c, u, dv, taper, floorY, topY);
            }
        }
        return best;
    }

    /** True if (x, y, z) is in the open part of a canyon, allowing {@code slack} blocks. */
    public boolean inside(double x, double y, double z, double slack) {
        Hit h = nearest(x, z, slack);
        return h != null && y >= h.floorY() - slack && y <= h.topY() + slack && h.wallDistance(y) < slack;
    }

    static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }
}
