package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

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
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(f -> f.hills),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("erosion", DensityFunctions.zero()).forGetter(f -> f.erosion),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("ceiling", false).forGetter(f -> f.ceiling)
    ).apply(instance, CsaHills::new));
    public static final KeyDispatchDataCodec<CsaHills> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = -1000.0;
    // the hills rise from about sea level: low ones at LOW, the highest LOW + HIGH
    private static final double BASE = 64.0, LOW = 10.0, HIGH = 32.0;
    private static final double CLIMATE_EDGE = 0.04, COAST_EDGE = 0.08, RIVER_EDGE = 0.18;

    // the ceiling version: no ceiling at all
    public static final double FAR = 1000.0;
    // the farmland and olive groves are levelled to this height (their grass at y 64)
    private static final double FLAT = 64.5, FARMLAND_EASE = 32.0;
    // the highland's rolling tops: between HIGHLAND_TOP and HIGHLAND_TOP + HIGHLAND_ROLL
    private static final double HIGHLAND_TOP = 150.0, HIGHLAND_ROLL = 30.0;
    // the farmland and olive groves are never levelled into the mountains (low erosion): that would cut a pit into
    // them. Fully levelled above FLAT_EROSION_FULL, not at all below FLAT_EROSION_NONE
    public static final double FLAT_EROSION_FULL = -0.2, FLAT_EROSION_NONE = -0.35;
    private final DensityFunction continents, temperature, humidity, ridges, hills, erosion;
    // false: the height up to which the ground is filled in; true: the height above which it is cut away
    // (the farmland and olive groves are levelled; everywhere else it is FAR)
    private final boolean ceiling;
    private volatile Long salt;

    public CsaHills(DensityFunction continents, DensityFunction temperature, DensityFunction humidity,
                    DensityFunction ridges, DensityFunction hills, DensityFunction erosion, boolean ceiling) {
        this.erosion = erosion;
        this.continents = continents;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ridges = ridges;
        this.hills = hills;
        this.ceiling = ceiling;
    }

    private long salt() {
        Long s = this.salt;
        if (s == null) this.salt = s = CsaZones.salt(this.temperature);
        return s;
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

    /** The hill height here (a fill height; 60 or less is none), and the climate weight; null outside the Csa. */
    private double[] hillsAt(FunctionContext context) {
        double h = this.humidity.compute(context);
        if (h >= KoppenBiomeSource.CSA_MAX_HUMIDITY) return null;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.CSA_MIN_TEMP || t >= KoppenBiomeSource.CSA_MAX_TEMP) return null;
        double w = weight(this.continents.compute(context), t, h);
        if (w <= 0.0) return null;
        double river = ease(Math.abs(this.ridges.compute(context)) / RIVER_EDGE);
        double n = Mth.clamp(this.hills.compute(context) * 0.5 + 0.5, 0.0, 1.0);
        double height = (LOW + HIGH * n * n) * w * river;
        return new double[]{height < 1.0 ? 60.0 : BASE + height, w, river, n};
    }

    @Override
    public double compute(FunctionContext context) {
        double outside = this.ceiling ? FAR : NONE;
        double[] here = this.hillsAt(context);
        if (here == null) return outside;
        double hills = here[0], w = here[1], river = here[2], n = here[3];
        boolean dry = river >= 0.6;   // not in or right by a river channel, which is left as it is
        // no ponds or lakes on the Mediterranean's dry land: hollows are filled up to the farmland level
        double fill = dry ? Math.max(hills, Mth.lerp(ease((w - 0.2) / 0.2), 60.0, FLAT)) : hills, top = FAR;
        CsaZones.Spot spot = CsaZones.at(context.blockX(), context.blockZ(), this.salt(), this.ridges);
        switch (spot.zone()) {
            case FARMLAND, OLIVE_GROVE -> {
                // levelled perfectly flat just above the sea, right up to the river banks; eased in over the
                // outer FARMLAND_EASE blocks of the patch
                double e = ease(spot.edge() / FARMLAND_EASE) * w * flatness(this.erosion.compute(context));
                fill = Mth.lerp(e, fill, dry ? FLAT : 60.0);
                top = Mth.lerp(e, FAR, FLAT);
            }
            default -> {
            }
        }
        if (this.ceiling) {
            // the mountains here (the Mediterranean highland, Csb) are worn down on top into rolling hills
            double highland = HIGHLAND_TOP + HIGHLAND_ROLL * n;
            return Math.min(top, Mth.lerp(ease(w / 0.5), FAR, highland));
        }
        return fill < 61.0 ? NONE : fill;
    }

    /** How far the farmland and olive groves may be levelled at this erosion: 1 on gentle land, 0 in the mountains. */
    public static double flatness(double erosion) {
        return ease((erosion - FLAT_EROSION_NONE) / (FLAT_EROSION_FULL - FLAT_EROSION_NONE));
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CsaHills(this.continents.mapAll(visitor), this.temperature.mapAll(visitor),
                this.humidity.mapAll(visitor), this.ridges.mapAll(visitor), this.hills.mapAll(visitor), this.erosion.mapAll(visitor), this.ceiling));
    }

    @Override
    public double minValue() {
        return this.ceiling ? 60.0 : NONE;   // (the highland cap is never lower than HIGHLAND_TOP)
    }

    @Override
    public double maxValue() {
        return this.ceiling ? FAR : BASE + LOW + HIGH;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
