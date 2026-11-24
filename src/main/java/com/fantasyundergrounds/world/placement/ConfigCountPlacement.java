package com.fantasyundergrounds.world.placement;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.config.FuSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * {@code fantasyundergrounds:config_count}: like {@code minecraft:count}, but the count is a
 * per-chunk cap from the config (maxWaterSpringsPerChunk / maxLavaSpringsPerChunk). Each
 * attempt can still fail later filters, so the number actually placed is at most the cap.
 */
public final class ConfigCountPlacement extends PlacementModifier {
    public enum Setting implements StringRepresentable {
        WATER_SPRINGS("water_springs"), LAVA_SPRINGS("lava_springs");

        public static final Codec<Setting> CODEC = StringRepresentable.fromEnum(Setting::values);
        private final String name;

        Setting(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final MapCodec<ConfigCountPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Setting.CODEC.fieldOf("setting").forGetter(p -> p.setting)
    ).apply(i, ConfigCountPlacement::new));

    private final Setting setting;

    public ConfigCountPlacement(Setting setting) {
        this.setting = setting;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        FuSettings s = FuSettings.get();
        int n = this.setting == Setting.WATER_SPRINGS ? s.maxWaterSprings() : s.maxLavaSprings();
        return Stream.generate(() -> origin).limit(Math.max(0, n));
    }

    @Override
    public PlacementModifierType<?> type() {
        return FuRegistries.CONFIG_COUNT.get();
    }
}
