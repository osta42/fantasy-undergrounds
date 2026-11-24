package com.fantasyundergrounds.world.feature;

import com.fantasyundergrounds.FuRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

/**
 * {@code fantasyundergrounds:y_bands}: rock strata. Picks a block by height, in horizontal bands
 * {@code band_height} blocks thick that undulate gently across the landscape, cycling through
 * {@code states}. A pure function of the position (no randomness), so neighbouring features
 * always agree and the canyon walls show continuous layers.
 */
public final class YBandsProvider extends BlockStateProvider {
    public static final MapCodec<YBandsProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("states").forGetter(p -> p.states),
        Codec.intRange(1, 16).fieldOf("band_height").forGetter(p -> p.bandHeight)
    ).apply(i, YBandsProvider::new));

    private final List<BlockState> states;
    private final int bandHeight;

    public YBandsProvider(List<BlockState> states, int bandHeight) {
        this.states = List.copyOf(states);
        this.bandHeight = bandHeight;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return FuRegistries.Y_BANDS.get();
    }

    @Override
    public BlockState getState(WorldGenLevel level, RandomSource random, BlockPos pos) {
        return stateAt(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Shared with the self-test. */
    public BlockState stateAt(int x, int y, int z) {
        double warp = 2.5 * Math.sin(x * 0.031 + z * 0.017) + 1.5 * Math.sin(z * 0.043 - x * 0.011);
        int band = Math.floorDiv(y + (int) Math.round(warp) + 4096, this.bandHeight);
        return this.states.get(Math.floorMod(band, this.states.size()));
    }
}
