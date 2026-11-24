package com.fantasyundergrounds.world;

/**
 * Floating Stone Islands (PLAN.md 2.1, 4): only inside tall (>= 60 blocks) deep Grand Cavern
 * chambers that rolled islands ({@link ZoneField.Slot#islands()}). Each island is a density blob
 * with a gently domed, flattened top and a noise-perturbed cone tapering to a point underneath.
 *
 * <p>Islands are positioned in the zone's warped space, relative to the cell point, like the
 * chamber footprint itself, so they always sit inside their chamber. Pure functions of (seed,
 * cell, position): the cavern density and the decoration filter agree exactly.
 */
public final class IslandField {
    private static final long SALT_ISLAND = 0x452821E638D01377L;

    private final FuNoise edge;
    private final FuNoise top;
    private final FuNoise under;

    public IslandField(long seed) {
        this.edge = new FuNoise(seed, 0x15A1L, 2);
        this.top = new FuNoise(seed, 0x15A2L, 2);
        this.under = new FuNoise(seed, 0x15A3L, 2);
    }

    /** One island, in blocks: offset from the cell point (warped space), radius, top and cone depth. */
    public record Island(double ox, double oz, double radius, double topY, double depth) {}

    public static Island island(ZoneField.Slot slot, double cell, int i) {
        long h = Hash.cell(slot.hash(), SALT_ISLAND, i, 0, 0);
        double r1 = Hash.unit(h);
        double r2 = Hash.unit(h = Hash.fmix(h));
        double r3 = Hash.unit(h = Hash.fmix(h));
        double r4 = Hash.unit(h = Hash.fmix(h));
        double r5 = Hash.unit(Hash.fmix(h));
        // Keep islands well inside the footprint, where the ceiling is highest.
        double dist = (0.05 + 0.30 * r1) * slot.radius() * cell;
        double angle = r2 * 2.0 * Math.PI + i * 2.1;
        double radius = 9.0 + 12.0 * r3;
        double depth = radius * (0.9 + 0.5 * r4);
        double lo = slot.floorY() + depth + 14.0;
        double hi = slot.ceilY() - 16.0;
        double topY = lo + (hi - lo) * (0.35 + 0.5 * r5);
        if (hi < lo) {
            topY = (lo + hi) * 0.5;
        }
        return new Island(dist * Math.cos(angle), dist * Math.sin(angle), radius, topY, depth);
    }

    /**
     * Island density at a point: positive inside an island (rock), negative outside. {@code px, pz}
     * are the sample's warped offsets from the cell point in blocks.
     */
    public double density(ZoneField.Slot slot, double cell, double px, double pz, double x, double y, double z) {
        double best = -1.0;
        for (int i = 0; i < slot.islands(); i++) {
            Island is = island(slot, cell, i);
            double dx = px - is.ox();
            double dz = pz - is.oz();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > is.radius() * 1.25 + 6.0 || y > is.topY() + 8.0 || y < is.topY() - is.depth() - 10.0) {
                continue;
            }
            double rn = is.radius() * (1.0 + 0.18 * this.edge.sample(x / 14.0, 0.0, z / 14.0));
            double q = Math.min(1.0, d / rn);
            double topY = is.topY() + 1.5 * (1.0 - q * q) + 1.2 * this.top.sample(x / 9.0, 3.3, z / 9.0);
            double bottomY = is.topY() - 3.0
                - is.depth() * Math.pow(1.0 - q, 1.3) * (0.85 + 0.3 * this.under.sample(x / 11.0, y / 11.0, z / 11.0));
            double horizontal = (rn - d) / 4.0;
            double vertical = Math.min(topY - y, y - bottomY) / 6.0;
            best = Math.max(best, Math.min(horizontal, vertical));
        }
        return best;
    }

    /** True if (x, y, z) is within {@code range} blocks of an island's top surface, inside its outline. */
    public boolean nearTop(ZoneField.Slot slot, double cell, double px, double pz, double y, double range) {
        for (int i = 0; i < slot.islands(); i++) {
            Island is = island(slot, cell, i);
            double d = Math.hypot(px - is.ox(), pz - is.oz());
            if (d < is.radius() * 0.95 && Math.abs(y - is.topY()) <= range) {
                return true;
            }
        }
        return false;
    }
}
