package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The height the African savanna's land may not rise above, as a 2D density function (a y level per column).
 * The mod's noise settings cut vanilla's terrain off at this height, so the savanna is rolling lowland instead of
 * vanilla's peaks, spires and floating rocks. Away from the savanna it is far above the build limit, and towards
 * the savanna's edge it climbs gradually so neighbouring hills slope down into it rather than ending in a cliff.
 *
 * <p>It uses the same climate as {@link KoppenBiomeSource#isSavannaClimate}, so the flattened land and the
 * savanna biome cover the same ground.
 */
public final class SavannaCeiling implements DensityFunction {
    public static final MapCodec<SavannaCeiling> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("undulation").forGetter(f -> f.undulation)
    ).apply(instance, SavannaCeiling::new));
    public static final KeyDispatchDataCodec<SavannaCeiling> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    // no ceiling at all
    public static final double NONE = 1000.0;
    // the savanna's highest ground: gentle swells around this height
    private static final double BASE = 76.0;
    private static final double UNDULATION = 12.0;
    // how far above BASE the ceiling starts at the savanna's very edge, before easing down to it
    private static final double EDGE_RISE = 200.0;
    // width of the climate band over which the savanna's edge eases in
    private static final double CLIMATE_EDGE = 0.05;
    private static final double COAST_EDGE = 0.08;

    private final DensityFunction continents, temperature, humidity, undulation;

    public SavannaCeiling(DensityFunction continents, DensityFunction temperature, DensityFunction humidity, DensityFunction undulation) {
        this.continents = continents;
        this.temperature = temperature;
        this.humidity = humidity;
        this.undulation = undulation;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** How deep inside the savanna's climate this spot is: 0 outside or at the edge, 1 well inside. */
    static double savannaWeight(double c, double t, double h) {
        double land = ease((c - KoppenBiomeSource.SAVANNA_MIN_CONTINENTALNESS) / COAST_EDGE);
        double dry = ease((KoppenBiomeSource.SAVANNA_MAX_HUMIDITY - h) / CLIMATE_EDGE);
        double warm = ease((t - KoppenBiomeSource.SAVANNA_MIN_TEMP) / CLIMATE_EDGE);
        // beyond the jungle's temperature band only the deserts' less parched edges are savanna
        double notDesert = Math.max(ease((KoppenBiomeSource.SAVANNA_MAX_TEMP - t) / CLIMATE_EDGE),
                ease((h - KoppenBiomeSource.SAVANNA_DESERT_MIN_HUMIDITY) / CLIMATE_EDGE));
        return land * dry * warm * notDesert;
    }

    @Override
    public double compute(FunctionContext context) {
        double h = this.humidity.compute(context);
        if (h >= KoppenBiomeSource.SAVANNA_MAX_HUMIDITY) return NONE;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.SAVANNA_MIN_TEMP) return NONE;
        double c = this.continents.compute(context);
        double w = savannaWeight(c, t, h);
        if (w <= 0.0) return NONE;
        double u = Mth.clamp(this.undulation.compute(context), -1.0, 1.0);
        return BASE + UNDULATION * u + (1.0 - w) * EDGE_RISE;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new SavannaCeiling(this.continents.mapAll(visitor), this.temperature.mapAll(visitor),
                this.humidity.mapAll(visitor), this.undulation.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return BASE - UNDULATION;
    }

    @Override
    public double maxValue() {
        return NONE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
