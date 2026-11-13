package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Australian desert's plain, as a 2D density function (a y level per column), in two halves the mod's noise
 * settings use together: a ceiling the land may not rise above and a floor it may not sink below. Inside the
 * Australian desert the two lie a block apart, so the ground there is a dead-flat plain (with the odd one-block
 * step), and any river valley or hollow under it is filled in solid. Away from it the ceiling is far above the build
 * limit and the floor far below the ground; around its edge they ease apart, so the land around slopes gently to the
 * plain's height instead of ending in a wall or a pit.
 *
 * <p>It uses the same climate as {@link KoppenBiomeSource#isAustralianClimate}, a little beyond it at the edge, so
 * the flattened ground always covers the whole biome.
 */
public final class AustralianPlain implements DensityFunction {
    public static final MapCodec<AustralianPlain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("floor").forGetter(f -> f.floor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("undulation").forGetter(f -> f.undulation)
    ).apply(instance, AustralianPlain::new));
    public static final KeyDispatchDataCodec<AustralianPlain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    // no ceiling (or no floor) at all
    public static final double NONE = 1000.0;
    // the plain's height, and how far it may step up or down
    private static final double BASE = 72.0, STEP = 0.9;
    // how far the ceiling rises (and the floor sinks) at the very edge of the easing band
    private static final double EDGE_RISE = 160.0, EDGE_DROP = 90.0;
    // width of the band around the biome's climate over which the plain eases out
    private static final double EDGE = 0.06;

    private final boolean floor;
    private final DensityFunction continents, erosion, temperature, humidity, undulation;

    public AustralianPlain(boolean floor, DensityFunction continents, DensityFunction erosion, DensityFunction temperature,
                           DensityFunction humidity, DensityFunction undulation) {
        this.floor = floor;
        this.continents = continents;
        this.erosion = erosion;
        this.temperature = temperature;
        this.humidity = humidity;
        this.undulation = undulation;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** 1 anywhere in the Australian desert's climate, easing to 0 over a band just outside it. */
    static double plainWeight(double c, double e, double t, double h) {
        return ease((c - (KoppenBiomeSource.AUSTRALIAN_MIN_CONTINENTS - EDGE)) / EDGE)
                * ease((e - (KoppenBiomeSource.AUSTRALIAN_MIN_EROSION - EDGE)) / EDGE)
                * ease((t - (KoppenBiomeSource.DRY_MIN_TEMP - EDGE)) / EDGE)
                * ease(((KoppenBiomeSource.AUSTRALIAN_MAX_HUMIDITY + EDGE) - h) / EDGE);
    }

    @Override
    public double compute(FunctionContext context) {
        double none = this.floor ? -NONE : NONE;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.DRY_MIN_TEMP - EDGE) return none;
        double h = this.humidity.compute(context);
        if (h >= KoppenBiomeSource.AUSTRALIAN_MAX_HUMIDITY + EDGE) return none;
        double c = this.continents.compute(context);
        if (c <= KoppenBiomeSource.AUSTRALIAN_MIN_CONTINENTS - EDGE) return none;
        double e = this.erosion.compute(context);
        double w = plainWeight(c, e, t, h);
        if (w <= 0.0) return none;
        double u = Mth.clamp(this.undulation.compute(context), -1.0, 1.0);
        double level = BASE + STEP * u;
        return this.floor ? level - 1.0 - (1.0 - w) * EDGE_DROP : level + (1.0 - w) * EDGE_RISE;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new AustralianPlain(this.floor, this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.temperature.mapAll(visitor), this.humidity.mapAll(visitor), this.undulation.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return this.floor ? -NONE : BASE - STEP;
    }

    @Override
    public double maxValue() {
        return this.floor ? BASE + STEP : NONE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Value equality, so that the same function used twice (in the terrain and in the biomes' depth) is
    // recognised as one by a chunk's noise caches and worked out only once.
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AustralianPlain o && this.floor == o.floor
                && this.continents.equals(o.continents)
                && this.erosion.equals(o.erosion)
                && this.temperature.equals(o.temperature)
                && this.humidity.equals(o.humidity)
                && this.undulation.equals(o.undulation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.floor, this.continents, this.erosion, this.temperature, this.humidity, this.undulation);
    }
}
