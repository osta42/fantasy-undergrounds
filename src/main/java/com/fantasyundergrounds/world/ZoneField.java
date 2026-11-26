package com.fantasyundergrounds.world;

import com.fantasyundergrounds.config.FuSettings;

/**
 * Deterministic environment zones (PLAN.md 2.1). Independent of biomes.
 *
 * <p>Space is divided into horizontal jittered cells (~192 blocks at cavernSizeScale 1.0) whose
 * borders are domain-warped in 3D, so walls between zones curve and lean instead of being
 * vertical planes. Every cell has two vertical <em>slots</em>: a deep one (below about y=0) and a
 * shallow one. Each slot independently rolls an environment from a depth-dependent probability
 * table, plus the parameters of its chamber (centre height, height, flags). A tall deep chamber
 * claims the whole column and leaves the shallow slot empty, which keeps the vertical budget.
 *
 * <p>All values are pure functions of (seed, settings, position), so the same object answers
 * the density functions and the decoration placement filters identically on any thread.
 */
public final class ZoneField {
    private static final long SALT_JITTER = 0x6A09E667F3BCC908L;
    private static final long SALT_DEEP = 0xBB67AE8584CAA73BL;
    private static final long SALT_SHALLOW = 0x3C6EF372FE94F82BL;
    private static final long SALT_PARAMS = 0xA54FF53A5F1D36F1L;
    private static final long SALT_ISLANDS = 0x510E527FADE682D1L;
    private static final long SALT_SHAPE = 0x9B05688C2B3E6C1FL;

    /** Default per-slot environment chances. Order matches {@link #CAVERN_ENVS}. */
    private static final Environment[] CAVERN_ENVS = {
        Environment.GRAND_CAVERNS, Environment.DROWNED_ABYSSES, Environment.INFERNAL_CHASMS,
        Environment.UNDERGROUND_FORESTS, Environment.CRYSTAL_HOLLOWS, Environment.ROOT_SYSTEMS
    };
    // "Rather common" (fantasy pass): about 37% of deep slots and 36% of shallow slots hold an
    // environment; the rest are ordinary rock with tunnels.
    private static final double[] DEEP_CHANCE = {0.11, 0.05, 0.065, 0.045, 0.06, 0.045};
    private static final double[] SHALLOW_CHANCE = {0.08, 0.06, 0.02, 0.09, 0.05, 0.06};

    private final long seed;
    private final FuSettings settings;
    private final double cell;
    private final double warpAmp;
    private final FuNoise warpX;
    private final FuNoise warpZ;
    private final double[] deepCdf;
    private final double[] shallowCdf;
    private final double islandChance;

    public ZoneField(long seed, FuSettings settings) {
        this.seed = seed;
        this.settings = settings;
        this.cell = 192.0 * settings.cavernSize();
        this.warpAmp = 55.0 * settings.cavernSize();
        this.warpX = new FuNoise(seed, 0x51A7L, 2);
        this.warpZ = new FuNoise(seed, 0x51A8L, 2);
        this.deepCdf = cdf(DEEP_CHANCE, settings);
        this.shallowCdf = cdf(SHALLOW_CHANCE, settings);
        this.islandChance = Math.min(1.0, 0.9 * settings.frequency(Environment.FLOATING_ISLANDS));
    }

