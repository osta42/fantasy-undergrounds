package com.fantasyundergrounds.world;

import com.fantasyundergrounds.config.FuSettings;
import java.util.List;

/**
 * Deterministic environment zones (PLAN.md 2.1, "monumental" layout). Independent of biomes.
 *
 * <p>Space is divided into horizontal jittered cells (~192 blocks at cavernSizeScale 1.0) whose
 * borders are domain-warped in 3D, so walls between zones curve and lean. Every cell gets a
 * vertical <em>plan</em> over the deep world (floor y=-128):
 * <ul>
 *   <li><b>stacked</b>: a deep chamber near bedrock, a mid chamber above it and a shallow cave on top,
 *       joined by vertical shafts (the mid chamber's floor opens into a chasm down to the deep one)</li>
 *   <li><b>mega</b>: one monumental chamber spanning the deep and mid levels (80-120 tall), plus a shallow cave</li>
 * </ul>
 * Each slot rolls its environment from a depth-dependent table: Infernal Chasms, Drowned Abysses
 * and Crystal Hollows sit near bedrock, Grand Caverns, Underground Forests and Root Systems in the
 * middle, small caves above. A slot can also be ordinary rock with tunnels.
 *
 * <p>All values are pure functions of (seed, settings, position), so the same object answers
 * the density functions and the decoration placement filters identically on any thread.
 */
public final class ZoneField {
    private static final long SALT_JITTER = 0x6A09E667F3BCC908L;
    private static final long SALT_PLAN = 0xBB67AE8584CAA73BL;
    private static final long SALT_TIER = 0x3C6EF372FE94F82BL;
    private static final long SALT_PARAMS = 0xA54FF53A5F1D36F1L;
    private static final long SALT_ISLANDS = 0x510E527FADE682D1L;
    private static final long SALT_SHAPE = 0x9B05688C2B3E6C1FL;
    private static final long SALT_SHAFT = 0x1F83D9ABFB41BD6BL;

    public static final int DEEP = 0, MID = 1, UPPER = 2;

    private static final Environment[] ENVS = {
        Environment.GRAND_CAVERNS, Environment.DROWNED_ABYSSES, Environment.INFERNAL_CHASMS,
        Environment.UNDERGROUND_FORESTS, Environment.CRYSTAL_HOLLOWS, Environment.ROOT_SYSTEMS
    };
    // Per tier chances, order as ENVS. Deep: lava seas, abysses, crystal vaults near bedrock.
    private static final double[] DEEP_CHANCE = {0.064, 0.048, 0.064, 0.0, 0.056, 0.0};
    // Mid: the great caverns, forests and root systems.
    private static final double[] MID_CHANCE = {0.072, 0.012, 0.006, 0.042, 0.018, 0.03};
    // Upper: smaller shallow caves.
    private static final double[] UPPER_CHANCE = {0.03, 0.012, 0.0, 0.024, 0.0, 0.024};
    // Mega chambers (deep through mid).
    private static final double[] MEGA_CHANCE = {0.40, 0.20, 0.25, 0.0, 0.15, 0.0};

    private final long seed;
    private final FuSettings settings;
    private final double cell;
    private final double warpAmp;
    private final FuNoise warpX;
    private final FuNoise warpZ;
    private final double[][] cdf;
    private final double megaShare;
    private final double islandChance;

    public ZoneField(long seed, FuSettings settings) {
        this.seed = seed;
        this.settings = settings;
        this.cell = 192.0 * settings.cavernSize();
        this.warpAmp = 55.0 * settings.cavernSize();
        this.warpX = new FuNoise(seed, 0x51A7L, 2);
        this.warpZ = new FuNoise(seed, 0x51A8L, 2);
        this.cdf = new double[][] {cdf(DEEP_CHANCE, settings), cdf(MID_CHANCE, settings), cdf(UPPER_CHANCE, settings), cdf(MEGA_CHANCE, settings)};
        this.megaShare = Math.min(0.6, 0.12 * settings.intensity());
        this.islandChance = Math.min(1.0, 0.9 * settings.frequency(Environment.FLOATING_ISLANDS));
    }

    private static double[] cdf(double[] base, FuSettings s) {
        double[] p = new double[base.length];
        double sum = 0;
        for (int i = 0; i < base.length; i++) {
            p[i] = base[i] * s.intensity() * s.frequency(ENVS[i]);
            sum += p[i];
        }
        double scale = sum > 0.95 ? 0.95 / sum : 1.0;
        double acc = 0;
        for (int i = 0; i < p.length; i++) {
            acc += p[i] * scale;
            p[i] = acc;
        }
        return p;
    }

