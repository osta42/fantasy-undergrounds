package com.fantasyundergrounds.mixin;

import com.fantasyundergrounds.world.FuWorldgen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The mod's only Mixin (PLAN.md 2.3). NeoForge 26.2 has no hook for modifying a noise router,
 * and replacing the Overworld noise settings file would break terrain mods. Instead we wrap the
 * one call where a server level builds its RandomState from a NoiseBasedChunkGenerator's
 * settings, and hand vanilla a copy whose router has this mod's caves swapped in. Nothing is
 * overwritten; other dimensions and the registries are untouched.
 */
@Mixin(ChunkMap.class)
abstract class ChunkMapMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;",
            ordinal = 0
        )
    )
    private RandomState fantasyundergrounds$rewriteRouter(
        NoiseGeneratorSettings settings,
        HolderGetter<NormalNoise.NoiseParameters> noises,
        long seed,
        Operation<RandomState> original,
        @Local(argsOnly = true) ServerLevel level
    ) {
        return original.call(FuWorldgen.settingsFor(level.dimension(), settings, seed), noises, seed);
    }
}
