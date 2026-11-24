package com.fantasyundergrounds.world.density;

import com.fantasyundergrounds.FantasyUndergrounds;
import com.fantasyundergrounds.config.FuSettings;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Rewrites a dimension's (unwired) NoiseRouter so this mod's caves replace vanilla's
 * (PLAN.md 2.2 / 2.3). Nodes are matched by registry key, so the rewrite works on whatever
 * noise settings the world uses (vanilla or a terrain mod's) as long as they still reference the
 * vanilla cave inputs:
 *
 * <pre>
 *   noise minecraft:cave_cheese                      -> caverns - 0.27   (vanilla adds 0.27 back)
 *   noise minecraft:cave_layer                       -> 0
 *   minecraft:overworld/caves/spaghetti_2d           -> tunnels (+ surface guard)
 *   minecraft:overworld/caves/spaghetti_roughness... -> 0
 *   min(x, minecraft:overworld/caves/noodle)         -> x (FU's thin tunnels replace noodles)
 *   minecraft:overworld/caves/entrances              -> entrances + bias, only within ~40 blocks of the surface
 * </pre>
 *
 * If none of these keys are found, it falls back to {@code min(final_density, FU caves)} with its
 * own interpolation and surface guard, and logs a warning (that dimension then keeps its own
 * caves as well).
 *
 * Must run on the router BEFORE RandomState wires it: RandomState's visitor replaces registry
 * holders with direct ones, after which keys are no longer visible.
 */
public final class RouterRewriter {
    private static final Class<?> NOISE_NODE_CLASS;

    static {
        try {
            // DensityFunctions.Noise is a protected record; identify it by class, read its key through
            // the public Visitor API (visitNoise), so no access transformer is needed.
            NOISE_NODE_CLASS = Class.forName("net.minecraft.world.level.levelgen.DensityFunctions$Noise");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final Identifier CAVE_CHEESE = Identifier.withDefaultNamespace("cave_cheese");
    private static final Identifier CAVE_LAYER = Identifier.withDefaultNamespace("cave_layer");
    private static final Identifier SPAGHETTI_2D = Identifier.withDefaultNamespace("overworld/caves/spaghetti_2d");
    private static final Identifier SPAGHETTI_ROUGHNESS = Identifier.withDefaultNamespace("overworld/caves/spaghetti_roughness_function");
    private static final Identifier NOODLE = Identifier.withDefaultNamespace("overworld/caves/noodle");
    private static final Identifier ENTRANCES = Identifier.withDefaultNamespace("overworld/caves/entrances");

    private RouterRewriter() {}

    public record Result(NoiseGeneratorSettings settings, String report) {}

    public static Result rewrite(NoiseGeneratorSettings settings, long seed, String dimension) {
        FuSettings s = FuSettings.get();
        ZoneField zones = FuWorldgen.zones(seed);
        NoiseRouter router = settings.noiseRouter();

        // Surface guard shared by caverns and tunnels: stay at least N blocks under the
        // preliminary surface (protects ocean floors and thin terrain; vanilla's top slide is
        // based on terrain density, not on absolute depth).
        DensityFunction y = DensityFunctions.yClampedGradient(-2048, 2048, -2048.0, 2048.0);
        DensityFunction surface = DensityFunctions.flatCache(router.preliminarySurfaceLevel());
        DensityFunction caverns = guard(new CavernDensity(zones), y, surface, 22.0);
        DensityFunction tunnels = guard(new TunnelDensity(seed, s), y, surface, 12.0);

        // Vanilla's "entrances" input also carries vanilla 3D spaghetti tunnels everywhere
        // underground. Keep it only near the surface, where it makes cave entrances; deeper
        // down this mod's tunnels replace it (no layering of vanilla caves).
        DensityFunction entranceDepthLimit = DensityFunctions.mul(
            DensityFunctions.add(DensityFunctions.add(surface, DensityFunctions.constant(-40.0)),
                DensityFunctions.mul(DensityFunctions.constant(-1.0), y)),
            DensityFunctions.constant(0.125)).clamp(-1.0, 1.0);
        Rewriter rw = new Rewriter(caverns, tunnels, s.entranceFrequency(), entranceDepthLimit);
        DensityFunction finalDensity = rw.apply(router.finalDensity());
        String report;
        if (rw.cheese == 0 && rw.spaghetti == 0) {
            finalDensity = DensityFunctions.min(router.finalDensity(),
                DensityFunctions.interpolated(DensityFunctions.min(caverns, tunnels)));
            report = "FALLBACK (no vanilla cave inputs found; FU caves added on top of the existing terrain's caves)";
            FantasyUndergrounds.LOGGER.warn("[Fantasy Undergrounds] {}: noise router does not reference vanilla cave inputs; using fallback mode. "
                + "This usually means a terrain mod replaced the Overworld noise settings.", dimension);
        } else {
            report = "replaced cave_cheese x" + rw.cheese + ", cave_layer x" + rw.layer + ", spaghetti_2d x" + rw.spaghetti
                + ", spaghetti_roughness x" + rw.roughness + ", noodle x" + rw.noodle + ", entrances x" + rw.entrances;
        }
        // Aquifer inputs: Drowned Abysses flood, Infernal Chasms get lava lakes (PLAN.md 2.5).
        NoiseRouter out = new NoiseRouter(
            router.barrierNoise(),
            new FluidBias(router.fluidLevelFloodednessNoise(), FluidBias.Mode.FLOODEDNESS, zones),
            new FluidBias(router.fluidLevelSpreadNoise(), FluidBias.Mode.SPREAD, zones),
            new FluidBias(router.lavaNoise(), FluidBias.Mode.LAVA, zones),
            router.temperature(),
            router.vegetation(),
            router.continents(),
            router.erosion(),
            router.depth(),
            router.ridges(),
            router.preliminarySurfaceLevel(),
            finalDensity,
            router.veinToggle(),
            router.veinRidged(),
            router.veinGap()
        );
        NoiseGeneratorSettings newSettings = new NoiseGeneratorSettings(
            settings.noiseSettings(),
            settings.defaultBlock(),
            settings.defaultFluid(),
            out,
            settings.surfaceRule(),
            settings.spawnTarget(),
            settings.seaLevel(),
            settings.disableMobGeneration(),
            settings.aquifersEnabled(),
            settings.oreVeinsEnabled(),
            settings.useLegacyRandomSource()
        );
        return new Result(newSettings, report);
    }

    /** max(f, (y - (surface - depth)) / 4): rock within {@code depth} blocks of the surface. */
    private static DensityFunction guard(DensityFunction f, DensityFunction y, DensityFunction surface, double depth) {
        DensityFunction belowSurface = DensityFunctions.mul(
            DensityFunctions.add(DensityFunctions.add(y, DensityFunctions.mul(DensityFunctions.constant(-1.0), surface)),
                DensityFunctions.constant(depth)),
            DensityFunctions.constant(0.25));
        return DensityFunctions.max(f, belowSurface.clamp(-1.0, 1.0));
    }

    /** Pre-order rewriting visitor: checks a node before descending, so registry keys are still visible. */
    private static final class Rewriter implements DensityFunction.Visitor {
        private final DensityFunction caverns;
        private final DensityFunction tunnels;
        private final double entranceBias;
        private final DensityFunction entranceDepthLimit;
        int cheese, layer, spaghetti, roughness, noodle, entrances;

        Rewriter(DensityFunction caverns, DensityFunction tunnels, double entranceFrequency, DensityFunction entranceDepthLimit) {
            this.entranceDepthLimit = entranceDepthLimit;
            this.caverns = caverns;
            this.tunnels = tunnels;
            // 1.0 = vanilla; 0 = no entrances; >1 = more.
            this.entranceBias = (1.0 - entranceFrequency) * 0.35;
        }

        @Override
        public DensityFunction apply(DensityFunction f) {
            // min(x, noodle) -> x. Removing the whole node (instead of making noodle a large
            // constant) avoids vanilla's "MIN between non-overlapping inputs" warning on every
            // NoiseChunk and saves a min per sample.
            if (f instanceof DensityFunctions.TwoArgumentSimpleFunction t
                && t.type() == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN) {
                if (isKey(t.argument2(), NOODLE)) {
                    this.noodle++;
                    return apply(t.argument1());
                }
                if (isKey(t.argument1(), NOODLE)) {
                    this.noodle++;
                    return apply(t.argument2());
                }
            }
            if (f instanceof DensityFunctions.HolderHolder holder) {
                Optional<Identifier> id = holder.function().unwrapKey().map(ResourceKey::identifier);
                if (id.isPresent()) {
                    Identifier k = id.get();
                    if (k.equals(SPAGHETTI_2D)) {
                        this.spaghetti++;
                        return this.tunnels;
                    }
                    if (k.equals(SPAGHETTI_ROUGHNESS)) {
                        this.roughness++;
                        return DensityFunctions.constant(0.0);
                    }
                    if (k.equals(NOODLE)) {
                        this.noodle++;
                        return DensityFunctions.constant(64.0);
                    }
                    if (k.equals(ENTRANCES)) {
                        this.entrances++;
                        DensityFunction e = this.entranceBias == 0.0 ? f
                            : DensityFunctions.add(f, DensityFunctions.constant(this.entranceBias));
                        return DensityFunctions.max(e, this.entranceDepthLimit);
                    }
                }
            } else if (NOISE_NODE_CLASS.isInstance(f)) {
                Identifier noise = noiseKey(f);
                if (CAVE_CHEESE.equals(noise)) {
                    this.cheese++;
                    return DensityFunctions.add(this.caverns, DensityFunctions.constant(-0.27));
                }
                if (CAVE_LAYER.equals(noise)) {
                    this.layer++;
                    return DensityFunctions.constant(0.0);
                }
                return f;
            }
            return f.mapChildren(this);
        }

        private static boolean isKey(DensityFunction f, Identifier key) {
            return f instanceof DensityFunctions.HolderHolder h
                && h.function().unwrapKey().map(k -> k.identifier().equals(key)).orElse(false);
        }

        private static Identifier noiseKey(DensityFunction noiseNode) {
            Identifier[] out = new Identifier[1];
            noiseNode.mapChildren(new DensityFunction.Visitor() {
                @Override
                public DensityFunction apply(DensityFunction input) {
                    return input;
                }

                @Override
                public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                    Holder<NormalNoise.NoiseParameters> data = noise.noiseData();
                    out[0] = data.unwrapKey().map(ResourceKey::identifier).orElse(null);
                    return noise;
                }
            });
            return out[0];
        }
    }
}
