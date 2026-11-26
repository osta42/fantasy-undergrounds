package com.fantasyundergrounds.devtools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.Tags;

/**
 * Cross-section PNGs of the audit region for eyeballing cave shapes: horizontal slices at fixed
 * Y levels (1 px = 1 block) and two vertical cuts through the region centre (X/Y and Z/Y).
 */
final class SliceImages {
    static final int[] LEVELS = {-110, -90, -70, -50, -30, -10, 10, 25};
    private static final int V_MIN = -128;
    private static final int V_MAX = 96;

    private final int minBlockX;
    private final int minBlockZ;
    private final int size;
    private final int cutZ;
    private final int cutX;
    private final BufferedImage[] horizontal;
    private final BufferedImage vertical;
    private final BufferedImage verticalZ;

    SliceImages(int minChunkX, int minChunkZ, int chunks) {
        this.minBlockX = minChunkX * 16;
        this.minBlockZ = minChunkZ * 16;
        this.size = chunks * 16;
        this.cutZ = this.minBlockZ + this.size / 2;
        this.cutX = this.minBlockX + this.size / 2;
        this.horizontal = new BufferedImage[LEVELS.length];
        for (int i = 0; i < LEVELS.length; i++) {
            this.horizontal[i] = new BufferedImage(this.size, this.size, BufferedImage.TYPE_INT_RGB);
        }
        this.vertical = new BufferedImage(this.size, V_MAX - V_MIN, BufferedImage.TYPE_INT_RGB);
        this.verticalZ = new BufferedImage(this.size, V_MAX - V_MIN, BufferedImage.TYPE_INT_RGB);
    }

    void record(LevelChunk chunk) {
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < LEVELS.length; i++) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int px = x0 + dx - this.minBlockX;
                    int pz = z0 + dz - this.minBlockZ;
                    if (px >= 0 && pz >= 0 && px < this.size && pz < this.size) {
                        this.horizontal[i].setRGB(px, pz, color(chunk.getBlockState(p.set(x0 + dx, LEVELS[i], z0 + dz))));
                    }
                }
            }
        }
        recordZ(chunk, p, x0, z0);
        if (this.cutZ >= z0 && this.cutZ < z0 + 16) {
            for (int dx = 0; dx < 16; dx++) {
                int px = x0 + dx - this.minBlockX;
                if (px < 0 || px >= this.size) {
                    continue;
                }
                for (int y = V_MIN; y < V_MAX; y++) {
                    this.vertical.setRGB(px, V_MAX - 1 - y, color(chunk.getBlockState(p.set(x0 + dx, y, this.cutZ))));
                }
            }
        }
    }

    private void recordZ(LevelChunk chunk, BlockPos.MutableBlockPos p, int x0, int z0) {
        if (this.cutX < x0 || this.cutX >= x0 + 16) {
            return;
        }
        for (int dz = 0; dz < 16; dz++) {
            int pz = z0 + dz - this.minBlockZ;
            if (pz < 0 || pz >= this.size) {
                continue;
            }
            for (int y = V_MIN; y < V_MAX; y++) {
                this.verticalZ.setRGB(pz, V_MAX - 1 - y, color(chunk.getBlockState(p.set(this.cutX, y, z0 + dz))));
            }
        }
    }

    void write(Path dir, String label) throws IOException {
        Files.createDirectories(dir);
        for (int i = 0; i < LEVELS.length; i++) {
            ImageIO.write(this.horizontal[i], "png", dir.resolve(label + "-y" + LEVELS[i] + ".png").toFile());
        }
        ImageIO.write(this.vertical, "png", dir.resolve(label + "-vertical.png").toFile());
        ImageIO.write(this.verticalZ, "png", dir.resolve(label + "-verticalz.png").toFile());
    }

    private static int color(BlockState s) {
        if (s.isAir()) {
            return 0x101014;
        }
        if (!s.getFluidState().isEmpty()) {
            return s.getFluidState().is(FluidTags.LAVA) ? 0xFF6A00 : 0x2D6BE0;
        }
        if (s.is(Tags.Blocks.ORES)) {
            return 0xFFD23F;
        }
        if (s.is(Blocks.DEEPSLATE) || s.is(Blocks.TUFF)) {
            return 0x55555F;
        }
        if (s.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return 0x8C8C8C;
        }
        if (s.is(Blocks.BEDROCK)) {
            return 0x222222;
        }
        if (s.is(Blocks.DRIPSTONE_BLOCK) || s.is(Blocks.POINTED_DRIPSTONE)) {
            return 0xB07A4F;
        }
        return 0x5FAF4F;
    }
}
