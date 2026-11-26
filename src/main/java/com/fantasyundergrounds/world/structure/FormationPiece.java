package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.world.Hash;
import com.fantasyundergrounds.world.feature.SkinFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One giant formation ({@link FormationStructure}). The shape is a pure function of (seed,
 * position), so every chunk the piece crosses builds its own part consistently, in any order.
 * Blocks only go into air, base stone ({@code #fantasyundergrounds:replaceable_base}) or plants
 * ({@code #minecraft:replaceable_by_trees}), like a tree: ores, fluids, other structures and
 * other mods' blocks are never overwritten.
 */
public final class FormationPiece extends StructurePiece {
    private static final BlockState STEM = Blocks.MUSHROOM_STEM.defaultBlockState();
    private static final BlockState RED_CAP = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
    private static final BlockState BROWN_CAP = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
    private static final BlockState SHROOMLIGHT = Blocks.SHROOMLIGHT.defaultBlockState();
    private static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState BUDDING = Blocks.BUDDING_AMETHYST.defaultBlockState();
    private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    private static final BlockState CRYSTAL_CORE = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
    private static final BlockState ROOT_WOOD = Blocks.DARK_OAK_WOOD.defaultBlockState();
    private static final BlockState ROOT_SHELL = Blocks.MANGROVE_ROOTS.defaultBlockState();
    private static final BlockState SAP = Blocks.VERDANT_FROGLIGHT.defaultBlockState();
    private static final BlockState HANGING_ROOTS = Blocks.HANGING_ROOTS.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();

    private final FormationStructure.Kind kind;
    private final long seed;
    private final int x0, y0, z0, ceiling;
    /** Giant crystals only: index into {@link #WALL_DX}/{@link #WALL_DZ} of the wall the cluster grows from, or -1. */
    private final int wall;

    /** Eight horizontal directions (the scan that found the wall); the crystal grows the opposite way. */
    static final double[] WALL_DX = {1, 0.7071, 0, -0.7071, -1, -0.7071, 0, 0.7071};
    static final double[] WALL_DZ = {0, 0.7071, 1, 0.7071, 0, -0.7071, -1, -0.7071};

    // Derived parameters (from the seed).
    private final double p1, p2, p3, p4, p5, p6;

    public FormationPiece(FormationStructure.Kind kind, long seed, int x, int floor, int z, int ceiling) {
        this(kind, seed, x, floor, z, ceiling, -1);
    }

    public FormationPiece(FormationStructure.Kind kind, long seed, int x, int floor, int z, int ceiling, int wall) {
        super(FuRegistries.FORMATION_PIECE.get(), 0, box(kind, x, floor, z, ceiling, wall));
        this.kind = kind;
        this.wall = wall;
        this.seed = seed;
        this.x0 = x;
        this.y0 = floor;
        this.z0 = z;
        this.ceiling = ceiling;
        this.setOrientation(null);
        double[] p = params(seed);
        this.p1 = p[0]; this.p2 = p[1]; this.p3 = p[2]; this.p4 = p[3]; this.p5 = p[4]; this.p6 = p[5];
    }

    public FormationPiece(CompoundTag tag) {
        super(FuRegistries.FORMATION_PIECE.get(), tag);
        this.kind = FormationStructure.Kind.values()[Math.floorMod(tag.getIntOr("Kind", 0), FormationStructure.Kind.values().length)];
        this.seed = tag.getLongOr("Seed", 0L);
        this.x0 = tag.getIntOr("X", 0);
        this.y0 = tag.getIntOr("Y", 0);
        this.z0 = tag.getIntOr("Z", 0);
        this.ceiling = tag.getIntOr("Ceil", 0);
        this.wall = tag.getIntOr("Wall", -1);
        double[] p = params(this.seed);
        this.p1 = p[0]; this.p2 = p[1]; this.p3 = p[2]; this.p4 = p[3]; this.p5 = p[4]; this.p6 = p[5];
    }

    private static double[] params(long seed) {
        double[] p = new double[6];
        long h = seed;
        for (int i = 0; i < 6; i++) {
            h = Hash.fmix(h + 0x9E3779B97F4A7C15L);
            p[i] = Hash.unit(h);
        }
        return p;
    }

    private static BoundingBox box(FormationStructure.Kind kind, int x, int floor, int z, int ceiling, int wall) {
        int r = switch (kind) {
            case GIANT_MUSHROOM -> 16;
            case GIANT_CRYSTAL -> wall >= 0 ? 40 : 34;
            case GIANT_ROOT -> 34;
            case INFERNAL_SPIRE -> 10;
        };
        int below = wall >= 0 ? 18 : 3;
        return new BoundingBox(x - r, floor - below, z - r, x + r, Math.max(floor + 4, ceiling + 1), z + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Kind", this.kind.ordinal());
        tag.putLong("Seed", this.seed);
        tag.putInt("X", this.x0);
        tag.putInt("Y", this.y0);
        tag.putInt("Z", this.z0);
        tag.putInt("Ceil", this.ceiling);
        tag.putInt("Wall", this.wall);
    }

    @Override
    protected boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox chunkBB) {
        BlockState s = level.getBlockState(this.getWorldPos(x, y, z));
        return s.isAir() || s.is(SkinFeature.REPLACEABLE_BASE) || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private double h(int x, int y, int z, long salt) {
        return Hash.unit(Hash.cell(this.seed, salt, x, y, z));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        BoundingBox bb = this.boundingBox;
        int minX = Math.max(bb.minX(), chunkBB.minX()), maxX = Math.min(bb.maxX(), chunkBB.maxX());
        int minZ = Math.max(bb.minZ(), chunkBB.minZ()), maxZ = Math.min(bb.maxZ(), chunkBB.maxZ());
        int minY = Math.max(bb.minY(), chunkBB.minY()), maxY = Math.min(bb.maxY(), chunkBB.maxY());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockState s = switch (this.kind) {
                        case GIANT_MUSHROOM -> mushroom(x, y, z);
                        case GIANT_CRYSTAL -> crystal(x, y, z);
                        case GIANT_ROOT -> root(x, y, z);
                        case INFERNAL_SPIRE -> spire(x, y, z);
                    };
                    if (s != null) {
                        this.placeBlock(level, s, x, y, z, chunkBB);
                    }
                }
            }
        }
    }

    /** Block of this formation at a world position, or null (dev self-test uses it too). */
    BlockState blockAt(int x, int y, int z) {
        return switch (this.kind) {
            case GIANT_MUSHROOM -> mushroom(x, y, z);
            case GIANT_CRYSTAL -> crystal(x, y, z);
            case GIANT_ROOT -> root(x, y, z);
            case INFERNAL_SPIRE -> spire(x, y, z);
        };
    }

    // ------------------------------------------------------------ giant mushroom

    private BlockState mushroom(int x, int y, int z) {
        boolean red = this.p1 < 0.6;
        double capR = red ? 6.0 + 5.0 * this.p2 : 8.0 + 6.0 * this.p2;
        double capH = red ? 4.0 + 3.0 * this.p3 : 2.0;
        int room = this.ceiling - this.y0;
        double stemH = Math.min(12.0 + 16.0 * this.p4, room - capH - 2.0);
        if (stemH < 6) {
            return null;
        }
        double stemR = 1.2 + 1.3 * this.p5;
        double top = this.y0 + stemH;
        // The stem leans gently and widens at the foot.
        double t = (y - this.y0) / stemH;
        double sx = this.x0 + 3.0 * Math.sin(this.p6 * 6.28) * t * t;
        double sz = this.z0 + 3.0 * Math.cos(this.p6 * 6.28) * t * t;
        double cx = this.x0 + 3.0 * Math.sin(this.p6 * 6.28);
        double cz = this.z0 + 3.0 * Math.cos(this.p6 * 6.28);
        double dr = Math.hypot(x - cx, z - cz);
        if (red) {
            // Domed cap: an ellipsoid shell above the stem top, with shroomlights underneath.
            double ey = (y - (top - 1.0)) / capH;
            double e = (dr / capR) * (dr / capR) + ey * ey;
            if (y >= top - 1 && e <= 1.0 && e >= 0.55) {
                return RED_CAP;
            }
            if (y == (int) Math.floor(top - 2) && dr < capR * 0.85 && dr > stemR + 0.5 && h(x, 0, z, 0xA1L) < 0.12) {
                return SHROOMLIGHT;
            }
        } else {
            // Wide flat cap with drooping rim.
            double rim = dr > capR * 0.8 ? 1 : 0;
            if (dr <= capR && (y == (int) top - (int) rim || (y == (int) top + 1 && dr < capR * 0.6))) {
                return BROWN_CAP;
            }
            if (y == (int) top - 1 && dr < capR * 0.75 && dr > stemR + 0.5 && h(x, 0, z, 0xA2L) < 0.10) {
                return SHROOMLIGHT;
            }
        }
        if (y >= this.y0 - 2 && y < top) {
            double r = stemR * (1.0 + 0.6 * Math.max(0, 1.0 - (y - this.y0) / 3.0));
            if (Math.hypot(x - sx, z - sz) <= r) {
                return STEM;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ giant crystal cluster

    private BlockState crystal(int x, int y, int z) {
        // Rare, massive, angled prisms: from the floor, hanging from the ceiling, or (wall mode)
        // jutting out of a cavern wall at mid height. Edges and tips are pearlescent froglight,
        // so the crystals read as lit from inside; the cavern around them stays dark.
        boolean wallMode = this.wall >= 0;
        boolean hanging = !wallMode && this.p6 < 0.4;
        int n = 3 + (int) (this.p1 * 4);
        double ax = this.x0 + 0.5, az = this.z0 + 0.5;
        double ay = wallMode ? this.y0 + 0.5 : (hanging ? this.ceiling + 1.0 : this.y0 - 1.0);
        double room = Math.max(8.0, this.ceiling - this.y0);
        double inward = wallMode ? Math.atan2(-WALL_DZ[this.wall], -WALL_DX[this.wall]) : 0.0;
        BlockState best = null;
        double bestScore = 0;
        for (int i = 0; i < n; i++) {
            long c = Hash.mix(this.seed, 0xC7157AL + i);
            double dx, dy, dz, len;
            double u1 = Hash.unit(c), u2 = Hash.unit(c = Hash.fmix(c)), u3 = Hash.unit(c = Hash.fmix(c));
            double rad = (i == 0 ? 3.4 : 2.0) + 2.0 * Hash.unit(Hash.fmix(c));
            if (wallMode) {
                double yaw = inward + (i == 0 ? 0.25 : 0.75) * (2.0 * u1 - 1.0);
                double pitch = Math.toRadians(-20.0 + 55.0 * u2);
                dx = Math.cos(pitch) * Math.cos(yaw);
                dz = Math.cos(pitch) * Math.sin(yaw);
                dy = Math.sin(pitch);
                len = (i == 0 ? 24.0 : 12.0) + 16.0 * u3;
            } else {
                double tilt = Math.toRadians((i == 0 ? 5 : 15) + (i == 0 ? 12 : 38) * u1);
                double yaw = 6.283 * u2;
                dx = Math.sin(tilt) * Math.cos(yaw);
                dz = Math.sin(tilt) * Math.sin(yaw);
                dy = Math.cos(tilt) * (hanging ? -1 : 1);
                len = Math.min(room * 0.9, (i == 0 ? 24.0 : 12.0) + 18.0 * u3);
            }
            // Project onto the crystal's axis.
            double px = x + 0.5 - ax, py = y + 0.5 - ay, pz = z + 0.5 - az;
            double along = px * dx + py * dy + pz * dz;
            if (along < -2 || along > len) {
                continue;
            }
            double qx = px - dx * along, qy = py - dy * along, qz = pz - dz * along;
            double perp = Math.sqrt(qx * qx + qy * qy + qz * qz);
            double t = along / len;
            double r = t < 0.7 ? rad : rad * (1.0 - (t - 0.7) / 0.3) + 0.3;   // prism, then a pointed tip
            // Hexagonal facets: the six edges stand out.
            double ang = Math.atan2(qz, qx);
            double facet = Math.cos(6 * ang);
            r *= 0.92 + 0.08 * facet;
            if (perp <= r) {
                double score = r - perp + 1;
                if (score > bestScore) {
                    bestScore = score;
                    if (t > 0.86 || (perp > r - 1.2 && facet > 0.94 && t > 0.12) || (perp < rad * 0.35 && t < 0.8)) {
                        best = CRYSTAL_CORE;                           // glowing edges, tip and core
                    } else if (along < 2.0) {
                        best = CALCITE;
                    } else {
                        best = h(x, y, z, 0xC8L) < 0.06 ? BUDDING : AMETHYST;
                    }
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------ giant root

    private BlockState root(int x, int y, int z) {
        // 1-3 roots from one point on the ceiling, arching down to the floor 12-30 blocks away.
        int n = 1 + (int) (this.p1 * 3);
        double room = this.ceiling - this.y0;
        double best = Double.MAX_VALUE;
        double bestR = 0;
        double bestT = 0;
        for (int i = 0; i < n; i++) {
            long c = Hash.mix(this.seed, 0x2007L + i);
            double yaw = 6.283 * Hash.unit(c) + i * 2.1;
            double reach = 12.0 + 18.0 * Hash.unit(c = Hash.fmix(c));
            double rad = (i == 0 ? 3.2 : 2.2) + 1.2 * Hash.unit(Hash.fmix(c));
            double ax = this.x0, ay = this.ceiling + 1.0, az = this.z0;
            double bx = ax + reach * Math.cos(yaw), by = this.y0, bz = az + reach * Math.sin(yaw);
            // Quadratic Bezier with the control point pushed outwards and down: an arching root.
            double cx = ax + reach * 0.15 * Math.cos(yaw), cy = this.y0 + room * 0.35, cz = az + reach * 0.15 * Math.sin(yaw);
            for (int k = 0; k <= 24; k++) {
                double t = k / 24.0, u = 1 - t;
                double qx = u * u * ax + 2 * u * t * cx + t * t * bx;
                double qy = u * u * ay + 2 * u * t * cy + t * t * by;
                double qz = u * u * az + 2 * u * t * cz + t * t * bz;
                double r = rad * (1.0 - 0.45 * t);
                double d = Math.sqrt((x + 0.5 - qx) * (x + 0.5 - qx) + (y + 0.5 - qy) * (y + 0.5 - qy) + (z + 0.5 - qz) * (z + 0.5 - qz));
                if (d - r < best) {
                    best = d - r;
                    bestR = r;
                    bestT = t;
                }
            }
        }
        if (best <= -1.0) {
            return ROOT_WOOD;
        }
        if (best <= 0.4) {
            return h(x, y, z, 0x2008L) < 0.04 ? SAP : ROOT_SHELL;
        }
        if (best <= 1.4 && bestT > 0.1 && bestT < 0.9 && h(x, y, z, 0x2009L) < 0.08 && bestR > 0) {
            return HANGING_ROOTS;
        }
        return null;
    }

    // ------------------------------------------------------------ infernal spire

    private BlockState spire(int x, int y, int z) {
        double room = this.ceiling - this.y0;
        boolean pillar = this.p1 < 0.35 && room < 60;           // some reach the ceiling
        double height = pillar ? room + 2 : Math.min(room - 3, 24.0 + 30.0 * this.p2);
        double base = 4.0 + 3.0 * this.p3;
        double t = (y - this.y0) / height;
        if (t < -0.1 || t > 1.0) {
            return null;
        }
        double r = pillar ? base * (0.55 + 0.45 * Math.abs(2 * t - 1)) : base * Math.pow(1.0 - Math.max(t, 0), 1.3) + 0.4;
        double ang = Math.atan2(z - this.z0, x - this.x0);
        r *= 0.85 + 0.15 * Math.sin(5 * ang + this.p4 * 6.28 + t * 3.0);
        double dist = Math.hypot(x + 0.5 - this.x0 - 0.5, z + 0.5 - this.z0 - 0.5);
        if (dist > r) {
            return null;
        }
        // Bands of blackstone and basalt with obsidian seams; crying obsidian and magma specks.
        int band = Math.floorMod((int) Math.floor((y + 3.0 * Math.sin(ang * 2)) / 5.0), 4);
        double v = h(x, y, z, 0x5713L);
        if (v < 0.04) {
            return CRYING;
        }
        if (t < 0.2 && v < 0.12) {
            return MAGMA;
        }
        return switch (band) {
            case 0 -> OBSIDIAN;
            case 1, 3 -> BLACKSTONE;
            default -> BASALT;
        };
    }
}
