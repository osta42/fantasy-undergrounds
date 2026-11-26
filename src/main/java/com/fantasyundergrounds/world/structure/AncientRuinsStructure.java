package com.fantasyundergrounds.world.structure;

import com.fantasyundergrounds.FuRegistries;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code fantasyundergrounds:ancient_ruins}: the Ancient Depths ruins. A ruin complex deep
 * underground (floor at y -40..-22): a central room (hall, crypt or temple) and 1-3 wings, linked
 * by collapsed corridors or broken bridges over carved chasms ({@link AncientRuinsPiece}). The structure's {@code beard_box} terrain adaptation carves the space and adds support
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
        // Most complexes are built into the wall of a nearby chamber, level with its floor, so the
        // front room opens onto the cavern; the rest stay buried (reached through tunnels).
        Direction inward = null;
        double wallRoll = random.nextDouble();
        double wallAngle = random.nextDouble() * Math.PI * 2.0;
        if (wallRoll < 0.75) {
            int[] wall = wallSite(ctx, cx, cz, wallAngle, size);
            if (wall != null) {
                cx = wall[0];
                y = wall[1];
                cz = wall[2];
                double ix = -Math.cos(wallAngle), iz = -Math.sin(wallAngle);
                inward = Math.abs(ix) > Math.abs(iz) ? (ix > 0 ? Direction.EAST : Direction.WEST) : (iz > 0 ? Direction.SOUTH : Direction.NORTH);
            }
        }
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
        List<AncientRuinsPiece> pieces = new ArrayList<>();
        pieces.add(new AncientRuinsPiece(pieceSeed, variant, box));
        // A ruin complex: 1-3 wings (more rooms) off the central room, each reached through a
        // collapsed corridor or, sometimes, a broken bridge over a carved chasm.
        List<Direction> dirs = new ArrayList<>(List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH));
        if (inward != null) {
            dirs.remove(inward);                    // no wing out into the open cavern
        }
        int wings = Math.min(dirs.size(), 1 + random.nextInt(3));
        for (int i = 0; i < wings; i++) {
            Direction dir = dirs.remove(random.nextInt(dirs.size()));
            int wingVariant = random.nextInt(AncientRuinsPiece.VARIANTS);
            int[] ws = AncientRuinsPiece.size(wingVariant);
            int len = 10 + random.nextInt(14);
            boolean bridge = random.nextFloat() < 0.35;
            int width = bridge ? 9 : 7;
            int lo = bridge ? y - AncientRuinsPiece.CHASM : y;
            int hi = y + 7;
            BoundingBox link;
            BoundingBox wing;
            if (dir.getAxis() == Direction.Axis.X) {
                int start = dir == Direction.EAST ? box.maxX() + 1 : box.minX() - len;
                link = new BoundingBox(start, lo, cz - width / 2, start + len - 1, hi, cz - width / 2 + width - 1);
                int wx = dir == Direction.EAST ? start + len : start - ws[0];
                wing = new BoundingBox(wx, y, cz - ws[2] / 2, wx + ws[0] - 1, y + ws[1] - 1, cz - ws[2] / 2 + ws[2] - 1);
            } else {
                int start = dir == Direction.SOUTH ? box.maxZ() + 1 : box.minZ() - len;
                link = new BoundingBox(cx - width / 2, lo, start, cx - width / 2 + width - 1, hi, start + len - 1);
                int wz = dir == Direction.SOUTH ? start + len : start - ws[2];
                wing = new BoundingBox(cx - ws[0] / 2, y, wz, cx - ws[0] / 2 + ws[0] - 1, y + ws[1] - 1, wz + ws[2] - 1);
            }
            pieces.add(new AncientRuinsPiece(random.nextLong(), bridge ? AncientRuinsPiece.BRIDGE : AncientRuinsPiece.CORRIDOR, link));
            pieces.add(new AncientRuinsPiece(random.nextLong(), wingVariant, wing));
        }
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> pieces.forEach(builder::addPiece)));
    }

    /**
     * Finds a chamber wall: walks out from the chamber's centre along {@code angle} through the
     * final terrain density until open space turns into rock, then returns {x, floorY, z} for a
     * ruin whose centre sits a few blocks inside that wall, level with the floor in front of it.
     * Null if the column has no suitable chamber.
     */
    private static int[] wallSite(GenerationContext ctx, int cx, int cz, double angle, int[] size) {
        ZoneField zones = FuWorldgen.zones(ctx.seed());
        ZoneField.Sample smp = zones.sample(cx, -30, cz);
        ZoneField.Slot slot = null;
        for (ZoneField.Slot sl : new ZoneField.Slot[] {smp.column().mid(), smp.column().deep(), smp.column().upper()}) {
            if (sl.isCavern() && sl.env() != Environment.DROWNED_ABYSSES && sl.env() != Environment.INFERNAL_CHASMS
                && sl.floorY() > -92 && sl.floorY() < 20) {
                slot = sl;
                break;
            }
        }
        if (slot == null) {
            return null;
        }
        double[] cp = zones.cellPoint(smp.cellX(), smp.cellZ());
        DensityFunction terrain = ctx.randomState().router().finalDensity();
        double c = Math.cos(angle), sn = Math.sin(angle);
        int probeY = (int) (slot.floorY() + slot.height() * 0.3);
        boolean seenOpen = false;
        for (int r = 0; r <= 150; r += 3) {
            int px = (int) Math.round(cp[0] + c * r);
            int pz = (int) Math.round(cp[1] + sn * r);
            boolean open = terrain.compute(new DensityFunction.SinglePointContext(px, probeY, pz)) < 0;
            if (open) {
                seenOpen = true;
                continue;
            }
            if (!seenOpen) {
                continue;
            }
            // Floor in front of the wall.
            int ox = (int) Math.round(cp[0] + c * (r - 5));
            int oz = (int) Math.round(cp[1] + sn * (r - 5));
            int fy = probeY;
            while (fy > probeY - 48 && terrain.compute(new DensityFunction.SinglePointContext(ox, fy - 1, oz)) < 0) {
                fy--;
            }
            if (fy <= probeY - 48) {
                return null;
            }
            int depth = Math.max(size[0], size[2]) / 2 - 3;
            return new int[] {(int) Math.round(px + c * depth), fy, (int) Math.round(pz + sn * depth)};
        }
        return null;
    }

    @Override
    public StructureType<?> type() {
        return FuRegistries.ANCIENT_RUINS.get();
    }
}
