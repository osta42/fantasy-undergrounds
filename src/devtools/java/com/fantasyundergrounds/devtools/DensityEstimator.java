package com.fantasyundergrounds.devtools;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Fast open-volume estimate straight from the level's wired final_density, without generating
 * chunks: samples a large area on a jittered grid and reports the fraction of points with
 * density <= 0 (air or fluid) per 8-block Y band. Works identically for vanilla and modded
 * worlds, so it is the tuning tool for the air budget (PLAN.md 2.2). It ignores the structure
 * beardifier and interpolation, so it is an estimate; the chunk audit is the ground truth.
 */
final class DensityEstimator {
    private DensityEstimator() {}

    private static final int[] MAP_LEVELS = {-44, -28, -12, 4, 20};

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** Top-down density maps, 4 blocks per pixel: black = open, grey = rock. */
    private static void writeMaps(ServerLevel level, DensityFunction f, int cx, int cz, int radius) {
        int px = radius * 2 / 4;
        java.nio.file.Path dir = level.getServer().getServerDirectory().resolve("fu-audit").resolve("maps");
        try {
            java.nio.file.Files.createDirectories(dir);
            for (int y : MAP_LEVELS) {
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_RGB);
                for (int i = 0; i < px; i++) {
                    for (int j = 0; j < px; j++) {
                        double d = f.compute(new DensityFunction.SinglePointContext(cx - radius + i * 4, y, cz - radius + j * 4));
                        img.setRGB(i, j, d <= 0 ? 0x101014 : 0x8C8C8C);
                    }
                }
                javax.imageio.ImageIO.write(img, "png", dir.resolve("map-y" + y + ".png").toFile());
            }
        } catch (java.io.IOException e) {
            FuDevTools.LOGGER.error("[fu-audit] cannot write density maps", e);
        }
    }

    static Map<String, Object> run(ServerLevel level, int centerBlockX, int centerBlockZ, int radiusBlocks) {
        DensityFunction f = level.getChunkSource().randomState().router().finalDensity();
        int step = 8;
        int minY = -64;
        int maxY = 80;
        int bands = (maxY - minY) / 8;
        long[] open = new long[bands];
        long[] total = new long[bands];
        long t0 = System.nanoTime();
        // Emulates NoiseChunk: final_density is evaluated only at the corners of 4 x 8 x 4 cells
        // and trilinearly interpolated inside (the sign survives the later squeeze). Sample one
        // whole cell column every 16 blocks and classify all 128 blocks of each cell in it.
        int cells = (maxY - minY) / 8;
        double[][] corner = new double[4][cells + 1];
        for (int x = centerBlockX - radiusBlocks; x < centerBlockX + radiusBlocks; x += 16) {
            for (int z = centerBlockZ - radiusBlocks; z < centerBlockZ + radiusBlocks; z += 16) {
                long h = (x * 341873128712L) ^ (z * 132897987541L);
                int cx = x + 4 * (int) Math.floorMod(h, 4);
                int cz = z + 4 * (int) Math.floorMod(h >>> 8, 4);
                for (int c = 0; c < 4; c++) {
                    int px = cx + ((c & 1) == 0 ? 0 : 4);
                    int pz = cz + ((c & 2) == 0 ? 0 : 4);
                    for (int k = 0; k <= cells; k++) {
                        corner[c][k] = f.compute(new DensityFunction.SinglePointContext(px, minY + 8 * k, pz));
                    }
                }
                for (int k = 0; k < cells; k++) {
                    int b = k;
                    for (int iy = 0; iy < 8; iy++) {
                        double ty = iy / 8.0;
                        for (int ix = 0; ix < 4; ix++) {
                            double tx = ix / 4.0;
                            for (int iz = 0; iz < 4; iz++) {
                                double tz = iz / 4.0;
                                double v00 = lerp(ty, corner[0][k], corner[0][k + 1]);
                                double v10 = lerp(ty, corner[1][k], corner[1][k + 1]);
                                double v01 = lerp(ty, corner[2][k], corner[2][k + 1]);
                                double v11 = lerp(ty, corner[3][k], corner[3][k + 1]);
                                double v = lerp(tz, lerp(tx, v00, v10), lerp(tx, v01, v11));
                                total[b]++;
                                if (v <= 0.0) {
                                    open[b]++;
                                }
                            }
                        }
                    }
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Double> perBand = new LinkedHashMap<>();
        long openBelow0 = 0;
        long totalBelow0 = 0;
        for (int b = 0; b < bands; b++) {
            int y = minY + b * 8;
            perBand.put(String.valueOf(y), 100.0 * open[b] / Math.max(1, total[b]));
            if (y < 0) {
                openBelow0 += open[b];
                totalBelow0 += total[b];
            }
        }
        out.put("radiusBlocks", radiusBlocks);
        out.put("openPctBelow0", 100.0 * openBelow0 / Math.max(1, totalBelow0));
        out.put("openPctByBand", perBand);
        out.put("ms", (System.nanoTime() - t0) / 1_000_000);
        writeMaps(level, f, centerBlockX, centerBlockZ, radiusBlocks);
        FuDevTools.LOGGER.info("[fu-audit] density estimate: {}% open below y=0 over {} blocks radius ({} ms)",
            String.format("%.2f", out.get("openPctBelow0")), radiusBlocks, out.get("ms"));
        return out;
    }
}
