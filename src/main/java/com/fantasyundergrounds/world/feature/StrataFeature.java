package com.fantasyundergrounds.world.feature;

import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuNoise;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code fantasyundergrounds:strata}: macro rock strata on every exposed chamber wall of a chunk
 * (overhaul spec section 1: "deepslate bases, tuff mid-sections, stone/calcite upper walls"
 * and dark ceilings, instead of a 1x1 patchwork).
 *
 * <p>Placed once per chunk. For each column it finds the chamber (zone slot) and maps the height
 * inside it, {@code t = (y - floor) / (ceiling - floor)}, to a palette band of that environment.
 * Band borders are warped by a low-frequency noise so they undulate like real bedding planes.
 * Only {@code #fantasyundergrounds:replaceable_base} blocks within 2 blocks of open space (air or
 * fluid) are replaced; ores (of any mod) and every other block are left alone. It runs in the
 * underground_decoration step, after all ores have been placed.
 *
 * <p>Writes only inside its own chunk. Reads one block beyond it (the exposure test), like any
 * vanilla decoration feature.
 */
public final class StrataFeature extends Feature<NoneFeatureConfiguration> {
    /** One band: up to (exclusive) relative height {@code top}, the block to use (null = keep the base rock). */
    private record Band(double top, BlockState state) {}

    private static final Map<Environment, Band[]> PALETTES = new EnumMap<>(Environment.class);

    static {
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState tuff = Blocks.TUFF.defaultBlockState();
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState dripstone = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
        BlockState granite = Blocks.GRANITE.defaultBlockState();
        BlockState prismarine = Blocks.PRISMARINE.defaultBlockState();
        BlockState darkPrismarine = Blocks.DARK_PRISMARINE.defaultBlockState();
        PALETTES.put(Environment.GRAND_CAVERNS, new Band[] {
            new Band(0.30, deepslate), new Band(0.55, tuff), new Band(0.60, calcite),
            new Band(0.78, stone), new Band(0.82, calcite), new Band(0.88, andesite), new Band(9, deepslate)});
        PALETTES.put(Environment.UNDERGROUND_FORESTS, new Band[] {
            new Band(0.25, deepslate), new Band(0.45, tuff), new Band(0.50, calcite),
            new Band(0.62, dripstone), new Band(0.80, stone), new Band(9, deepslate)});
        PALETTES.put(Environment.CRYSTAL_HOLLOWS, new Band[] {
            new Band(0.32, tuff), new Band(0.36, calcite), new Band(0.66, deepslate), new Band(9, smoothBasalt)});
        PALETTES.put(Environment.INFERNAL_CHASMS, new Band[] {
            new Band(0.28, blackstone), new Band(0.52, basalt), new Band(0.74, deepslate), new Band(9, blackstone)});
        PALETTES.put(Environment.DROWNED_ABYSSES, new Band[] {
            new Band(0.30, deepslate), new Band(0.34, prismarine), new Band(0.58, tuff),
            new Band(0.63, darkPrismarine), new Band(9, deepslate)});
        PALETTES.put(Environment.ROOT_SYSTEMS, new Band[] {
            new Band(0.28, deepslate), new Band(0.52, dripstone), new Band(0.58, granite),
            new Band(0.84, stone), new Band(9, deepslate)});
    }


    private static volatile FuNoise warpCache;
    private static volatile long warpSeed;

    public StrataFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static FuNoise warp(long seed) {
        FuNoise n = warpCache;
        if (n == null || warpSeed != seed) {
            n = new FuNoise(seed, 0x57A7AL, 2);
            warpSeed = seed;
            warpCache = n;
        }
        return n;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeStrata(context.level(), context.origin());
    }

