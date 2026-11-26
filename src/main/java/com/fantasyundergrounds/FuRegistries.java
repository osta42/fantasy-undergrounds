package com.fantasyundergrounds;

import com.fantasyundergrounds.world.biome.RemoveVanillaCarversModifier;
import com.fantasyundergrounds.world.density.CanyonDensity;
import com.fantasyundergrounds.world.density.CavernDensity;
import com.fantasyundergrounds.world.density.TunnelDensity;
import com.fantasyundergrounds.world.density.FluidBias;
import com.fantasyundergrounds.world.feature.CrystalFeature;
import com.fantasyundergrounds.world.feature.PillarFeature;
import com.fantasyundergrounds.world.feature.RootFeature;
import com.fantasyundergrounds.world.feature.SkinFeature;
import com.fantasyundergrounds.world.feature.YBandsProvider;
import com.fantasyundergrounds.world.placement.ConfigCountPlacement;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import com.fantasyundergrounds.world.structure.AncientRuinsPiece;
import com.fantasyundergrounds.world.structure.AncientRuinsStructure;
import com.fantasyundergrounds.world.structure.ConfigSpreadPlacement;
import com.fantasyundergrounds.world.structure.FormationPiece;
import com.fantasyundergrounds.world.structure.FormationStructure;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
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
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, FantasyUndergrounds.MODID);
    private static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, FantasyUndergrounds.MODID);
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
    public static final Supplier<CrystalFeature> CRYSTAL = FEATURES.register("crystal", () -> new CrystalFeature(CrystalFeature.Config.CODEC));
    public static final Supplier<RootFeature> ROOT = FEATURES.register("root", () -> new RootFeature(RootFeature.Config.CODEC));
    public static final Supplier<com.fantasyundergrounds.world.feature.StrataFeature> STRATA = FEATURES.register("strata",
        () -> new com.fantasyundergrounds.world.feature.StrataFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final Supplier<BlockStateProviderType<YBandsProvider>> Y_BANDS =
        STATE_PROVIDERS.register("y_bands", () -> new BlockStateProviderType<>(YBandsProvider.CODEC));
    // Ancient Depths ruins (Phase 5).
    public static final Supplier<StructureType<AncientRuinsStructure>> ANCIENT_RUINS =
        STRUCTURE_TYPES.register("ancient_ruins", () -> () -> AncientRuinsStructure.CODEC);
    public static final Supplier<StructurePieceType> ANCIENT_RUINS_PIECE =
        STRUCTURE_PIECES.register("ancient_ruins", () -> (StructurePieceType.ContextlessType) AncientRuinsPiece::new);
    // Giant formations (fantasy pass): mushrooms, crystals, roots, spires.
    public static final Supplier<StructureType<FormationStructure>> FORMATION =
        STRUCTURE_TYPES.register("formation", () -> () -> FormationStructure.CODEC);
    public static final Supplier<StructurePieceType> FORMATION_PIECE =
        STRUCTURE_PIECES.register("formation", () -> (StructurePieceType.ContextlessType) FormationPiece::new);
    public static final Supplier<StructurePlacementType<ConfigSpreadPlacement>> CONFIG_SPREAD =
        STRUCTURE_PLACEMENTS.register("config_spread", () -> () -> ConfigSpreadPlacement.CODEC);
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
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        STRUCTURE_PLACEMENTS.register(modBus);
        BIOME_MODIFIERS.register(modBus);
    }

    private FuRegistries() {}
}
