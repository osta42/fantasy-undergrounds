package com.fantasyundergrounds.world;

import com.fantasyundergrounds.world.feature.PillarFeature;
import com.fantasyundergrounds.world.feature.SkinFeature;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.neoforge.common.Tags;

/**
 * Gate 5b for this mod's own decoration features, plus a direct test of the ore-safety rule
 * (PLAN.md 2.4): each feature runs twice on identical synthetic caves (stone box, spherical
 * cavity, optional water or lava pool, diamond ore planted on the walls) through a minimal
 * in-memory world, and the result must be identical, may only have replaced base stone or open
 * space, and must leave every ore block untouched. Dev/audit use only.
 */
public final class FeatureSelfTest {
    private FeatureSelfTest() {}

    private enum Fill { AIR, WATER, LAVA }

    private interface Run {
        void run(WorldGenLevel level, RandomSource random);
    }

    public static Map<String, Object> run(long seed) {
        SkinFeature.Config skinRock = new SkinFeature.Config(BlockStateProvider.simple(Blocks.BASALT),
            6, 2, 0.7F, SkinFeature.Exposure.AIR, SkinFeature.Surface.ANY);
        SkinFeature.Config skinLava = new SkinFeature.Config(BlockStateProvider.simple(Blocks.OBSIDIAN),
            5, 1, 0.9F, SkinFeature.Exposure.LAVA, SkinFeature.Surface.ANY);
        SkinFeature.Config skinWater = new SkinFeature.Config(BlockStateProvider.simple(Blocks.GRAVEL),
            5, 2, 0.6F, SkinFeature.Exposure.WATER, SkinFeature.Surface.FLOOR);
        PillarFeature.Config pillar = new PillarFeature.Config(BlockStateProvider.simple(Blocks.BLACKSTONE), UniformInt.of(2, 4), 48);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("skin_air", check(seed, Fill.AIR, (l, r) -> SkinFeature.placeSkin(skinRock, l, r, new BlockPos(0, 6, 0))));
        out.put("skin_lava", check(seed, Fill.LAVA, (l, r) -> SkinFeature.placeSkin(skinLava, l, r, new BlockPos(0, -6, 0))));
        out.put("skin_water", check(seed, Fill.WATER, (l, r) -> SkinFeature.placeSkin(skinWater, l, r, new BlockPos(0, -6, 0))));
        out.put("pillar", check(seed, Fill.AIR, (l, r) -> PillarFeature.placePillar(pillar, l, r, new BlockPos(0, -9, 0))));
        return out;
    }

    private static Map<String, Object> check(long seed, Fill fill, Run run) {
        Map<BlockPos, BlockState> before = cave(fill);
        Map<BlockPos, BlockState> a = new HashMap<>(before);
        Map<BlockPos, BlockState> b = new HashMap<>(before);
        run.run(fakeLevel(a), new XoroshiroRandomSource(seed));
        run.run(fakeLevel(b), new XoroshiroRandomSource(seed));
        long changed = 0;
        long illegal = 0;
        long oresTouched = 0;
        for (Map.Entry<BlockPos, BlockState> e : a.entrySet()) {
            BlockState old = before.get(e.getKey());
            if (old == e.getValue()) {
                continue;
            }
            changed++;
            if (old.is(Tags.Blocks.ORES)) {
                oresTouched++;
            }
            if (!(old.isAir() || !old.getFluidState().isEmpty() || old.is(SkinFeature.REPLACEABLE_BASE))) {
                illegal++;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("identical", a.equals(b));
        r.put("changedBlocks", changed);
        r.put("illegalReplacements", illegal);
        r.put("oresTouched", oresTouched);
        return r;
    }

    private static Map<BlockPos, BlockState> cave(Fill fill) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState ore = Blocks.DIAMOND_ORE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState pool = switch (fill) {
            case LAVA -> Blocks.LAVA.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case AIR -> air;
        };
        Map<BlockPos, BlockState> m = new HashMap<>();
        for (int x = -14; x <= 14; x++) {
            for (int y = -14; y <= 14; y++) {
                for (int z = -14; z <= 14; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (d < 9.5) {
                        m.put(pos, y <= -4 ? pool : air);
                    } else if (d < 10.5 && Math.floorMod(x * 31 + y * 17 + z * 7, 5) == 0) {
                        m.put(pos, ore);
                    } else {
                        m.put(pos, stone);
                    }
                }
            }
        }
        return m;
    }

    /** Minimal WorldGenLevel: block reads/writes against a map; anything else fails loudly. */
    private static WorldGenLevel fakeLevel(Map<BlockPos, BlockState> blocks) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        InvocationHandler h = (proxy, method, args) -> switch (method.getName()) {
            case "getBlockState" -> blocks.getOrDefault(((BlockPos) args[0]).immutable(), stone);
            case "getFluidState" -> blocks.getOrDefault(((BlockPos) args[0]).immutable(), stone).getFluidState();
            case "setBlock" -> {
                blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                yield true;
            }
            case "getMinY" -> -64;
            case "getHeight" -> 384;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FeatureSelfTest.fakeLevel";
            default -> {
                if (method.isDefault()) {
                    yield InvocationHandler.invokeDefault(proxy, method, args);
                }
                throw new UnsupportedOperationException("fake level does not support " + method);
            }
        };
        return (WorldGenLevel) Proxy.newProxyInstance(FeatureSelfTest.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, h);
    }
}
