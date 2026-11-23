package com.fantasyundergrounds.world;

import com.fantasyundergrounds.config.FuSettings;
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
        ZoneField sharedZones = FuWorldgen.zones(seed);
        CavernDensity sharedCaverns = new CavernDensity(sharedZones);
        TunnelDensity sharedTunnels = new TunnelDensity(seed, s);

        long[] seq = new long[samples * 3];
        for (int i = 0; i < samples; i++) {
            int[] p = position(seed, i);
            DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            seq[i * 3] = Double.doubleToLongBits(freshCaverns.compute(ctx));
            seq[i * 3 + 1] = Double.doubleToLongBits(freshTunnels.compute(ctx));
            seq[i * 3 + 2] = freshZones.sample(p[0], p[1], p[2]).slot().env().ordinal();
        }
        AtomicLong mismatches = new AtomicLong();
        IntStream.range(0, samples).parallel().forEach(i -> {
            int[] p = position(seed, i);
            DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            if (Double.doubleToLongBits(sharedCaverns.compute(ctx)) != seq[i * 3]
                || Double.doubleToLongBits(sharedTunnels.compute(ctx)) != seq[i * 3 + 1]
                || sharedZones.sample(p[0], p[1], p[2]).slot().env().ordinal() != seq[i * 3 + 2]) {
                mismatches.incrementAndGet();
            }
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("samples", samples);
        out.put("mismatches", mismatches.get());
        out.put("configFingerprint", s.fingerprint());
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