    private static double[] cdf(double[] base, FuSettings s) {
        double[] p = new double[base.length];
        double sum = 0;
        for (int i = 0; i < base.length; i++) {
            p[i] = base[i] * s.intensity() * s.frequency(CAVERN_ENVS[i]);
            sum += p[i];
        }
        double scale = sum > 0.9 ? 0.9 / sum : 1.0; // always leave >= 10% ordinary rock
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
        double spikeChance
    ) {
        static final Slot EMPTY = new Slot(Environment.ORDINARY, 0, 0, false, 0, false, 0, 0, 0, 0, 0);

        public boolean isCavern() {
            return this.env != Environment.ORDINARY;
        }

        /** Drowned Abysses only: fully submerged (about 40%) rather than a lake with air above. */
        public boolean submerged() {
            return this.env == Environment.DROWNED_ABYSSES && (this.hash & 0xFF) < 100;
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

    /** Result of a lookup. {@code edge} is 0 on a cell border and grows towards the cell centre. */
    public record Sample(int cellX, int cellZ, double f1, double dx, double dz, double edge, Slot slot, boolean mergedWithNeighbour) {}

    /** Both slots of a cell (computed from the cell hash only). */
    public record Column(Slot deep, Slot shallow) {}

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
            // Two adjacent giant chambers of the same environment and depth open into one cavern
            // hundreds of blocks across: the wall between them is dropped.
            Column other = column(c2x, c2z);
            Slot o = pickSlot(other, y);
            merged = o.giant() && o.env() == slot.env();
        }
        return new Sample(c1x, c1z, f1, o1x, o1z, f2 - f1, slot, merged);
    }

    /** Jittered centre of a cell in warped space, {x, z}; the real chamber is within ~55 blocks. */
    public double[] cellPoint(int cx, int cz) {
        long h = Hash.cell(this.seed, SALT_JITTER, cx, 0, cz);
        return new double[] {
            (cx + 0.5 + 0.7 * (Hash.unit(h) - 0.5)) * this.cell,
            (cz + 0.5 + 0.7 * (Hash.unit(Hash.fmix(h)) - 0.5)) * this.cell
        };
    }

    /** Slot whose vertical band contains y. */
    private static Slot pickSlot(Column col, double y) {
        Slot deep = col.deep();
        Slot shallow = col.shallow();
        double split;
        if (deep.isCavern() && shallow.isCavern()) {
            split = (deep.ceilY() + shallow.floorY()) * 0.5;
        } else if (deep.isCavern()) {
            split = deep.ceilY() + 8;
        } else if (shallow.isCavern()) {
            split = shallow.floorY() - 8;
        } else {
            return Slot.EMPTY;
        }
        return y < split ? deep : shallow;
    }

    public Column column(int cx, int cz) {
        Slot deep = slot(cx, cz, true);
        Slot shallow = deep.isCavern() && deep.height() >= 56 ? Slot.EMPTY : slot(cx, cz, false);
        return new Column(deep, shallow);
    }

