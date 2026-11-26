package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.world.Hash;
import com.fantasyundergrounds.world.feature.SkinFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The single piece of an Ancient Depths ruin, in one of three layouts:
 * <ol start="0">
 *   <li>colonnade hall: tiled floor, two rows of 3x3 columns (some fallen), architraves, a dais with a chest</li>
 *   <li>sunken crypt: crumbling walls with doorways, sarcophagi, a partly fallen ceiling, hanging lanterns</li>
 *   <li>stepped temple: a ruined ziggurat with a hidden chamber, a tunnel to it and lanterns on the plaza</li>
 *   <li>collapsed corridor (any length): walls, arches, a partly fallen roof, rubble, hanging lanterns</li>
 *   <li>broken bridge (any length): a deck with gaps on piers over a carved chasm, crumbling parapets</li>
 * </ol>
 * Layouts 0-2 are rooms; 3 and 4 connect the rooms of a ruin complex ({@link AncientRuinsStructure}).
 *
 * <p>Vanilla blocks only. Every decision is a hash of (piece seed, local position), never a
 * sequential random draw, so the result does not depend on the order in which chunks are
 * decorated. Blocks are only ever placed into air or base stone
 * ({@code #fantasyundergrounds:replaceable_base}): ores, fluids, other structures and other mods'
 * blocks are never overwritten. Interior space is carved (base stone -> air) so the ruin is open
 * even where the beardifier leaves rock.
 */
public final class AncientRuinsPiece extends StructurePiece {
    public static final int VARIANTS = 3;
    public static final int CORRIDOR = 3;
    public static final int BRIDGE = 4;
    /** Depth of the chasm under a bridge deck. */
    public static final int CHASM = 12;
    private static final int[][] SIZES = {{31, 14, 23}, {21, 9, 21}, {23, 13, 23}};

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState CRACKED_TILES = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_BRICKS = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState COBBLED = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState BRICK_SLAB = Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState TILE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
    private static final BlockState HANGING_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    private static final BlockState CANDLE = Blocks.CANDLE.defaultBlockState();
    private static final BlockState COBWEB = Blocks.COBWEB.defaultBlockState();

    private final long seed;
    private final int variant;

    public AncientRuinsPiece(long seed, int variant, BoundingBox box) {
        super(FuRegistries.ANCIENT_RUINS_PIECE.get(), 0, box);
        this.seed = seed;
        this.variant = variant;
        this.setOrientation(null); // world coordinates
    }

    public AncientRuinsPiece(CompoundTag tag) {
        super(FuRegistries.ANCIENT_RUINS_PIECE.get(), tag);
        this.seed = tag.getLongOr("Seed", 0L);
        this.variant = tag.getIntOr("Variant", 0);
    }

    /** {width (x), height (y), length (z)} of a layout. */
    public static int[] size(int variant) {
        return switch (variant) {
            case CORRIDOR -> new int[] {16, 8, 7};
            case BRIDGE -> new int[] {16, 8 + CHASM, 9};
            default -> SIZES[Math.floorMod(variant, VARIANTS)].clone();
        };
    }

    /** Dev self-test: block census of one layout, straight from the layout functions (no level). */
    public static java.util.Map<String, Integer> census(int variant, long seed) {
        int[] sz = size(variant);
        AncientRuinsPiece p = new AncientRuinsPiece(seed, variant, new BoundingBox(0, 0, 0, sz[0] - 1, sz[1] - 1, sz[2] - 1));
        java.util.Map<String, Integer> m = new java.util.TreeMap<>();
        for (int x = 0; x < sz[0]; x++) {
            for (int y = 0; y < sz[1]; y++) {
                for (int z = 0; z < sz[2]; z++) {
                    BlockState st = switch (p.variant) {
                        case 1 -> p.crypt(x, y, z, sz[0], sz[1], sz[2]);
                        case 2 -> p.temple(x, y, z, sz[0], sz[1], sz[2]);
                        case CORRIDOR -> p.corridor(x, y, z, sz[0], sz[1], sz[2]);
                        case BRIDGE -> p.bridge(x, y, z, sz[0], sz[1], sz[2]);
                        default -> p.hall(x, y, z, sz[0], sz[1], sz[2]);
                    };
                    if (st != null && !st.isAir()) {
                        m.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath(), 1, Integer::sum);
                    }
                }
            }
        }
        return m;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", this.seed);
        tag.putInt("Variant", this.variant);
    }

    private double r(int lx, int ly, int lz, long salt) {
        return Hash.unit(Hash.cell(this.seed, salt, lx, ly, lz));
    }

    /** Only air and base stone may be replaced (brief: themed blocks replace base stone or air). */
    @Override
    protected boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox chunkBB) {
        BlockState s = level.getBlockState(this.getWorldPos(x, y, z));
        return s.isAir() || s.is(SkinFeature.REPLACEABLE_BASE);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        BoundingBox bb = this.boundingBox;
        int w = bb.getXSpan(), h = bb.getYSpan(), l = bb.getZSpan();
        int minX = Math.max(bb.minX(), chunkBB.minX()), maxX = Math.min(bb.maxX(), chunkBB.maxX());
        int minZ = Math.max(bb.minZ(), chunkBB.minZ()), maxZ = Math.min(bb.maxZ(), chunkBB.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = bb.minY(); y <= bb.maxY(); y++) {
                    int lx = x - bb.minX(), ly = y - bb.minY(), lz = z - bb.minZ();
                    BlockState s = switch (this.variant) {
                        case 1 -> crypt(lx, ly, lz, w, h, l);
                        case 2 -> temple(lx, ly, lz, w, h, l);
                        case CORRIDOR -> corridor(lx, ly, lz, w, h, l);
                        case BRIDGE -> bridge(lx, ly, lz, w, h, l);
                        default -> hall(lx, ly, lz, w, h, l);
                    };
                    if (s != null) {
                        this.placeBlock(level, s, x, y, z, chunkBB);
                    }
                }
            }
        }
        // Loot: vanilla tables only.
        switch (this.variant) {
            case CORRIDOR, BRIDGE -> { }
            case 1 -> {
                chest(level, chunkBB, random, w / 2, 1, l / 2, BuiltInLootTables.STRONGHOLD_CORRIDOR);
                chest(level, chunkBB, random, 2, 1, 2, BuiltInLootTables.SIMPLE_DUNGEON);
            }
            case 2 -> chest(level, chunkBB, random, w / 2, 1, l / 2, BuiltInLootTables.STRONGHOLD_CORRIDOR);
            default -> {
                chest(level, chunkBB, random, w / 2, 2, l / 2, BuiltInLootTables.STRONGHOLD_CORRIDOR);
                chest(level, chunkBB, random, 1, 1, l / 2, BuiltInLootTables.SIMPLE_DUNGEON);
            }
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, int lx, int ly, int lz, ResourceKey<LootTable> table) {
        BlockPos pos = new BlockPos(this.boundingBox.minX() + lx, this.boundingBox.minY() + ly, this.boundingBox.minZ() + lz);
        if (!chunkBB.isInside(pos) || !this.canBeReplaced(level, pos.getX(), pos.getY(), pos.getZ(), chunkBB)) {
            return;
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RandomSource.create(this.seed ^ pos.asLong()));
        this.createChest(level, chunkBB, random, pos, table, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
    }

    private BlockState floorTile(int lx, int ly, int lz) {
        double v = r(lx, ly, lz, 0xF1L);
        return v < 0.06 ? COBBLED : (v < 0.3 ? CRACKED_TILES : TILES);
    }

    private BlockState brick(int lx, int ly, int lz) {
        return r(lx, ly, lz, 0xB1L) < 0.3 ? CRACKED_BRICKS : BRICKS;
    }

    // ---------------------------------------------------------------- colonnade hall

    private boolean columnIntact(int cx, int cz) {
        return r(cx, 0, cz, 0xC2L) < 0.45;
    }

    private int columnHeight(int cx, int cz, int h) {
        return columnIntact(cx, cz) ? h - 3 : 2 + (int) (r(cx, 0, cz, 0xC3L) * (h - 6));
    }

    private BlockState hall(int lx, int ly, int lz, int w, int h, int l) {
        if (ly == 0) {
            if (lx == 0 || lx == w - 1 || lz == 0 || lz == l - 1) {
                return POLISHED;
            }
            if (lz == l / 2 && lx % 4 == 1) {
                return CHISELED;
            }
            return floorTile(lx, ly, lz);
        }
        // Columns: rows at z = 3 and z = l - 4, every 6 blocks along x.
        int[] rows = {3, l - 4};
        for (int cz : rows) {
            int cx = 3 + 6 * Math.round((lx - 3) / 6.0f);
            if (cx > w - 4 || Math.abs(lx - cx) > 1 || Math.abs(lz - cz) > 1) {
                continue;
            }
            int top = columnHeight(cx, cz, h);
            boolean corner = Math.abs(lx - cx) == 1 && Math.abs(lz - cz) == 1;
            if (ly <= top) {
                if (corner) {
                    return POLISHED;
                }
                return ly == 1 || (ly == top && columnIntact(cx, cz)) ? CHISELED : brick(lx, ly, lz);
            }
            if (ly == top + 1 && !columnIntact(cx, cz) && lx == cx && lz == cz && r(cx, 1, cz, 0xC4L) < 0.45) {
                return LANTERN;
            }
        }
        // Architraves between neighbouring intact columns.
        if (ly == h - 2 && (lz == 3 || lz == l - 4) && lx >= 2 && lx <= w - 3) {
            int left = 3 + 6 * Math.floorDiv(lx - 3, 6);
            int right = left + 6;
            boolean both = left >= 3 && right <= w - 4 && columnIntact(left, lz) && columnIntact(right, lz);
            if (both && r(lx, ly, lz, 0xA1L) < 0.85) {
                return brick(lx, ly, lz);
            }
        }
        // Dais in the middle with candles.
        int dx = lx - w / 2, dz = lz - l / 2;
        if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
            if (ly == 1) {
                return dx == 0 && dz == 0 ? CHISELED : POLISHED;
            }
            if (ly == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                return CANDLE;
            }
        }
        // Rubble and cobwebs.
        if (ly == 1) {
            double v = r(lx, ly, lz, 0xD1L);
            if (v < 0.04) {
                return COBBLED;
            }
            if (v < 0.07) {
                return BRICK_SLAB;
            }
        }
        if (ly >= h - 4 && r(lx, ly, lz, 0xE1L) < 0.012) {
            return COBWEB;
        }
        return ly < h - 1 ? AIR : null;
    }

    // ---------------------------------------------------------------- collapsed corridor

    private BlockState corridor(int lx, int ly, int lz, int w, int h, int l) {
        // Runs along the longer horizontal axis; a = along, b = across, wb = width across.
        boolean alongX = w >= l;
        int a = alongX ? lx : lz, b = alongX ? lz : lx, wb = alongX ? l : w;
        boolean side = b == 0 || b == wb - 1;
        boolean collapsed = r(a / 4, 0, 0, 0xF2L) < 0.25;     // 4-block segments that fell in
        if (ly == 0) {
            return side ? POLISHED : floorTile(lx, ly, lz);
        }
        if (collapsed) {
            // Rubble mound, open to the rock above.
            if (ly <= 2 && r(lx, ly, lz, 0xF3L) < 0.55 - 0.2 * ly) {
                return r(lx, ly, lz, 0xF4L) < 0.5 ? COBBLED : CRACKED_BRICKS;
            }
            return ly < h - 1 ? AIR : null;
        }
        boolean arch = a % 6 == 0;
        if (side && ly <= h - 2) {
            return arch ? POLISHED : (r(lx, ly, lz, 0xF5L) < 0.85 ? brick(lx, ly, lz) : AIR);
        }
        if (ly == h - 2 && arch) {
            return brick(lx, ly, lz);                          // lintel of the arch
        }
        if (ly == h - 1) {
            return r(a / 2, 0, b / 2, 0xF6L) < 0.6 ? TILES : AIR;
        }
        if (ly == h - 3 && a % 8 == 4 && b == wb / 2) {
            return HANGING_LANTERN;
        }
        if (ly == 1 && r(lx, ly, lz, 0xF7L) < 0.04) {
            return COBBLED;
        }
        if (ly >= h - 3 && side && r(lx, ly, lz, 0xF8L) < 0.05) {
            return COBWEB;
        }
        return AIR;
    }

    // ---------------------------------------------------------------- broken bridge

    private BlockState bridge(int lx, int ly, int lz, int w, int h, int l) {
        boolean alongX = w >= l;
        int a = alongX ? lx : lz, b = alongX ? lz : lx, la = alongX ? w : l, wb = alongX ? l : w;
        int deck = CHASM;                                      // deck level, above the chasm
        boolean pier = (a < 2 || a >= la - 2 || a % 8 == 0) && b >= 2 && b <= wb - 3;
        boolean gap = r(a / 3, 0, 0, 0xB2L) < 0.22 && a > 3 && a < la - 4;   // fallen spans
        if (ly < deck) {
            return pier && !gap ? (ly % 4 == 0 ? POLISHED : brick(lx, ly, lz)) : AIR;   // piers, open chasm
        }
        if (ly == deck) {
            if (gap) {
                return AIR;
            }
            return b == 0 || b == wb - 1 ? POLISHED : floorTile(lx, ly, lz);
        }
        if (ly == deck + 1 && (b == 0 || b == wb - 1) && !gap && r(lx, ly, lz, 0xB3L) < 0.7) {
            return BRICK_SLAB;                                // crumbling parapet
        }
        if (ly == deck + 1 && b == 0 && a % 8 == 4 && !gap) {
            return LANTERN;
        }
        return ly < h - 1 ? AIR : null;
    }

    // ---------------------------------------------------------------- sunken crypt

    private BlockState crypt(int lx, int ly, int lz, int w, int h, int l) {
        boolean edge = lx == 0 || lx == w - 1 || lz == 0 || lz == l - 1;
        if (ly == 0) {
            return edge ? POLISHED : floorTile(lx, ly, lz);
        }
        if (edge) {
            boolean doorway = ly <= 3 && (Math.abs(lx - w / 2) <= 1 || Math.abs(lz - l / 2) <= 1);
            if (doorway) {
                return AIR;
            }
            if (ly <= h - 2) {
                boolean pilaster = (lx % 5 == 0) && (lz % 5 == 0);
                if (pilaster) {
                    return POLISHED;
                }
                return r(lx, ly, lz, 0xD2L) < 0.88 - 0.07 * ly ? brick(lx, ly, lz) : AIR;
            }
            return null;
        }
        if (ly == h - 1) {
            return r(lx / 2, 0, lz / 2, 0xD3L) < 0.45 ? TILES : AIR;
        }
        // Sarcophagi: two rows of three, each 2 x 3.
        for (int sz : new int[] {6, l - 7}) {
            for (int sx : new int[] {5, w / 2, w - 6}) {
                if (lx >= sx - 1 && lx <= sx && Math.abs(lz - sz) <= 1) {
                    if (ly == 1) {
                        return POLISHED;
                    }
                    if (ly == 2) {
                        return r(sx, 2, sz, 0xD4L) < 0.7 ? TILE_SLAB : AIR;
                    }
                }
            }
        }
        if (ly == h - 2 && lx % 5 == 0 && lz % 5 == 0 && r(lx / 2, 0, lz / 2, 0xD3L) < 0.45) {
            return HANGING_LANTERN;
        }
        if (ly == 1 && r(lx, ly, lz, 0xD5L) < 0.05) {
            return COBBLED;
        }
        boolean nearCorner = (lx <= 2 || lx >= w - 3) && (lz <= 2 || lz >= l - 3);
        if (nearCorner && ly >= h - 3 && r(lx, ly, lz, 0xD6L) < 0.35) {
            return COBWEB;
        }
        return AIR;
    }

    // ---------------------------------------------------------------- stepped temple

    private BlockState temple(int lx, int ly, int lz, int w, int h, int l) {
        if (ly == 0) {
            return floorTile(lx, ly, lz);
        }
        int cx = w / 2, cz = l / 2;
        // Hidden chamber and the tunnel leading to it from the south.
        boolean chamber = Math.abs(lx - cx) <= 1 && Math.abs(lz - cz) <= 1 && ly <= 3;
        boolean tunnel = lx == cx && ly <= 2 && lz < cz;
        int k = (ly - 1) / 2;
        int inset = 2 * k + 1;
        boolean inside = lx >= inset && lx <= w - 1 - inset && lz >= inset && lz <= l - 1 - inset;
        if (inside && !chamber && !tunnel) {
            // Collapse grows with height and from one eroded corner.
            double erosion = 0.06 * k + 0.25 * Math.max(0, 1.0 - Math.hypot(lx - w * 0.85, lz - l * 0.15) / 9.0);
            if (r(lx, ly, lz, 0xE2L) < erosion) {
                return AIR;
            }
            boolean rim = lx == inset || lx == w - 1 - inset || lz == inset || lz == l - 1 - inset;
            boolean topLevel = k == (h - 2) / 2;
            if (topLevel && lx == cx && lz == cz) {
                return ly % 2 == 1 ? CHISELED : LANTERN;
            }
            return rim && ly % 2 == 0 ? POLISHED : brick(lx, ly, lz);
        }
        if (chamber || tunnel) {
            return AIR;
        }
        // Plaza: lanterns at the outer corners (outside the first step), rubble around the base.
        boolean plazaCorner = (lx == 0 || lx == w - 1) && (lz == 0 || lz == l - 1);
        if (ly == 1 && plazaCorner) {
            return LANTERN;
        }
        if (ly == 1 && r(lx, ly, lz, 0xE3L) < 0.08) {
            return COBBLED;
        }
        return ly < h - 1 ? AIR : null;
    }
}
