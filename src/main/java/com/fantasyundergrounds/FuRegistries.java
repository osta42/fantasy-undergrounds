package com.fantasyundergrounds;

import com.fantasyundergrounds.world.biome.RemoveVanillaCarversModifier;
import com.fantasyundergrounds.world.density.CavernDensity;
import com.fantasyundergrounds.world.density.TunnelDensity;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
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
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, FantasyUndergrounds.MODID);

    static {
        // Registered so that encoding a router that contains them (debug dumps, other mods) works.
        DENSITY_TYPES.register("caverns", () -> CavernDensity.CODEC);
        DENSITY_TYPES.register("tunnels", () -> TunnelDensity.CODEC);
    }

    public static final Supplier<PlacementModifierType<InZoneFilter>> IN_ZONE =
        PLACEMENT_TYPES.register("in_zone", () -> () -> InZoneFilter.CODEC);
    public static final Supplier<MapCodec<RemoveVanillaCarversModifier>> REMOVE_VANILLA_CARVERS =
        BIOME_MODIFIERS.register("remove_vanilla_carvers", () -> RemoveVanillaCarversModifier.CODEC);

    static void register(IEventBus modBus) {
        DENSITY_TYPES.register(modBus);
        PLACEMENT_TYPES.register(modBus);
        BIOME_MODIFIERS.register(modBus);
    }

    private FuRegistries() {}
}