    /** Shared with the purity self-test. */
    public static boolean placeStrata(WorldGenLevel level, BlockPos origin) {
        if (!FuWorldgen.appliesTo(level.getLevel().dimension())) {
            return false;
        }
        long seed = level.getSeed();
        ZoneField zones = FuWorldgen.zones(seed);
        FuNoise warp = warp(seed);
        ChunkPos cp = ChunkPos.containing(origin);
        ChunkAccess chunk = level.getChunk(cp.x(), cp.z());
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        int yLo = Math.max(level.getMinY() + 1, zones.settings().minY() - 4);
        int yHi = Math.min(level.getMaxY() - 1, zones.settings().maxY() + 8);
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        // open[] covers the section plus a 2-block margin on every side (P = 20 per axis).
        boolean[] open = new boolean[P * P * P];
        double[] bend = new double[256];
        boolean bendDone = false;
        boolean placed = false;
        for (int segY = Math.floorDiv(yLo, 16) * 16; segY <= yHi; segY += 16) {
            int idx = chunk.getSectionIndex(segY);
            if (idx < 0 || idx >= sections.length || !openNear(sections, idx)) {
                continue;
            }
            LevelChunkSection section = sections[idx];
            if (section.hasOnlyAir()) {
                continue;
            }
            fillOpen(level, chunk, sections, idx, segY, minX, minZ, probe, open);
            if (!bendDone) {
                for (int i = 0; i < 256; i++) {
                    bend[i] = 0.07 * warp.sample2d((minX + (i & 15)) / 70.0, (minZ + (i >> 4)) / 70.0);
                }
                bendDone = true;
            }
            for (int sx = 0; sx < 4; sx++) {
                for (int sz = 0; sz < 4; sz++) {
                    Band[] palette = null;
                    double floor = 0;
                    double span = 1;
                    boolean looked = false;
                    for (int dx = 0; dx < 4; dx++) {
                        for (int dz = 0; dz < 4; dz++) {
                            int lx = sx * 4 + dx;
                            int lz = sz * 4 + dz;
                            for (int ly = 0; ly < 16; ly++) {
                                int y = segY + ly;
                                if (y < yLo || y > yHi || open[at(lx, ly, lz)] || !exposed(open, lx, ly, lz)) {
                                    continue;
                                }
                                BlockState here = section.getBlockState(lx, ly, lz);
                                if (!here.is(SkinFeature.REPLACEABLE_BASE)) {
                                    continue;
                                }
                                if (!looked) {
                                    // One zone lookup per 4 x 4 x 16 block, only where there is something to paint.
                                    looked = true;
                                    int cx = minX + sx * 4 + 2;
                                    int cz = minZ + sz * 4 + 2;
                                    ZoneField.Slot slot = zones.sample(cx, segY + 8, cz).slot();
                                    palette = PALETTES.get(slot.env());
                                    if (palette != null && level.getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(segY + 8),
                                        QuartPos.fromBlock(cz)).is(InZoneFilter.KEEPS_OWN_DECORATION)) {
                                        palette = null;
                                    }
                                    floor = slot.floorY();
                                    span = Math.max(8.0, slot.ceilY() - floor);
                                }
                                if (palette == null) {
                                    break;
                                }
                                int x = minX + lx;
                                int z = minZ + lz;
                                double t = (y - floor) / span + bend[lx | (lz << 4)] + 0.025 * warp.sample(x / 9.0, y / 5.0, z / 9.0);
                                BlockState band = pick(palette, t);
                                if (band != null && band != here) {
                                    // Same-shape opaque rock for opaque rock: no light or heightmap change,
                                    // so the section is written directly, like vanilla's ore feature.
                                    section.setBlockState(lx, ly, lz, band, false);
                                    placed = true;
                                }
                            }
                            if (looked && palette == null) {
                                break;
                            }
                        }
                        if (looked && palette == null) {
                            break;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static final int P = 20;

    private static int at(int lx, int ly, int lz) {
        return (lx + 2) + P * ((lz + 2) + P * (ly + 2));
    }

    /** Open space (air or fluid) within 2 blocks along any axis, from the slab's open map. */
    private static boolean exposed(boolean[] open, int lx, int ly, int lz) {
        return open[at(lx + 1, ly, lz)] || open[at(lx - 1, ly, lz)] || open[at(lx, ly + 1, lz)] || open[at(lx, ly - 1, lz)]
            || open[at(lx, ly, lz + 1)] || open[at(lx, ly, lz - 1)]
            || open[at(lx + 2, ly, lz)] || open[at(lx - 2, ly, lz)] || open[at(lx, ly + 2, lz)] || open[at(lx, ly - 2, lz)]
            || open[at(lx, ly, lz + 2)] || open[at(lx, ly, lz - 2)];
    }

    /** Fills the open map of one section and its 2-block margin (corners of the margin are not needed). */
    private static void fillOpen(WorldGenLevel level, ChunkAccess chunk, LevelChunkSection[] sections, int idx, int segY,
                                 int minX, int minZ, BlockPos.MutableBlockPos probe, boolean[] open) {
        java.util.Arrays.fill(open, false);
        for (int ly = -2; ly < 18; ly++) {
            int si = idx + Math.floorDiv(ly, 16);
            LevelChunkSection sec = si >= 0 && si < sections.length ? sections[si] : null;
            int sy = Math.floorMod(ly, 16);
            boolean inner = ly >= 0 && ly < 16;
            for (int lz = -2; lz < 18; lz++) {
                boolean zIn = lz >= 0 && lz < 16;
                for (int lx = -2; lx < 18; lx++) {
                    boolean xIn = lx >= 0 && lx < 16;
                    if (xIn && zIn) {
                        open[at(lx, ly, lz)] = sec != null && open(sec.getBlockState(lx, sy, lz));
                    } else if (inner && (xIn || zIn)) {
                        // Horizontal margin, in the neighbouring chunk.
                        probe.set(minX + lx, segY + ly, minZ + lz);
                        open[at(lx, ly, lz)] = open(level.getBlockState(probe));
                    }
                }
            }
        }
    }

    private static BlockState pick(Band[] palette, double t) {
        for (Band b : palette) {
            if (t < b.top()) {
                return b.state();
            }
        }
        return null;
    }

    private static boolean openNear(LevelChunkSection[] sections, int idx) {
        for (int i = idx - 1; i <= idx + 1; i++) {
            if (i < 0 || i >= sections.length) {
                continue;
            }
            LevelChunkSection sec = sections[i];
            if (sec.hasOnlyAir() || sec.getStates().maybeHas(StrataFeature::open)) {
                return true;
            }
        }
        return false;
    }

    private static boolean open(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty();
    }

}
