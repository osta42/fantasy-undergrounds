package com.fantasyundergrounds.world;

/** Deterministic integer hashing (SplitMix64 finaliser). */
public final class Hash {
    private Hash() {}

    public static long mix(long a, long b) {
        return fmix(a * 0x9E3779B97F4A7C15L + b);
    }

    public static long cell(long seed, long salt, int x, int y, int z) {
        long h = seed ^ salt;
        h = fmix(h + x * 0x632BE59BD9B4E019L);
        h = fmix(h + y * 0x85157AF5L);
        h = fmix(h + z * 0x9E3779B97F4A7C15L);
        return h;
    }

    /** Uniform double in [0, 1) from the top 53 bits. */
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    public static long fmix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
