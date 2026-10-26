package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The rolling hills of the Mediterranean (Csa), as a 2D density function: the height (a y level per column) up to
 * which the land is raised in the Csa climate. The mod's noise settings make everything between sea level and
 * this height solid, on top of vanilla's terrain, so the Mediterranean is hilly wherever it is - with the hills
 * easing in from its edges, and dropping away towards the rivers (vanilla's river valleys, where the ridges
 * noise is near zero) so those still run through. Away from the Csa climate it is far below the world.
 *
 * <p>It uses the same climate as {@link KoppenBiomeSource#isMediterraneanClimate}.
 */
public final class CsaHills implements DensityFunction {
    public static final MapCodec<CsaHills> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(f -> f.hills)
    ).apply(instance, CsaHills::new));
    public static final KeyDispatchDataCodec<CsaHills> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = -1000.0;
    // the hills rise from about sea level: low ones at LOW, the highest LOW + HIGH
    private static final double BASE = 64.0, LOW = 10.0, HIGH = 32.0;
    private static final double CLIMATE_EDGE = 0.04, COAST_EDGE = 0.08, RIVER_EDGE = 0.18;

    private final DensityFunction continents, temperature, humidity, ridges, hills;

    public CsaHills(DensityFunction continents, DensityFunction temperature, DensityFunction humidity,
                    DensityFunction ridges, DensityFunction hills) {
        this.continents = continents;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ridges = ridges;
        this.hills = hills;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** How deep inside the Mediterranean climate this spot is: 0 outside or at its edge, 1 well inside. */
    static double weight(double c, double t, double h) {
        double land = ease((c - KoppenBiomeSource.CSA_MIN_CONTINENTALNESS) / COAST_EDGE);
        double cool = ease((t - KoppenBiomeSource.CSA_MIN_TEMP) / CLIMATE_EDGE);
        // the warm, dry side becomes savanna; the warm, wetter side (up to CSA_MAX_TEMP) stays Mediterranean
        double notSavanna = Math.max(ease((KoppenBiomeSource.SAVANNA_MIN_TEMP - t) / CLIMATE_EDGE),
                ease((h - KoppenBiomeSource.SAVANNA_MAX_HUMIDITY) / CLIMATE_EDGE));
        double notTropical = ease((KoppenBiomeSource.CSA_MAX_TEMP - t) / CLIMATE_EDGE);
        double dry = ease((KoppenBiomeSource.CSA_MAX_HUMIDITY - h) / CLIMATE_EDGE);
        return land * cool * notSavanna * notTropical * dry;
    }

    @Override
    public double compute(FunctionContext context) {
        double h = this.humidity.compute(context);
        if (h >= KoppenBiomeSource.CSA_MAX_HUMIDITY) return NONE;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.CSA_MIN_TEMP || t >= KoppenBiomeSource.CSA_MAX_TEMP) return NONE;
        double w = weight(this.continents.compute(context), t, h);
        if (w <= 0.0) return NONE;
        double river = ease(Math.abs(this.ridges.compute(context)) / RIVER_EDGE);
        double n = Mth.clamp(this.hills.compute(context) * 0.5 + 0.5, 0.0, 1.0);
        double height = (LOW + HIGH * n * n) * w * river;
        return height < 1.0 ? NONE : BASE + height;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CsaHills(this.continents.mapAll(visitor), this.temperature.mapAll(visitor),
                this.humidity.mapAll(visitor), this.ridges.mapAll(visitor), this.hills.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return NONE;
    }

    @Override
    public double maxValue() {
        return BASE + LOW + HIGH;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
