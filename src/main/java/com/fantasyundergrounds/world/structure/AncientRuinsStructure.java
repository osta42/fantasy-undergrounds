package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code fantasyundergrounds:ancient_ruins}: the Ancient Depths ruins (Phase 5). One procedural
 * piece ({@link AncientRuinsPiece}) deep underground (floor at y -40..-22), in one of three
 * layouts. The structure's {@code beard_box} terrain adaptation carves the space and adds support
 * underneath in the noise stage, like an ancient city.
 *
 * <p>Skipped where the spot is inside a Drowned Abyss or Infernal Chasm (it would be flooded),
 * inside a Grand Canyon, or in the deep dark (ancient city territory; the structure set also has
 * an exclusion zone around ancient cities).
 */
public final class AncientRuinsStructure extends Structure {
    public static final MapCodec<AncientRuinsStructure> CODEC = simpleCodec(AncientRuinsStructure::new);

    public AncientRuinsStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        FuSettings s = FuSettings.get();
        if (!s.enabled() || s.frequency(Environment.ANCIENT_RUINS) <= 0) {
            return Optional.empty();
        }
        WorldgenRandom random = ctx.random();
        int variant = random.nextInt(AncientRuinsPiece.VARIANTS);
        int[] size = AncientRuinsPiece.size(variant);
        ChunkPos cp = ctx.chunkPos();
        int cx = cp.getMinBlockX() + random.nextInt(16);
        int cz = cp.getMinBlockZ() + random.nextInt(16);
        int y = -40 + random.nextInt(19);
        long pieceSeed = random.nextLong();
        int midY = y + size[1] / 2;

        Environment env = FuWorldgen.zones(ctx.seed()).sample(cx, midY, cz).slot().env();
        if (env == Environment.DROWNED_ABYSSES || env == Environment.INFERNAL_CHASMS) {
            return Optional.empty();
        }
        if (FuWorldgen.canyons(ctx.seed()).inside(cx, midY, cz, 24.0)) {
            return Optional.empty();
        }
        if (ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(midY), QuartPos.fromBlock(cz),
            ctx.randomState().sampler()).is(Biomes.DEEP_DARK)) {
            return Optional.empty();
        }
        int x0 = cx - size[0] / 2;
        int z0 = cz - size[2] / 2;
        BoundingBox box = new BoundingBox(x0, y, z0, x0 + size[0] - 1, y + size[1] - 1, z0 + size[2] - 1);
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz),
            builder -> builder.addPiece(new AncientRuinsPiece(pieceSeed, variant, box))));
    }

    @Override
    public StructureType<?> type() {
        return FuRegistries.ANCIENT_RUINS.get();
    }
}
