package com.fantasyundergrounds;

import com.fantasyundergrounds.world.biome.RemoveVanillaCarversModifier;
import com.fantasyundergrounds.world.density.CanyonDensity;
import com.fantasyundergrounds.world.density.CavernDensity;
import com.fantasyundergrounds.world.density.TunnelDensity;
import com.fantasyundergrounds.world.density.FluidBias;
import com.fantasyundergrounds.world.feature.PillarFeature;
import com.fantasyundergrounds.world.feature.SkinFeature;
import com.fantasyundergrounds.world.feature.YBandsProvider;
import com.fantasyundergrounds.world.placement.ConfigCountPlacement;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registry entries. Data-only types: no blocks, items, fluids or entities (brief section 1). */
public final class FuRegistries {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_TYPES =
        DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_TYPES =
        DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<BlockStateProviderType<?>> STATE_PROVIDERS =
        DeferredRegister.create(Registries.BLOCK_STATE_PROVIDER_TYPE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, FantasyUndergrounds.MODID);

    static {
        // Registered so that encoding a router that contains them (debug dumps, other mods) works.
        DENSITY_TYPES.register("caverns", () -> CavernDensity.CODEC);
        DENSITY_TYPES.register("tunnels", () -> TunnelDensity.CODEC);
        DENSITY_TYPES.register("fluid_bias", () -> FluidBias.CODEC);
        DENSITY_TYPES.register("canyons", () -> CanyonDensity.CODEC);
    }

    public static final Supplier<SkinFeature> SKIN = FEATURES.register("skin", () -> new SkinFeature(SkinFeature.Config.CODEC));
    public static final Supplier<PillarFeature> PILLAR = FEATURES.register("pillar", () -> new PillarFeature(PillarFeature.Config.CODEC));
    public static final Supplier<BlockStateProviderType<YBandsProvider>> Y_BANDS =
        STATE_PROVIDERS.register("y_bands", () -> new BlockStateProviderType<>(YBandsProvider.CODEC));
    public static final Supplier<PlacementModifierType<ConfigCountPlacement>> CONFIG_COUNT =
        PLACEMENT_TYPES.register("config_count", () -> () -> ConfigCountPlacement.CODEC);

    public static final Supplier<PlacementModifierType<InZoneFilter>> IN_ZONE =
        PLACEMENT_TYPES.register("in_zone", () -> () -> InZoneFilter.CODEC);
    public static final Supplier<MapCodec<RemoveVanillaCarversModifier>> REMOVE_VANILLA_CARVERS =
        BIOME_MODIFIERS.register("remove_vanilla_carvers", () -> RemoveVanillaCarversModifier.CODEC);

    static void register(IEventBus modBus) {
        DENSITY_TYPES.register(modBus);
        PLACEMENT_TYPES.register(modBus);
        FEATURES.register(modBus);
        STATE_PROVIDERS.register(modBus);
        BIOME_MODIFIERS.register(modBus);
    }

    private FuRegistries() {}
}
