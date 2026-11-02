package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The humid subtropics (Cfa) - the Subtropical plains and swamp - lie dead flat, just above the lukewarm sea, as a
 * 2D density function: a height per column. With {@code floor} false it is the ceiling everything above which is
 * cut away; with {@code floor} true, the floor up to which hollows are filled in (from y 60, as the mod's noise
 * settings combine it). The swamp along the shore lies right at the water's surface (so its pools fill: see
 * BiomeSurfaceFeature), the plains rise a block or two above it further inland. Vanilla's river valleys (the
 * ridges noise near zero) are not filled, so the rivers run on through.
 *
 * <p>It uses the same climate as {@link KoppenBiomeSource#isCfaClimate}, a little beyond it at the edge, where the
 * ceiling rises and the floor drops away so the land around eases into the plain. That climate only covers gentle
 * ground (erosion CFA_MIN_EROSION and up), well short of the mountains (vanilla's start at -0.375).
 */
public final class CfaPlain implements DensityFunction {
    public static final MapCodec<CfaPlain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("floor").forGetter(f -> f.floor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("undulation").forGetter(f -> f.undulation)
    ).apply(instance, CfaPlain::new));
    public static final KeyDispatchDataCodec<CfaPlain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = 1000.0;
    // the swamp's height (its ground at y 62, its pools level with the sea at 63) and how much higher the plains lie
    private static final double SWAMP_LEVEL = 63.0, PLAINS_RISE = 2.0, STEP = 0.5;
    // how far the ceiling rises (and the floor sinks) at the very edge of the easing band
    private static final double EDGE_RISE = 120.0, EDGE_DROP = 60.0;
    // width of the band around the climate over which the plain eases out (wider in erosion, towards the hills)
    private static final double EDGE = 0.05, EROSION_EDGE = 0.15;
    // the river valleys, which the floor leaves as they are
    private static final double RIVER_EDGE = 0.15;

    private final boolean floor;
    private final DensityFunction continents, erosion, temperature, humidity, ridges, undulation;

    public CfaPlain(boolean floor, DensityFunction continents, DensityFunction erosion, DensityFunction temperature,
                    DensityFunction humidity, DensityFunction ridges, DensityFunction undulation) {
        this.floor = floor;
        this.continents = continents;
        this.erosion = erosion;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ridges = ridges;
        this.undulation = undulation;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** 1 anywhere in the Cfa's climate, easing to 0 over a band just outside it. */
    static double weight(double c, double e, double t, double h) {
        return ease((t - (KoppenBiomeSource.CSA_MIN_TEMP - EDGE)) / EDGE)
                * ease(((KoppenBiomeSource.CSA_MAX_TEMP + EDGE) - t) / EDGE)
                * ease((h - (KoppenBiomeSource.CFA_MIN_HUMIDITY - EDGE)) / EDGE)
                * ease(((KoppenBiomeSource.CFA_MAX_HUMIDITY + EDGE) - h) / EDGE)
                * ease(((KoppenBiomeSource.CFA_MAX_CONTINENTALNESS + EDGE) - c) / EDGE)
                * ease((e - (KoppenBiomeSource.CFA_MIN_EROSION - EROSION_EDGE)) / EROSION_EDGE);
    }

    @Override
    public double compute(FunctionContext context) {
        double none = this.floor ? -NONE : NONE;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.CSA_MIN_TEMP - EDGE || t >= KoppenBiomeSource.CSA_MAX_TEMP + EDGE) return none;
        double h = this.humidity.compute(context);
        if (h <= KoppenBiomeSource.CFA_MIN_HUMIDITY - EDGE || h >= KoppenBiomeSource.CFA_MAX_HUMIDITY + EDGE) return none;
        double c = this.continents.compute(context);
        if (c >= KoppenBiomeSource.CFA_MAX_CONTINENTALNESS + EDGE) return none;
        double e = this.erosion.compute(context);
        double w = weight(c, e, t, h);
        if (w <= 0.0) return none;
        // the swamp along the shore at the water's surface, the plains a little higher inland
        double inland = ease((c - KoppenBiomeSource.CFA_SWAMP_MAX_CONTINENTALNESS) / 0.12);
        double u = Mth.clamp(this.undulation.compute(context), -1.0, 1.0);
        double level = SWAMP_LEVEL + PLAINS_RISE * inland + STEP * u * inland;
        if (!this.floor) return level + (1.0 - w) * EDGE_RISE;
        // the floor: never out at sea, and never in a river valley
        double land = ease((c - KoppenBiomeSource.CFA_MIN_CONTINENTALNESS) / 0.04);
        double river = ease(Math.abs(this.ridges.compute(context)) / RIVER_EDGE);
        double f = w * land * river;
        if (f <= 0.0) return none;
        // (the swamp's floor is its ceiling: all of it right at the water's surface; the plains may dip a block)
        return level - inland - (1.0 - f) * EDGE_DROP;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CfaPlain(this.floor, this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.temperature.mapAll(visitor), this.humidity.mapAll(visitor), this.ridges.mapAll(visitor),
                this.undulation.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return this.floor ? -NONE : SWAMP_LEVEL - STEP;
    }

    @Override
    public double maxValue() {
        return this.floor ? SWAMP_LEVEL + PLAINS_RISE + STEP : NONE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
