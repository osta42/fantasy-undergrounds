package com.fantasyundergrounds.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code fantasyundergrounds:root}: a giant root (Ancient Root Systems). It starts at a ceiling
 * just above the origin and winds downwards, thinning as it goes, until it reaches the floor or
 * its length runs out. Each step is a small sphere: a {@code core} (wood) wrapped in a
 * {@code shell} (roots); {@code hanging} blocks (hanging roots) dangle under the core.
 *
 * <p>Only open space (air, water, lava) is ever replaced, so it cannot overwrite ores or any
 * other block (PLAN.md 2.4). Total reach stays under 16 blocks horizontally from the origin.
 */
public final class RootFeature extends Feature<RootFeature.Config> {
    public record Config(BlockStateProvider core, BlockStateProvider shell, BlockStateProvider hanging, IntProvider length,
                         IntProvider radius, float hangingChance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("core").forGetter(Config::core),
            BlockStateProvider.CODEC.fieldOf("shell").forGetter(Config::shell),
            BlockStateProvider.CODEC.fieldOf("hanging").forGetter(Config::hanging),
            net.minecraft.util.valueproviders.IntProviders.codec(4, 40).fieldOf("length").forGetter(Config::length),
            net.minecraft.util.valueproviders.IntProviders.codec(1, 3).fieldOf("radius").forGetter(Config::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("hanging_chance").forGetter(Config::hangingChance)
        ).apply(i, Config::new));
    }

    public RootFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return placeRoot(context.config(), context.level(), context.random(), context.origin());
    }

    /** Shared with the purity self-test. */
    public static boolean placeRoot(Config cfg, WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!CrystalFeature.open(level.getBlockState(origin)) || CrystalFeature.open(level.getBlockState(origin.above()))) {
            return false;
        }
        int length = cfg.length().sample(random);
        double radius = cfg.radius().sample(random) + 0.5;
        double x = origin.getX() + 0.5, y = origin.getY() + 1.0, z = origin.getZ() + 0.5;
        double dx = random.nextDouble() - 0.5, dz = random.nextDouble() - 0.5;
        List<BlockPos> core = new ArrayList<>();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int i = 0; i < length; i++) {
            double r = radius * (1.0 - 0.6 * i / (double) length);
            int ri = (int) Math.ceil(r + 1);
            for (int ox = -ri; ox <= ri; ox++) {
                for (int oy = -ri; oy <= ri; oy++) {
                    for (int oz = -ri; oz <= ri; oz++) {
                        q.set((int) Math.floor(x) + ox, (int) Math.floor(y) + oy, (int) Math.floor(z) + oz);
                        double ex = q.getX() + 0.5 - x, ey = q.getY() + 0.5 - y, ez = q.getZ() + 0.5 - z;
                        double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                        if (d > r + 0.6 || !CrystalFeature.open(level.getBlockState(q))) {
                            continue;
                        }
                        boolean isCore = d <= r - 0.4;
                        level.setBlock(q, (isCore ? cfg.core() : cfg.shell()).getState(level, random, q), Block.UPDATE_CLIENTS);
                        core.add(q.immutable());
                        placed = true;
                    }
                }
            }
            // Wind downwards: mostly down, drifting sideways; keep within 12 blocks of the origin.
            dx = Math.max(-0.7, Math.min(0.7, dx + (random.nextDouble() - 0.5) * 0.5));
            dz = Math.max(-0.7, Math.min(0.7, dz + (random.nextDouble() - 0.5) * 0.5));
            double nx = x + dx, nz = z + dz;
            if (Math.abs(nx - origin.getX()) < 12 && Math.abs(nz - origin.getZ()) < 12) {
                x = nx;
                z = nz;
            }
            y -= 0.8;
            if (!CrystalFeature.open(level.getBlockState(q.set((int) Math.floor(x), (int) Math.floor(y - r - 1), (int) Math.floor(z))))) {
                break; // reached the floor
            }
        }
        // Hanging roots under the root (wood and mangrove roots are full cubes, so they stay attached).
        for (BlockPos p : core) {
            BlockPos below = p.below();
            if (level.getBlockState(below).isAir() && random.nextFloat() < cfg.hangingChance()) {
                level.setBlock(below, cfg.hanging().getState(level, random, below), Block.UPDATE_CLIENTS);
            }
        }
        return placed;
    }
}