    private Slot slot(int cx, int cz, boolean deep) {
        long h = Hash.cell(this.seed, deep ? SALT_DEEP : SALT_SHALLOW, cx, 0, cz);
        // (radius of the chamber footprint is derived from the params hash below)
        double u = Hash.unit(h);
        double[] cdf = deep ? this.deepCdf : this.shallowCdf;
        Environment env = Environment.ORDINARY;
        for (int i = 0; i < cdf.length; i++) {
            if (u < cdf[i]) {
                env = CAVERN_ENVS[i];
                break;
            }
        }
        if (env == Environment.ORDINARY) {
            return Slot.EMPTY;
        }
        long p = Hash.cell(this.seed, SALT_PARAMS, cx, deep ? 0 : 1, cz);
        double r1 = Hash.unit(p);
        double r2 = Hash.unit(p = Hash.fmix(p));
        double r3 = Hash.unit(p = Hash.fmix(p));
        double r4 = Hash.unit(p = Hash.fmix(p));
        double r5 = Hash.unit(p = Hash.fmix(p));
        double r6 = Hash.unit(p = Hash.fmix(p));

        int maxH = this.settings.maxCavernHeight();
        double minH;
        double maxHEnv;
        switch (env) {
            case GRAND_CAVERNS -> { minH = 40; maxHEnv = maxH; }
            case INFERNAL_CHASMS -> { minH = 34; maxHEnv = Math.min(maxH, 84); }
            case DROWNED_ABYSSES -> { minH = 28; maxHEnv = Math.min(maxH, 64); }
            default -> { minH = 26; maxHEnv = Math.min(maxH, 60); }
        }
        // Deeper = more dramatic: deep slots skew tall, shallow slots stay modest.
        double skew = deep ? Math.pow(r1, 1.05) : Math.pow(r1, 1.6);
        double height = minH + (maxHEnv - minH) * skew;
        if (!deep) {
            height = Math.min(height, 48);
        }
        double centerY;
        double minY = this.settings.minY() + 2;
        double maxY = this.settings.maxY() - 6;
        if (deep) {
            centerY = -34 + 16 * (r2 - 0.5);
            centerY = Math.max(centerY, minY + height * 0.5);
            // Every open space below y=-54 is filled with lava by vanilla (hardcoded); keep the
            // floors of forests, crystal hollows and root systems above it.
            if (env == Environment.UNDERGROUND_FORESTS || env == Environment.CRYSTAL_HOLLOWS || env == Environment.ROOT_SYSTEMS) {
                centerY = Math.max(centerY, -48 + height * 0.5);
            }
        } else {
            centerY = 16 + 14 * (r2 - 0.5);
            centerY = Math.min(centerY, maxY - height * 0.5);
        }
        // Giant chambers reach the cell border, and two giant neighbours of the same environment
        // merge into one cavern hundreds of blocks across.
        boolean giant = deep && r3 < (env == Environment.GRAND_CAVERNS ? 0.5 : 0.28);
        // Shape extras from a separate hash: pillars, natural bridges and rock spikes per environment.
        long sh = Hash.cell(this.seed, SALT_SHAPE, cx, deep ? 0 : 1, cz);
        double s1 = Hash.unit(sh), s2 = Hash.unit(sh = Hash.fmix(sh)), s3 = Hash.unit(Hash.fmix(sh));
        double pillarChance = switch (env) {
            case GRAND_CAVERNS -> 0.30 + 0.35 * s1;
            case INFERNAL_CHASMS -> 0.20 + 0.20 * s1;
            case ROOT_SYSTEMS -> 0.15 + 0.20 * s1;
            default -> 0.05 + 0.08 * s1;
        };
        double bridgeOdds = switch (env) {
            case GRAND_CAVERNS -> 0.8;
            case DROWNED_ABYSSES, CRYSTAL_HOLLOWS, ROOT_SYSTEMS -> 0.4;
            case INFERNAL_CHASMS -> 0.3;
            default -> 0.0;
        };
        boolean bridge = height >= 36 && s2 < bridgeOdds;
        double bridgeOffset = (r6 - 0.5) * height * 0.4;
        double spikeChance = switch (env) {
            case GRAND_CAVERNS -> 0.40 + 0.20 * s3;
            case CRYSTAL_HOLLOWS -> 0.45 + 0.20 * s3;
            case INFERNAL_CHASMS, DROWNED_ABYSSES -> 0.25 + 0.15 * s3;
            case ROOT_SYSTEMS -> 0.20;
            default -> 0.08;
        };
        // Footprint radius in cell units. Drowned Abysses are wider: enormous underground lakes.
        double radius = giant ? 0.66 : (env == Environment.DROWNED_ABYSSES ? 0.46 + 0.14 * r6 : 0.38 + 0.14 * r6);
        // Floating Stone Islands: only in tall deep Grand Caverns (>= 60 blocks), from a separate
        // hash so every other chamber parameter stays exactly as before.
        int islands = 0;
        if (env == Environment.GRAND_CAVERNS && deep && height >= 56 && this.islandChance > 0) {
            long ih = Hash.cell(this.seed, SALT_ISLANDS, cx, 0, cz);
            if (Hash.unit(ih) < this.islandChance) {
                // A cluster of islands of mixed sizes.
                islands = 3 + (int) (Hash.unit(Hash.fmix(ih)) * 4.0) + (giant ? 2 : 0);
            }
        }
        return new Slot(env, centerY, height, giant, pillarChance, bridge, bridgeOffset, radius, p, islands, spikeChance);
    }
}
