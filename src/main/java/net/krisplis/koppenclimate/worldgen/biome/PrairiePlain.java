package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The ground of the Prairie (Dfa), as a 2D density function: a floor per column the land is filled up to (from y 60,
 * as the mod's noise settings combine it). It only ever adds ground, never cuts any away: inside the Prairie's
 * climate it lays a broad, gently rolling plain a few blocks above the sea, which fills every river valley, pond and
 * hollow solid, so no water is left anywhere on it; whatever vanilla raises above the plain (its low hills) stays as it
 * is. Around the climate's edge the floor sinks away over a band, so the land around eases into it and the rivers
 * outside run on as before.
 *
 * <p>It uses the same climate as {@link KoppenBiomeSource#isPrairieClimate}, easing out just beyond it, so the whole
 * biome lies on the full plain. The noise settings smooth it between vanilla's 4 x 4 climate cells (Smooth2D).
 */
public final class PrairiePlain implements DensityFunction {
    public static final MapCodec<PrairiePlain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("undulation").forGetter(f -> f.undulation)
    ).apply(instance, PrairiePlain::new));
    public static final KeyDispatchDataCodec<PrairiePlain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = 1000.0;
    // the plain: its grass between BASE - ROLL and BASE + ROLL (the sea's surface is 62), in long, low swells
    private static final double BASE = 68.0, ROLL = 3.0;
    // the band around the climate over which the plain eases out, and how far the floor sinks there
    private static final double EDGE = 0.05, EDGE_DROP = 60.0;

    private final DensityFunction continents, erosion, temperature, humidity, undulation;

    public PrairiePlain(DensityFunction continents, DensityFunction erosion, DensityFunction temperature,
                        DensityFunction humidity, DensityFunction undulation) {
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

    /** 1 anywhere in the Prairie's climate, easing to 0 over a band just outside it. */
    static double weight(double c, double e, double t, double h) {
        return ease((t - (KoppenBiomeSource.PRAIRIE_MIN_TEMP - EDGE)) / EDGE)
                * ease(((KoppenBiomeSource.PRAIRIE_MAX_TEMP + EDGE) - t) / EDGE)
                * ease(((KoppenBiomeSource.PRAIRIE_MAX_HUMIDITY + EDGE) - h) / EDGE)
                * ease((c - (KoppenBiomeSource.PRAIRIE_MIN_CONTINENTALNESS - EDGE)) / EDGE)
                * ease((e - (KoppenBiomeSource.PRAIRIE_MIN_EROSION - EDGE)) / EDGE);
    }

    @Override
    public double compute(FunctionContext context) {
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.PRAIRIE_MIN_TEMP - EDGE || t >= KoppenBiomeSource.PRAIRIE_MAX_TEMP + EDGE) return -NONE;
        double h = this.humidity.compute(context);
        if (h >= KoppenBiomeSource.PRAIRIE_MAX_HUMIDITY + EDGE) return -NONE;
        double c = this.continents.compute(context);
        if (c <= KoppenBiomeSource.PRAIRIE_MIN_CONTINENTALNESS - EDGE) return -NONE;
        double e = this.erosion.compute(context);
        double w = weight(c, e, t, h);
        if (w <= 0.0) return -NONE;
        double u = Mth.clamp(this.undulation.compute(context), -1.0, 1.0);
        double level = BASE + ROLL * u;
        return level - 1.0 - (1.0 - w) * EDGE_DROP;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PrairiePlain(this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.temperature.mapAll(visitor), this.humidity.mapAll(visitor), this.undulation.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -NONE;
    }

    @Override
    public double maxValue() {
        return BASE + ROLL;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Value equality, so that the same function used twice is recognised as one by a chunk's noise caches.
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PrairiePlain o
                && this.continents.equals(o.continents)
                && this.erosion.equals(o.erosion)
                && this.temperature.equals(o.temperature)
                && this.humidity.equals(o.humidity)
                && this.undulation.equals(o.undulation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.continents, this.erosion, this.temperature, this.humidity, this.undulation);
    }
}
