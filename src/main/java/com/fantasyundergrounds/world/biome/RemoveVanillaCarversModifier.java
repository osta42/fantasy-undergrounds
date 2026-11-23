package com.fantasyundergrounds.world.biome;

import com.fantasyundergrounds.config.FuSettings;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * {@code fantasyundergrounds:remove_vanilla_carvers}: removes exactly the three vanilla Overworld
 * carvers (cave, cave_extra_underground, canyon) from the given biomes, because this mod's
 * tunnels and canyons replace them (PLAN.md 2.2). Carvers from other mods and every feature list
 * are never touched. Skipped entirely when the mod is disabled or removeVanillaCarvers=false.
 */
public record RemoveVanillaCarversModifier(HolderSet<Biome> biomes) implements BiomeModifier {
    public static final MapCodec<RemoveVanillaCarversModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Biome.LIST_CODEC.fieldOf("biomes").forGetter(RemoveVanillaCarversModifier::biomes)
    ).apply(i, RemoveVanillaCarversModifier::new));

    private static final Set<Identifier> VANILLA_CARVERS = Set.of(
        Identifier.withDefaultNamespace("cave"),
        Identifier.withDefaultNamespace("cave_extra_underground"),
        Identifier.withDefaultNamespace("canyon"));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE || !this.biomes.contains(biome)) {
            return;
        }
        FuSettings s = FuSettings.get();
        if (!s.enabled() || !s.removeVanillaCarvers()) {
            return;
        }
        builder.getGenerationSettings().getCarvers().removeIf(c ->
            c.unwrapKey().map(k -> VANILLA_CARVERS.contains(k.identifier())).orElse(false));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
