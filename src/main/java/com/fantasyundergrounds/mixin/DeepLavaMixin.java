package com.fantasyundergrounds.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Second (and last) Mixin, approved for the deep world. Vanilla hardcodes "every open space below
 * y=-54 is lava" in the aquifer's global fluid picker. With the Overworld floor moved to y=-128,
 * that would drown the whole new deep layer in lava. For worlds whose noise floor is below -64,
 * the lava level becomes 10 blocks above the floor instead (y=-118 for the deep Overworld).
 * Every other world (vanilla depth, the Nether, the End, other dimensions) is left untouched.
 */
@Mixin(NoiseBasedChunkGenerator.class)
abstract class DeepLavaMixin {
    @Inject(method = "createFluidPicker", at = @At("HEAD"), cancellable = true)
    private static void fantasyundergrounds$deepLava(NoiseGeneratorSettings settings, CallbackInfoReturnable<Aquifer.FluidPicker> cir) {
        int minY = settings.noiseSettings().minY();
        if (minY >= -64) {
            return;
        }
        int lavaLevel = minY + 10;
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(lavaLevel, Blocks.LAVA.defaultBlockState());
        int seaLevel = settings.seaLevel();
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(seaLevel, settings.defaultFluid());
        int threshold = Math.min(lavaLevel, seaLevel);
        cir.setReturnValue((x, y, z) -> y < threshold ? lava : sea);
    }
}
