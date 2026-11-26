package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code fantasyundergrounds:formation}: giant natural formations that are too large for a
 * decoration feature (features may only write about 16 blocks around their chunk), built as
 * multi-chunk structure pieces instead ({@link FormationPiece}). One kind per environment:
 * giant mushrooms (Underground Forests), giant crystal clusters (Crystal Hollows), giant roots
 * (Ancient Root Systems) and spires (Infernal Chasms).
 *
 * <p>A candidate spawns only if its column lies in a chamber of the kind's environment. Floor and
 * ceiling come from the level's own final terrain density (available at structure start, before
 * any block exists), so the result is the same whatever order chunks are generated in.
 */
public final class FormationStructure extends Structure {
    public enum Kind implements StringRepresentable {
        GIANT_MUSHROOM("giant_mushroom", Environment.UNDERGROUND_FORESTS, 20),
        GIANT_CRYSTAL("giant_crystal", Environment.CRYSTAL_HOLLOWS, 16),
        GIANT_ROOT("giant_root", Environment.ROOT_SYSTEMS, 22),
        INFERNAL_SPIRE("infernal_spire", Environment.INFERNAL_CHASMS, 24);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;
        final Environment env;
        final int minHeadroom;

        Kind(String name, Environment env, int minHeadroom) {
            this.name = name;
            this.env = env;
            this.minHeadroom = minHeadroom;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final MapCodec<FormationStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        Kind.CODEC.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(i, FormationStructure::new));

    private final Kind kind;

    public FormationStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        FuSettings s = FuSettings.get();
        if (!s.enabled() || s.frequency(this.kind.env) <= 0) {
            return Optional.empty();
        }
        WorldgenRandom random = ctx.random();
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMinBlockX() + random.nextInt(16);
        int z = cp.getMinBlockZ() + random.nextInt(16);
        long pieceSeed = random.nextLong();
        ZoneField zones = FuWorldgen.zones(ctx.seed());
        // The chamber of the right environment in this column (any tier).
        ZoneField.Slot slot = zones.sample(x, -30, z).column().find(this.kind.env);
        if (slot == null) {
            return Optional.empty();
        }
        // Floor and ceiling of the open space at this column, from the final terrain density.
        DensityFunction terrain = ctx.randomState().router().finalDensity();
        int lo = (int) Math.floor(slot.floorY()) - 24;
        int hi = (int) Math.ceil(slot.ceilY()) + 16;
        int floor = Integer.MIN_VALUE;
        int ceiling = Integer.MIN_VALUE;
        for (int y = lo; y <= hi; y++) {
            boolean open = terrain.compute(new DensityFunction.SinglePointContext(x, y, z)) < 0;
            if (open && floor == Integer.MIN_VALUE) {
                floor = y;
            } else if (!open && floor != Integer.MIN_VALUE) {
                ceiling = y - 1;
                break;
            }
        }
        if (floor == Integer.MIN_VALUE || ceiling == Integer.MIN_VALUE || ceiling - floor < this.kind.minHeadroom) {
            return Optional.empty();
        }
        if (ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(floor), QuartPos.fromBlock(z),
            ctx.randomState().sampler()).is(Biomes.DEEP_DARK)) {
            return Optional.empty();
        }
        if (this.kind == Kind.GIANT_CRYSTAL && random.nextFloat() < 0.4F) {
            // Some clusters grow out of a cavern wall at mid height instead of the floor or ceiling.
            int mid = floor + (int) ((ceiling - floor) * 0.45);
            int start = random.nextInt(8);
            for (int k = 0; k < 8; k++) {
                int d = (start + k) & 7;
                for (int step = 4; step <= 44; step += 2) {
                    int wx = x + (int) Math.round(FormationPiece.WALL_DX[d] * step);
                    int wz = z + (int) Math.round(FormationPiece.WALL_DZ[d] * step);
                    if (terrain.compute(new DensityFunction.SinglePointContext(wx, mid, wz)) > 0) {
                        FormationPiece piece = new FormationPiece(this.kind, pieceSeed, wx, mid, wz, ceiling, d);
                        return Optional.of(new GenerationStub(new BlockPos(wx, mid, wz), builder -> builder.addPiece(piece)));
                    }
                }
            }
        }
        FormationPiece piece = new FormationPiece(this.kind, pieceSeed, x, floor, z, ceiling);
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z), builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return FuRegistries.FORMATION.get();
    }
}