    public long seed() {
        return this.seed;
    }

    public FuSettings settings() {
        return this.settings;
    }

    public double cellSize() {
        return this.cell;
    }

    /** Chamber parameters of one slot of one cell. */
    public record Slot(
        Environment env,
        double centerY,
        double height,
        boolean giant,
        double pillarChance,
        boolean bridge,
        double bridgeOffset,
        double radius,
        long hash,
        int islands,
        double spikeChance,
        int tier
    ) {
        static final Slot EMPTY = new Slot(Environment.ORDINARY, 0, 0, false, 0, false, 0, 0, 0, 0, 0, -1);

        public boolean isCavern() {
            return this.env != Environment.ORDINARY;
        }

        /** Drowned Abysses only: fully submerged rather than a lake with air above (deep ones mostly). */
        public boolean submerged() {
            return this.env == Environment.DROWNED_ABYSSES && (this.hash & 0xFF) < (this.tier == DEEP ? 150 : 90);
        }

        /** Orientation of elongated chambers (Infernal rifts), radians. */
        public double angle() {
            return ((this.hash >>> 8) & 0xFFFF) / 65536.0 * Math.PI;
        }

        public double floorY() {
            return this.centerY - this.height * 0.5;
        }

        public double ceilY() {
            return this.centerY + this.height * 0.5;
        }
    }

    /** A vertical shaft joining two chambers of a column: offset from the cell point (cell units), radius (blocks), span. */
    public record Shaft(double ox, double oz, double radius, double bottomY, double topY) {}

    /** The vertical plan of one cell: up to three chambers (deep, mid, upper) and the shafts between them. */
    public record Column(Slot deep, Slot mid, Slot upper, List<Shaft> shafts) {
        public List<Slot> slots() {
            return List.of(this.deep, this.mid, this.upper);
        }

        /** The first chamber of an environment, or null. */
        public Slot find(Environment env) {
            for (Slot s : slots()) {
                if (s.env() == env) {
                    return s;
                }
            }
            return null;
        }

        /** A chamber with floating islands, or null. */
        public Slot islandSlot() {
            for (Slot s : slots()) {
                if (s.islands() > 0) {
                    return s;
                }
            }
            return null;
        }
    }

    /** Result of a lookup. {@code edge} is 0 on a cell border and grows towards the cell centre. */
    public record Sample(int cellX, int cellZ, double f1, double dx, double dz, double edge, Slot slot, boolean mergedWithNeighbour, Column column) {}

