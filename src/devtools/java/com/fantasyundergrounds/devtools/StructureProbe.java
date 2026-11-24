package com.fantasyundergrounds.devtools;

import com.mojang.datafixers.util.Pair;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Gate 6: locate each required structure from the region centre, generate all chunks its
 * pieces cover, and count the structure's own blocks inside its piece bounding boxes
 * (everything except air, fluids and base stone). Comparing these counts against the vanilla
 * run on the same seed tells us whether caverns cut into the structure.
 */
final class StructureProbe {
    static final String[] REQUIRED = {"minecraft:ancient_city", "minecraft:trial_chambers", "minecraft:stronghold", "minecraft:mineshaft"};
    /** Probed when registered (this mod's own structures); reported, not compared against vanilla. */
    static final String[] OPTIONAL = {"fantasyundergrounds:ancient_ruins"};

    private StructureProbe() {}

    static Map<String, Object> run(ServerLevel level, BlockPos origin, int searchRadius) {
        Map<String, Object> out = new LinkedHashMap<>();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        java.util.List<String> names = new java.util.ArrayList<>(java.util.List.of(REQUIRED));
        for (String opt : OPTIONAL) {
            if (registry.get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(opt))).isPresent()) {
                names.add(opt);
            }
        }
        for (String name : names) {
            Map<String, Object> entry = new LinkedHashMap<>();
            out.put(name, entry);
            try {
                Holder<Structure> holder = registry.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(name)));
                long t0 = System.nanoTime();
                Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(holder), origin, searchRadius, false);
                entry.put("locateMs", (System.nanoTime() - t0) / 1_000_000.0);
                if (found == null) {
                    entry.put("found", false);
                    continue;
                }
                BlockPos pos = found.getFirst();
                entry.put("found", true);
                entry.put("pos", pos.getX() + "," + pos.getY() + "," + pos.getZ());
                ChunkAccess startChunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS, true);
                StructureStart start = startChunk.getStartForStructure(holder.value());
                if (start == null || !start.isValid()) {
                    entry.put("startValid", false);
                    continue;
                }
                entry.put("startValid", true);
                BoundingBox box = start.getBoundingBox();
                entry.put("boundingBox", box.toString());
                entry.put("pieces", start.getPieces().size());
                ChunkGen.generate(level.getChunkSource(),
                    SectionPosHelper.chunk(box.minX()), SectionPosHelper.chunk(box.minZ()),
                    SectionPosHelper.chunk(box.maxX()) + 1, SectionPosHelper.chunk(box.maxZ()) + 1);
                Map<String, Long> blocks = new TreeMap<>();
                long total = 0;
                // Count each block position once even if piece boxes overlap.
                it.unimi.dsi.fastutil.longs.LongOpenHashSet seen = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
                for (StructurePiece piece : start.getPieces()) {
                    BoundingBox pb = piece.getBoundingBox();
                    for (BlockPos p : BlockPos.betweenClosed(pb.minX(), pb.minY(), pb.minZ(), pb.maxX(), pb.maxY(), pb.maxZ())) {
                        if (!seen.add(p.asLong())) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p);
                        if (s.isAir() || !s.getFluidState().isEmpty() || s.is(BlockTags.BASE_STONE_OVERWORLD)) {
                            continue;
                        }
                        total++;
                        blocks.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1L, Long::sum);
                    }
                }
                entry.put("structureBlockTotal", total);
                entry.put("structureBlocks", blocks);
            } catch (RuntimeException e) {
                FuDevTools.LOGGER.error("[fu-audit] structure probe failed for {}", name, e);
                entry.put("error", e.toString());
            }
        }
        return out;
    }

    private static final class SectionPosHelper {
        static int chunk(int block) {
            return net.minecraft.core.SectionPos.blockToSectionCoord(block);
        }
    }
}
