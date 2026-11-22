package com.fantasyundergrounds.devtools;

import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.common.Tags;

/**
 * Accumulates per-block statistics over FULL chunks. Not thread-safe; used on the server thread.
 */
final class ChunkScanner {
    /** Upper Y bound (inclusive) of the dark-spawnable-floor proxy scan. */
    private static final int SPAWN_SCAN_MAX_Y = 40;

    final Reference2LongOpenHashMap<Block> histogram = new Reference2LongOpenHashMap<>();
    final Map<String, String> chunkHashes = new TreeMap<>();
    long chunks;
    long airBelowZero;
    long blocksBelowZero;
    long waterBelowZero;
    long lavaBelowZero;
    long darkSpawnableFloor;
    long scheduledFluidTicks;
    long scheduledBlockTicks;

    void scan(ServerLevel level, LevelChunk chunk) {
        this.chunks++;
        long h = 0xcbf29ce484222325L; // FNV-1a 64
        LevelChunkSection[] sections = chunk.getSections();
        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(si));
            boolean belowZeroSection = baseY + 15 < 0;
            if (section.hasOnlyAir()) {
                // Hash an all-air section by a fixed marker so the hash stays position-aware.
                h = (h ^ (0x41524CL + si)) * 0x100000001b3L;
                this.histogram.addTo(net.minecraft.world.level.block.Blocks.AIR, 4096);
                if (belowZeroSection) {
                    this.airBelowZero += 4096;
                    this.blocksBelowZero += 4096;
                }
                continue;
            }
            for (int y = 0; y < 16; y++) {
                boolean below0 = baseY + y < 0;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        h = (h ^ Block.getId(state)) * 0x100000001b3L;
                        this.histogram.addTo(state.getBlock(), 1);
                        if (below0) {
                            this.blocksBelowZero++;
                            if (state.isAir()) {
                                this.airBelowZero++;
                            } else if (state.getFluidState().isSource()) {
                                if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                                    this.waterBelowZero++;
                                } else if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                                    this.lavaBelowZero++;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.chunkHashes.put(chunk.getPos().x() + "," + chunk.getPos().z(), Long.toHexString(h));
        this.scheduledFluidTicks += chunk.getFluidTicks().count();
        this.scheduledBlockTicks += chunk.getBlockTicks().count();
        this.darkSpawnableFloor += countDarkSpawnable(level, chunk);
    }

    /**
     * Proxy for hostile-mob pressure: number of positions at or below y=40 where a zombie-sized
     * mob could spawn in total darkness (2 air blocks over a spawnable floor, block and sky light 0).
     */
    private static long countDarkSpawnable(ServerLevel level, LevelChunk chunk) {
        long count = 0;
        int minY = chunk.getMinY() + 1;
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockState prev = chunk.getBlockState(pos.set(x0 + dx, minY - 1, z0 + dz));
                BlockState cur = chunk.getBlockState(pos.set(x0 + dx, minY, z0 + dz));
                for (int y = minY; y <= SPAWN_SCAN_MAX_Y; y++) {
                    BlockState above = chunk.getBlockState(pos.set(x0 + dx, y + 1, z0 + dz));
                    if (cur.isAir() && above.isAir() && !prev.isAir()
                        && prev.isValidSpawn(level, below.set(x0 + dx, y - 1, z0 + dz), EntityTypes.ZOMBIE)) {
                        pos.set(x0 + dx, y, z0 + dz);
                        if (level.getBrightness(LightLayer.BLOCK, pos) == 0 && level.getBrightness(LightLayer.SKY, pos) == 0) {
                            count++;
                        }
                    }
                    prev = cur;
                    cur = above;
                }
            }
        }
        return count;
    }

    Map<String, Long> histogramById() {
        Map<String, Long> out = new TreeMap<>();
        this.histogram.reference2LongEntrySet().forEach(e ->
            out.put(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(), e.getLongValue()));
        return out;
    }

    Map<String, Long> oresById() {
        Map<String, Long> out = new TreeMap<>();
        this.histogram.reference2LongEntrySet().forEach(e -> {
            if (e.getKey().defaultBlockState().is(Tags.Blocks.ORES)) {
                out.put(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(), e.getLongValue());
            }
        });
        return out;
    }
}
