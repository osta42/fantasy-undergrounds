package com.fantasyundergrounds.world.placement;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * {@code fantasyundergrounds:in_zone}: passes only when the position is inside one of the given
 * environments of this mod's zone field, in a dimension this mod overhauls, and not inside a
 * biome tagged {@code #fantasyundergrounds:keeps_own_cave_decoration} (lush caves, dripstone
 * caves, deep dark, {@code #c:is_cave}...). There, the cave biome keeps its own decoration and
 * the zone only changes the cave's shape (PLAN.md section 3).
 *
 * <p>{@code grand_canyons} matches open canyon space (within 3 blocks), from the canyon field;
 * {@code floating_islands} matches positions within 4 blocks of a floating island's top surface.
 */
public final class InZoneFilter extends PlacementFilter {
    public static final TagKey<Biome> KEEPS_OWN_DECORATION =
        TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("fantasyundergrounds", "keeps_own_cave_decoration"));

    public static final MapCodec<InZoneFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Environment.CODEC.listOf().fieldOf("environments").forGetter(f -> f.environments)
    ).apply(i, InZoneFilter::new));

    private final List<Environment> environments;

    public InZoneFilter(List<Environment> environments) {
        this.environments = List.copyOf(environments);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        if (!FuWorldgen.appliesTo(level.getLevel().dimension())) {
            return false;
        }
        if (!matches(level.getSeed(), pos)) {
            return false;
        }
        return !level.getBiome(pos).is(KEEPS_OWN_DECORATION);
    }

    private boolean matches(long seed, BlockPos pos) {
        ZoneField zones = FuWorldgen.zones(seed);
        ZoneField.Sample s = zones.sample(pos.getX(), pos.getY(), pos.getZ());
        if (this.environments.contains(s.slot().env())) {
            return true;
        }
        if (this.environments.contains(Environment.GRAND_CANYONS)
            && FuWorldgen.canyons(seed).inside(pos.getX(), pos.getY(), pos.getZ(), 3.0)) {
            return true;
        }
        if (this.environments.contains(Environment.FLOATING_ISLANDS) && s.slot().islands() > 0) {
            double c = zones.cellSize();
            return FuWorldgen.islands(seed).nearTop(s.slot(), c, s.dx() * c, s.dz() * c, pos.getY(), 4.0);
        }
        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return FuRegistries.IN_ZONE.get();
    }

    /** Codec shared by the registry entry. */
    public static Codec<InZoneFilter> codec() {
        return CODEC.codec();
    }
}
