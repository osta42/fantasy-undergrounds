package com.fantasyundergrounds.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code fantasyundergrounds:pillar}: a free-standing formation in open air (PLAN.md 2.4).
 * Placed on a floor; if a ceiling is found within {@code max_height} it becomes a full
 * hourglass column, otherwise a tapering spire. It only ever replaces air, water or lava, never
 * a solid block, so it cannot overwrite ores. Radius is limited to 4 so writes stay inside the
 * area the generator allows.
 */
public final class PillarFeature extends Feature<PillarFeature.Config> {
    public record Config(BlockStateProvider state, IntProvider radius, int maxHeight) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("state").forGetter(Config::state),
            net.minecraft.util.valueproviders.IntProviders.codec(1, 4).fieldOf("radius").forGetter(Config::radius),
            Codec.intRange(4, 64).fieldOf("max_height").forGetter(Config::maxHeight)
        ).apply(i, Config::new));
    }

    public PillarFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return placePillar(context.config(), context.level(), context.random(), context.origin());
    }

    private static boolean open(BlockState s) {
        return s.isAir() || (s.getBlock() == net.minecraft.world.level.block.Blocks.WATER)
            || (s.getBlock() == net.minecraft.world.level.block.Blocks.LAVA);
    }

    /** Shared with the purity self-test. */
    public static boolean placePillar(Config cfg, WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!open(level.getBlockState(origin)) || open(level.getBlockState(origin.below()))) {
            return false;
        }
        int height = 0;
        BlockPos.MutableBlockPos p = origin.mutable();
        while (height < cfg.maxHeight() && open(level.getBlockState(p))) {
            p.move(0, 1, 0);
            height++;
        }
        boolean column = height < cfg.maxHeight() && height >= 3;
        if (!column) {
            height = 3 + random.nextInt(Math.max(1, cfg.maxHeight() / 3));
        }
        int r = cfg.radius().sample(random);
        boolean placed = false;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < height; dy++) {
            double t = height <= 1 ? 0 : dy / (double) (height - 1);
            double rad = column ? r * (0.6 + 0.4 * (2 * t - 1) * (2 * t - 1)) : r * (1.0 - t) + 0.35;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double jitter = random.nextFloat() * 0.6;
                    if (dx * dx + dz * dz > (rad + jitter) * (rad + jitter)) {
                        continue;
                    }
                    q.setWithOffset(origin, dx, dy, dz);
                    if (open(level.getBlockState(q))) {
                        level.setBlock(q, cfg.state().getState(level, random, q), Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }
}
