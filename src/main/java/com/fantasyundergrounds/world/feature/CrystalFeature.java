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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code fantasyundergrounds:crystal}: a free-standing crystal spire (Crystal Hollows). It grows
 * from a rock surface next to the origin, pointing away from that surface with a random tilt,
 * and tapers to a point. Afterwards, buds or clusters ({@code buds}) are scattered on its faces,
 * oriented outwards when the block has a {@code facing} property (amethyst buds and clusters).
 *
 * <p>Only open space (air, water, lava) is ever replaced, so it cannot overwrite ores or any
 * other block (PLAN.md 2.4). Length is at most 12 and radius at most 3 (reach under 16 blocks), so writes stay within
 * the area the generator allows around the decorated chunk.
 */
public final class CrystalFeature extends Feature<CrystalFeature.Config> {
    public record Config(BlockStateProvider core, BlockStateProvider buds, IntProvider length, IntProvider radius, float budChance)
        implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("core").forGetter(Config::core),
            BlockStateProvider.CODEC.fieldOf("buds").forGetter(Config::buds),
            net.minecraft.util.valueproviders.IntProviders.codec(3, 12).fieldOf("length").forGetter(Config::length),
            net.minecraft.util.valueproviders.IntProviders.codec(1, 3).fieldOf("radius").forGetter(Config::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("bud_chance").forGetter(Config::budChance)
        ).apply(i, Config::new));
    }

    public CrystalFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return placeCrystal(context.config(), context.level(), context.random(), context.origin());
    }

    static boolean open(BlockState s) {
        return s.isAir() || s.getBlock() == Blocks.WATER || s.getBlock() == Blocks.LAVA;
    }

    /** Shared with the purity self-test. */
    public static boolean placeCrystal(Config cfg, WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!open(level.getBlockState(origin))) {
            return false;
        }
        // Grow away from the first solid neighbour, checked in a fixed order (floor first).
        Direction surface = null;
        for (Direction d : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (!open(level.getBlockState(origin.relative(d)))) {
                surface = d;
                break;
            }
        }
        if (surface == null) {
            return false;
        }
        Direction grow = surface.getOpposite();
        double dx = grow.getStepX() + (random.nextDouble() - 0.5) * 0.9;
        double dy = grow.getStepY() + (random.nextDouble() - 0.5) * 0.9;
        double dz = grow.getStepZ() + (random.nextDouble() - 0.5) * 0.9;
        double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= n;
        dy /= n;
        dz /= n;
        int length = cfg.length().sample(random);
        int radius = cfg.radius().sample(random);
        // Start half a radius inside the rock, so the base is anchored.
        double sx = origin.getX() + 0.5 - grow.getStepX() * radius * 0.5;
        double sy = origin.getY() + 0.5 - grow.getStepY() * radius * 0.5;
        double sz = origin.getZ() + 0.5 - grow.getStepZ() * radius * 0.5;
        List<BlockPos> placed = new ArrayList<>();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int t = 0; t <= length; t++) {
            double r = radius * (1.0 - t / (double) (length + 1)) + 0.35;
            double cx = sx + dx * t, cy = sy + dy * t, cz = sz + dz * t;
            int ri = (int) Math.ceil(r);
            for (int ox = -ri; ox <= ri; ox++) {
                for (int oy = -ri; oy <= ri; oy++) {
                    for (int oz = -ri; oz <= ri; oz++) {
                        q.set((int) Math.floor(cx) + ox, (int) Math.floor(cy) + oy, (int) Math.floor(cz) + oz);
                        double ex = q.getX() + 0.5 - cx, ey = q.getY() + 0.5 - cy, ez = q.getZ() + 0.5 - cz;
                        if (ex * ex + ey * ey + ez * ez > r * r) {
                            continue;
                        }
                        if (open(level.getBlockState(q))) {
                            level.setBlock(q, cfg.core().getState(level, random, q), Block.UPDATE_CLIENTS);
                            placed.add(q.immutable());
                        }
                    }
                }
            }
        }
        // Buds and clusters on the faces, pointing outwards.
        for (BlockPos p : placed) {
            for (Direction d : Direction.values()) {
                if (random.nextFloat() >= cfg.budChance()) {
                    continue;
                }
                BlockPos b = p.relative(d);
                BlockState there = level.getBlockState(b);
                if (!there.isAir() && there.getBlock() != Blocks.WATER) {
                    continue;
                }
                BlockState bud = cfg.buds().getState(level, random, b);
                if (bud.hasProperty(BlockStateProperties.FACING)) {
                    bud = bud.setValue(BlockStateProperties.FACING, d);
                }
                if (bud.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    bud = bud.setValue(BlockStateProperties.WATERLOGGED, there.getBlock() == Blocks.WATER);
                }
                level.setBlock(b, bud, Block.UPDATE_CLIENTS);
            }
        }
        return !placed.isEmpty();
    }
}
