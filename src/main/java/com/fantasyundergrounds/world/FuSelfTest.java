package com.fantasyundergrounds.world;

import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.density.CanyonDensity;
import com.fantasyundergrounds.world.density.CavernDensity;
import com.fantasyundergrounds.world.density.TunnelDensity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Gate 5b purity check: this mod's zone field and density functions must be pure functions of
 * (seed, config, position). Evaluates a fixed set of positions sequentially on fresh instances
 * and in parallel on the shared instances, and compares results bit for bit. Called by the
 * dev-only audit tooling through reflection; harmless and unused in normal play.
 */
public final class FuSelfTest {
    private FuSelfTest() {}

    public static Map<String, Object> run(long seed, int samples) {
        FuSettings s = FuSettings.get();
        ZoneField freshZones = new ZoneField(seed, s);
        CavernDensity freshCaverns = new CavernDensity(freshZones);
        TunnelDensity freshTunnels = new TunnelDensity(seed, s);
        CanyonDensity freshCanyons = new CanyonDensity(new CanyonField(seed, s), seed, s.minY(), s.maxY());
        ZoneField sharedZones = FuWorldgen.zones(seed);
        CavernDensity sharedCaverns = new CavernDensity(sharedZones);
        TunnelDensity sharedTunnels = new TunnelDensity(seed, s);
        CanyonDensity sharedCanyons = new CanyonDensity(FuWorldgen.canyons(seed), seed, s.minY(), s.maxY());

        long[] seq = new long[samples * 4];
        for (int i = 0; i < samples; i++) {
            int[] p = position(seed, i);
            DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            seq[i * 4] = Double.doubleToLongBits(freshCaverns.compute(ctx));
            seq[i * 4 + 1] = Double.doubleToLongBits(freshTunnels.compute(ctx));
            seq[i * 4 + 2] = freshZones.sample(p[0], p[1], p[2]).slot().env().ordinal();
            seq[i * 4 + 3] = Double.doubleToLongBits(freshCanyons.compute(ctx));
        }
        AtomicLong mismatches = new AtomicLong();
        IntStream.range(0, samples).parallel().forEach(i -> {
            int[] p = position(seed, i);
            DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            if (Double.doubleToLongBits(sharedCaverns.compute(ctx)) != seq[i * 4]
                || Double.doubleToLongBits(sharedTunnels.compute(ctx)) != seq[i * 4 + 1]
                || sharedZones.sample(p[0], p[1], p[2]).slot().env().ordinal() != seq[i * 4 + 2]
                || Double.doubleToLongBits(sharedCanyons.compute(ctx)) != seq[i * 4 + 3]) {
                mismatches.incrementAndGet();
            }
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("samples", samples);
        out.put("mismatches", mismatches.get());
        out.put("configFingerprint", s.fingerprint());
        out.put("phase3", phase3Coverage(seed, freshZones, new CanyonField(seed, s)));
        // Ancient Depths ruin layouts (Phase 5): census per layout, and the same twice.
        Map<String, Object> ruins = new LinkedHashMap<>();
        boolean ruinsDeterministic = true;
        for (int v = 0; v < com.fantasyundergrounds.world.structure.AncientRuinsPiece.VARIANTS; v++) {
            Map<String, Integer> a = com.fantasyundergrounds.world.structure.AncientRuinsPiece.census(v, seed + v);
            ruinsDeterministic &= a.equals(com.fantasyundergrounds.world.structure.AncientRuinsPiece.census(v, seed + v));
            ruins.put("layout" + v, a);
        }
        ruins.put("deterministic", ruinsDeterministic);
        out.put("ruinLayouts", ruins);
        out.put("features", FeatureSelfTest.run(seed));
        return out;
    }

    /**
     * How many sampled positions fall in canyons and islands. Uniform random positions almost
     * never land in such rare features, so this also samples near located canyons and island
     * chambers, which exercises those code paths in the purity comparison above as well.
     */
    private static Map<String, Object> phase3Coverage(long seed, ZoneField zones, CanyonField canyons) {
        int canyonCells = 0, islandCells = 0, canyonOpen = 0, islandRock = 0, floating = 0;
        String example = null;
        IslandField islands = new IslandField(seed);
        CavernDensity caverns = new CavernDensity(zones);
        CanyonDensity canyonDensity = new CanyonDensity(canyons, seed, zones.settings().minY(), zones.settings().maxY());
        for (int gx = -8; gx < 8; gx++) {
            for (int gz = -8; gz < 8; gz++) {
                CanyonField.Canyon c = canyons.canyon(gx, gz);
                if (c == null) {
                    continue;
                }
                canyonCells++;
                for (int k = 0; k < 64; k++) {
                    double y = c.floorY() + 3 + k % 16 * 3;
                    double x = c.cx() + (k - 32) * 4 * c.ax();
                    double z = c.cz() + (k - 32) * 4 * c.az();
                    if (canyonDensity.compute(new DensityFunction.SinglePointContext((int) x, (int) y, (int) z)) < 0) {
                        canyonOpen++;
                    }
                }
            }
        }
        for (int cx = -40; cx < 40; cx++) {
            for (int cz = -40; cz < 40; cz++) {
                ZoneField.Slot slot = zones.column(cx, cz).deep();
                if (slot.islands() == 0) {
                    continue;
                }
                islandCells++;
                double[] cp = zones.cellPoint(cx, cz);
                double cell = zones.cellSize();
                IslandField.Island is = IslandField.island(slot, cell, 0);
                int y = (int) (is.topY() - 3);
                // The island offset is in warped space; scan real positions around it. A floating
                // island column: rock under the top, open air above it and open air below the cone.
                for (int ox = -96; ox <= 96; ox += 4) {
                    for (int oz = -96; oz <= 96; oz += 4) {
                        int x = (int) (cp[0] + is.ox()) + ox, z = (int) (cp[1] + is.oz()) + oz;
                        ZoneField.Sample smp = zones.sample(x, y, z);
                        if (smp.slot().islands() == 0
                            || islands.density(smp.slot(), cell, smp.dx() * cell, smp.dz() * cell, x, y, z) <= 0) {
                            continue;
                        }
                        islandRock++;
                        int below = (int) (is.topY() - is.depth() - 8);
                        if (caverns.compute(new DensityFunction.SinglePointContext(x, (int) is.topY() + 4, z)) < 0
                            && caverns.compute(new DensityFunction.SinglePointContext(x, below, z)) < 0) {
                            floating++;
                            if (example == null) {
                                example = x + "," + (int) is.topY() + "," + z;
                            }
                        }
                    }
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("canyonCellsIn16x16", canyonCells);
        out.put("canyonOpenOnAxisSamples", canyonOpen);
        out.put("islandChambersIn80x80", islandCells);
        out.put("islandRockSamplesUnderTop", islandRock);
        out.put("floatingIslandColumns", floating);
        out.put("exampleIslandTop", example);
        return out;
    }

    private static int[] position(long seed, int i) {
        long h = Hash.cell(seed, 0x5E1FL, i, 0, 0);
        int x = (int) (Hash.unit(h) * 200_000) - 100_000;
        int z = (int) (Hash.unit(Hash.fmix(h)) * 200_000) - 100_000;
        int y = (int) (Hash.unit(Hash.fmix(h + 1)) * 128) - 64;
        return new int[] {x, y, z};
    }
}