    public Sample sample(double x, double y, double z) {
        double s = this.cell;
        // Low-frequency 3D warp: borders curve horizontally and lean with height.
        double wx = x + this.warpAmp * this.warpX.sample(x / 140.0, y / 80.0, z / 140.0);
        double wz = z + this.warpAmp * this.warpZ.sample(x / 140.0, y / 80.0, z / 140.0);
        int bx = (int) Math.floor(wx / s);
        int bz = (int) Math.floor(wz / s);
        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;
        int c1x = 0, c1z = 0, c2x = 0, c2z = 0;
        double o1x = 0, o1z = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = bx + dx;
                int cz = bz + dz;
                long h = Hash.cell(this.seed, SALT_JITTER, cx, 0, cz);
                double px = (cx + 0.5 + 0.7 * (Hash.unit(h) - 0.5)) * s;
                double pz = (cz + 0.5 + 0.7 * (Hash.unit(Hash.fmix(h)) - 0.5)) * s;
                double ddx = (wx - px) / s;
                double ddz = (wz - pz) / s;
                double d = Math.sqrt(ddx * ddx + ddz * ddz);
                if (d < f1) {
                    f2 = f1; c2x = c1x; c2z = c1z;
                    f1 = d; c1x = cx; c1z = cz;
                    o1x = ddx; o1z = ddz;
                } else if (d < f2) {
                    f2 = d; c2x = cx; c2z = cz;
                }
            }
        }
        Column col = column(c1x, c1z);
        Slot slot = pickSlot(col, y);
        boolean merged = false;
        if (slot.giant()) {
            // Two adjacent giant chambers of the same environment and tier open into one cavern
            // hundreds of blocks across: the wall between them is dropped.
            Slot o = pickSlot(column(c2x, c2z), y);
            merged = o.giant() && o.env() == slot.env();
        }
        return new Sample(c1x, c1z, f1, o1x, o1z, f2 - f1, slot, merged, col);
    }

    /** Jittered centre of a cell in warped space, {x, z}; the real chamber is within ~55 blocks. */
    public double[] cellPoint(int cx, int cz) {
        long h = Hash.cell(this.seed, SALT_JITTER, cx, 0, cz);
        return new double[] {
            (cx + 0.5 + 0.7 * (Hash.unit(h) - 0.5)) * this.cell,
            (cz + 0.5 + 0.7 * (Hash.unit(Hash.fmix(h)) - 0.5)) * this.cell
        };
    }

    /** Slot whose vertical band contains y: split halfway between neighbouring chambers. */
    private static Slot pickSlot(Column col, double y) {
        Slot best = Slot.EMPTY;
        double bestGap = Double.MAX_VALUE;
        for (Slot s : col.slots()) {
            if (!s.isCavern()) {
                continue;
            }
            double gap = y < s.floorY() ? s.floorY() - y : (y > s.ceilY() ? y - s.ceilY() : 0);
            if (gap < bestGap) {
                bestGap = gap;
                best = s;
            }
        }
        return bestGap > 24 ? Slot.EMPTY : best;
    }

    public Column column(int cx, int cz) {
        long plan = Hash.cell(this.seed, SALT_PLAN, cx, 0, cz);
        boolean mega = Hash.unit(plan) < this.megaShare;
        Slot deep;
        Slot mid;
        if (mega) {
            deep = slot(cx, cz, 3, Double.NaN);
            mid = Slot.EMPTY;
        } else {
            deep = slot(cx, cz, DEEP, Double.NaN);
            mid = slot(cx, cz, MID, deep.isCavern() ? deep.ceilY() : Double.NaN);
        }
        double below = mid.isCavern() ? mid.ceilY() : (deep.isCavern() ? deep.ceilY() : Double.NaN);
        Slot upper = slot(cx, cz, UPPER, below);
        return new Column(deep, mid, upper, shafts(cx, cz, deep, mid, upper));
    }

    /** Shafts joining stacked chambers: upper -> the chamber below it, mid -> deep (a chasm in the mid floor). */
    private List<Shaft> shafts(int cx, int cz, Slot deep, Slot mid, Slot upper) {
        java.util.ArrayList<Shaft> out = new java.util.ArrayList<>(2);
        long h = Hash.cell(this.seed, SALT_SHAFT, cx, 0, cz);
        Slot lowerOfUpper = mid.isCavern() ? mid : deep;
        if (upper.isCavern() && lowerOfUpper.isCavern() && Hash.unit(h) < 0.7) {
            double a = Hash.unit(h = Hash.fmix(h)) * 6.283, r = 0.08 * Hash.unit(h = Hash.fmix(h));
            out.add(new Shaft(r * Math.cos(a), r * Math.sin(a), 4.0 + 4.0 * Hash.unit(h = Hash.fmix(h)),
                lowerOfUpper.ceilY() - 6, upper.floorY() + 4));
        }
        if (mid.isCavern() && deep.isCavern() && Hash.unit(h = Hash.fmix(h)) < 0.75) {
            double a = Hash.unit(h = Hash.fmix(h)) * 6.283, r = 0.06 * Hash.unit(h = Hash.fmix(h));
            out.add(new Shaft(r * Math.cos(a), r * Math.sin(a), 9.0 + 9.0 * Hash.unit(h = Hash.fmix(h)),
                deep.ceilY() - 6, mid.floorY() + 6));
        }
        return out;
    }

    /**
     * One slot of one cell. {@code tier} is DEEP, MID, UPPER or 3 (mega). {@code below} is the
     * ceiling of the chamber underneath (NaN if none), so stacked chambers keep a rock slab.
     */
    private Slot slot(int cx, int cz, int tier, double below) {
        long h = Hash.cell(this.seed, SALT_TIER, cx, tier, cz);
        double u = Hash.unit(h);
        double[] cdf = this.cdf[tier];
        Environment env = Environment.ORDINARY;
        for (int i = 0; i < cdf.length; i++) {
            if (u < cdf[i]) {
                env = ENVS[i];
                break;
            }
        }
        if (env == Environment.ORDINARY) {
            return Slot.EMPTY;
        }
        long p = Hash.cell(this.seed, SALT_PARAMS, cx, tier, cz);
        double r1 = Hash.unit(p);
        double r2 = Hash.unit(p = Hash.fmix(p));
        double r3 = Hash.unit(p = Hash.fmix(p));
        double r6 = Hash.unit(p = Hash.fmix(p));
        p = Hash.fmix(p);

        double minY = this.settings.minY() + 4;      // the bottom layer (diamond depth) stays solid
        double maxY = this.settings.maxY() - 6;
        int maxH = this.settings.maxCavernHeight();
        double height;
        double floor;
        switch (tier) {
            case DEEP -> {
                height = 40 + Math.min(24, maxH - 40) * Math.pow(r1, 0.9);
                floor = minY + 2 + 8 * r2;
            }
            case MID -> {
                height = 40 + Math.max(0, Math.min(26, maxH - 40)) * Math.pow(r1, 0.9);
                floor = Double.isNaN(below) ? -62 + 12 * r2 : Math.max(below + 8, -62 + 12 * r2);
            }
            case 3 -> {
                height = 80 + Math.max(0, Math.min(24, maxH + 24 - 80)) * r1;
                floor = minY + 2 + 14 * r2;
            }
            default -> {
                height = 16 + 16 * r1;
                floor = Double.isNaN(below) ? 2 + 10 * r2 : Math.max(below + 6, 2 + 10 * r2);
            }
        }
        // Infernal chasms sit right on the lava level; forests and roots never dip into the
        // vanilla lava layer.
        if (env == Environment.INFERNAL_CHASMS && tier != UPPER) {
            floor = minY;
        }
        height = Math.min(height, maxY - floor);
        if (height < 14) {
            return Slot.EMPTY;
        }
        double centerY = floor + height * 0.5;
        boolean giant = tier != UPPER && r3 < (env == Environment.GRAND_CAVERNS || tier == 3 ? 0.5 : 0.3);
        // Shape extras: rare, monumental pillars, arches and rock spikes (fewer but thicker).
        long sh = Hash.cell(this.seed, SALT_SHAPE, cx, tier, cz);
        double s1 = Hash.unit(sh), s2 = Hash.unit(sh = Hash.fmix(sh)), s3 = Hash.unit(Hash.fmix(sh));
        double pillarChance = switch (env) {
            case GRAND_CAVERNS -> 0.16 + 0.16 * s1;
            case INFERNAL_CHASMS -> 0.18 + 0.14 * s1;
            case ROOT_SYSTEMS, DROWNED_ABYSSES -> 0.08 + 0.10 * s1;
            default -> 0.04 * s1;
        };
        double bridgeOdds = switch (env) {
            case GRAND_CAVERNS -> 0.85;
            case DROWNED_ABYSSES, CRYSTAL_HOLLOWS, ROOT_SYSTEMS -> 0.5;
            case INFERNAL_CHASMS -> 0.45;
            default -> 0.0;
        };
        boolean bridge = height >= 36 && s2 < bridgeOdds;
        double bridgeOffset = (r6 - 0.5) * height * 0.3;
        double spikeChance = switch (env) {
            case GRAND_CAVERNS -> 0.14 + 0.08 * s3;
            case CRYSTAL_HOLLOWS -> 0.10 + 0.06 * s3;
            case INFERNAL_CHASMS, DROWNED_ABYSSES -> 0.10 + 0.06 * s3;
            case ROOT_SYSTEMS -> 0.06;
            default -> 0.03;
        };
        double radius = tier == UPPER ? 0.30 + 0.12 * r6
            : giant ? 0.58 : (env == Environment.DROWNED_ABYSSES ? 0.42 + 0.10 * r6 : 0.36 + 0.12 * r6);
        int islands = 0;
        if (env == Environment.GRAND_CAVERNS && tier != UPPER && height >= 56 && this.islandChance > 0) {
            long ih = Hash.cell(this.seed, SALT_ISLANDS, cx, tier, cz);
            if (Hash.unit(ih) < this.islandChance) {
                islands = 3 + (int) (Hash.unit(Hash.fmix(ih)) * 4.0) + (giant ? 2 : 0);
            }
        }
        return new Slot(env, centerY, height, giant, pillarChance, bridge, bridgeOffset, radius, p, islands, spikeChance,
            tier == 3 ? DEEP : tier);
    }
}
