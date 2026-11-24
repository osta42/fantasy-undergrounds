package com.fantasyundergrounds.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code fantasyundergrounds:skin}: coats exposed rock with a themed block, at most
 * {@code depth} (1-2) blocks deep (PLAN.md 2.4). Only blocks in
 * {@code #fantasyundergrounds:replaceable_base} (stone, deepslate, tuff, granite, diorite,
 * andesite) are ever replaced, so ores from any mod are never overwritten: they are not in
 * that tag. Patches fade out irregularly towards the edge of the radius.
 */
public final class SkinFeature extends Feature<SkinFeature.Config> {
    public static final TagKey<Block> REPLACEABLE_BASE =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("fantasyundergrounds", "replaceable_base"));

    public enum Exposure implements StringRepresentable {
        AIR("air"), WATER("water"), LAVA("lava");

        public static final Codec<Exposure> CODEC = StringRepresentable.fromEnum(Exposure::values);
        private final String name;

        Exposure(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        boolean matches(BlockState s) {
            return switch (this) {
                case AIR -> s.isAir();
                case WATER -> s.getFluidState().is(FluidTags.WATER);
                case LAVA -> s.getFluidState().is(FluidTags.LAVA);
            };
        }
    }

    public enum Surface implements StringRepresentable {
        ANY("any"), FLOOR("floor"), CEILING("ceiling"), WALL("wall");

        public static final Codec<Surface> CODEC = StringRepresentable.fromEnum(Surface::values);
        private final String name;

        Surface(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        /** Directions from the rock block towards the open space. */
        Direction[] directions() {
            return switch (this) {
                case ANY -> Direction.values();
                case FLOOR -> new Direction[] {Direction.UP};
                case CEILING -> new Direction[] {Direction.DOWN};
                case WALL -> new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            };
        }
    }

    public record Config(BlockStateProvider state, int radius, int depth, float chance, Exposure exposure, Surface surface)
        implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("state").forGetter(Config::state),
            Codec.intRange(1, 7).fieldOf("radius").forGetter(Config::radius),
            Codec.intRange(1, 2).fieldOf("depth").forGetter(Config::depth),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Config::chance),
            Exposure.CODEC.optionalFieldOf("exposure", Exposure.AIR).forGetter(Config::exposure),
            Surface.CODEC.optionalFieldOf("surface", Surface.ANY).forGetter(Config::surface)
        ).apply(i, Config::new));
    }

    public SkinFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return placeSkin(context.config(), context.level(), context.random(), context.origin());
    }

    /** Shared with the purity self-test. */
    public static boolean placeSkin(Config cfg, WorldGenLevel level, RandomSource random, BlockPos origin) {
        int r = cfg.radius();
        Direction[] dirs = cfg.surface().directions();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r * r) {
                        continue;
                    }
                    // One random draw per candidate, in fixed order: deterministic for a given seed.
                    float roll = random.nextFloat();
                    double fade = 1.0 - Math.sqrt(d2) / (r + 1.0);
                    if (roll > cfg.chance() * fade * 1.4) {
                        continue;
                    }
                    pos.setWithOffset(origin, dx, dy, dz);
                    if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMaxY()) {
                        continue;
                    }
                    if (!level.getBlockState(pos).is(REPLACEABLE_BASE)) {
                        continue;
                    }
                    if (exposed(level, pos, probe, dirs, cfg)) {
                        level.setBlock(pos, cfg.state().getState(level, random, pos), Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    private static boolean exposed(WorldGenLevel level, BlockPos pos, BlockPos.MutableBlockPos probe, Direction[] dirs, Config cfg) {
        for (Direction d : dirs) {
            BlockState first = level.getBlockState(probe.setWithOffset(pos, d));
            if (cfg.exposure().matches(first)) {
                return true;
            }
            if (cfg.depth() >= 2 && first.is(REPLACEABLE_BASE)) {
                BlockState second = level.getBlockState(probe.set(pos.getX() + 2 * d.getStepX(), pos.getY() + 2 * d.getStepY(), pos.getZ() + 2 * d.getStepZ()));
                if (cfg.exposure().matches(second)) {
                    return true;
                }
            }
        }
        return false;
    }
}
