package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.Environment;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * {@code fantasyundergrounds:config_spread}: vanilla random-spread structure placement whose
 * spacing and frequency come from the STARTUP config instead of the data file
 * ({@code ancientRuinSpacing}, {@code ancientRuins.enabled/frequency}, {@code intensity}).
 * Separation is a third of the spacing. The data file still supplies salt, exclusion zone and
 * a base frequency, which is multiplied by the config. Because it extends
 * {@link RandomSpreadStructurePlacement}, {@code /locate structure} works unchanged.
 */
public final class ConfigSpreadPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<ConfigSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(
        i -> placementCodec(i).apply(i, ConfigSpreadPlacement::new));

    public ConfigSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                 Optional<ExclusionZone> exclusionZone) {
        super(locateOffset, method, effectiveFrequency(frequency), salt, exclusionZone,
            FuSettings.get().ruinSpacing(), Math.max(4, FuSettings.get().ruinSpacing() / 3), RandomSpreadType.LINEAR);
    }

    private static float effectiveFrequency(float base) {
        FuSettings s = FuSettings.get();
        if (!s.enabled()) {
            return 0.0F;
        }
        return (float) Math.max(0.0, Math.min(1.0, base * s.intensity() * s.frequency(Environment.ANCIENT_RUINS)));
    }

    @Override
    public StructurePlacementType<?> type() {
        return FuRegistries.CONFIG_SPREAD.get();
    }
}
