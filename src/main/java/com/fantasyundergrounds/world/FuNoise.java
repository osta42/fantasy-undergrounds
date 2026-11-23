package com.fantasyundergrounds.world;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * Seeded fractal Perlin noise (fBm) built on vanilla {@link ImprovedNoise}. Immutable after
 * construction, so it is safe to share between worldgen threads.
 */
public final class FuNoise {
    private final ImprovedNoise[] octaves;
    private final double norm;

    /**
     * @param seed    world seed
     * @param salt    distinguishes independent noise fields
     * @param octaves number of octaves (each doubles frequency and halves amplitude)
     */
    public FuNoise(long seed, long salt, int octaves) {
        XoroshiroRandomSource random = new XoroshiroRandomSource(Hash.mix(seed, salt), Hash.mix(salt, 0x5DEECE66DL));
        this.octaves = new ImprovedNoise[octaves];
        double amp = 1.0;
        double total = 0.0;
        for (int i = 0; i < octaves; i++) {
            this.octaves[i] = new ImprovedNoise(random);
            total += amp;
            amp *= 0.5;
        }
        this.norm = 1.0 / total;
    }

    /** fBm in roughly [-1, 1]. Coordinates are already scaled by the caller. */
    public double sample(double x, double y, double z) {
        double sum = 0.0;
        double amp = 1.0;
        double freq = 1.0;
        for (ImprovedNoise n : this.octaves) {
            sum += n.noise(x * freq, y * freq, z * freq) * amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum * this.norm;
    }

    /** 2D convenience: samples the y = 0 plane at a fixed offset. */
    public double sample2d(double x, double z) {
        return sample(x, 17.37, z);
    }
}
