package net.krisplis.koppenclimate.worldgen.island;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The terrain of the tropical volcanic islands, as a density function: solid below an island's surface, air
 * above it, and no say at all away from the islands. The mod's noise settings take the larger of this and
 * vanilla's own terrain, so the islands rise out of the sea and everything else is untouched.
 *
 * <p>It is handed the overworld's own continentalness, temperature and humidity functions so it can tell where
 * the warm open ocean is, exactly as the biome source does.
 */
public final class VolcanicIslands implements DensityFunction {
    public static final MapCodec<VolcanicIslands> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity)
    ).apply(instance, VolcanicIslands::new));
    public static final KeyDispatchDataCodec<VolcanicIslands> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    // how quickly the density goes from solid to air across the surface
    private static final double SURFACE_SOFTNESS = 4.0;

    private final DensityFunction continents;
    private final DensityFunction temperature;
    private final DensityFunction humidity;
    private final IslandMap.ClimateProbe probe;
    private volatile IslandMap map;

    public VolcanicIslands(DensityFunction continents, DensityFunction temperature, DensityFunction humidity) {
        this.continents = continents;
        this.temperature = temperature;
        this.humidity = humidity;
        this.probe = new IslandMap.ClimateProbe() {
            @Override
            public double continentalness(int x, int z) {
                return continents.compute(new SinglePointContext(x, 0, z));
            }

            @Override
            public double temperature(int x, int z) {
                return temperature.compute(new SinglePointContext(x, 0, z));
            }

            @Override
            public double humidity(int x, int z) {
                return humidity.compute(new SinglePointContext(x, 0, z));
            }
        };
    }

    private IslandMap map() {
        IslandMap m = this.map;
        if (m == null) {
            double t = this.temperature.compute(new SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            m = IslandMap.forSeed(IslandMap.layoutSeed(t));
            this.map = m;
        }
        return m;
    }

    @Override
    public double compute(FunctionContext context) {
        double h = this.map().height(context.blockX(), context.blockZ(), this.probe);
        if (Double.isNaN(h)) {
            return -1.0;
        }
        double surface = IslandMap.SEA_LEVEL + h;
        return Mth.clamp((surface - context.blockY()) / SURFACE_SOFTNESS, -1.0, 1.0);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    // the seeded (and, inside a chunk, cached) versions of the climate functions come through here
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new VolcanicIslands(this.continents.mapAll(visitor), this.temperature.mapAll(visitor),
                this.humidity.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
