package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The ground of the monsoon subtropics (Cwa), as a 2D density function: a height per column, the floor up to which
 * the ground is filled in (from y 60, as the mod's noise settings combine it). It only ever adds ground: vanilla's
 * own hills and mountains are left whole, never cut down.
 *
 * <ul>
 *   <li>Where the erosion is high (the Subtropical karst peaks) it fills the low ground up to a broad plain a few
 *       blocks above the rivers, as along the Li river: the karst towers (KarstTowerFeature) rise out of it.</li>
 *   <li>Where it is lower (the Monsoon subtropical hills) it raises long, rolling green hills, 20 to 60 blocks high,
 *       cut by deep valleys: the rivers run down the deepest of them, and narrower dry valleys wind between.</li>
 * </ul>
 *
 * <p>At the edge of the climate the floor sinks away, so the land around eases into it. It uses the same climate
 * as {@link KoppenBiomeSource#isCwaClimate}. (With {@code floor} false it gives the matching ceiling, which the
 * noise settings no longer use.) The noise settings smooth it between vanilla's 4 x 4 climate cells (Smooth2D).
 */
public final class CwaTerrain implements DensityFunction {
    public static final MapCodec<CwaTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("floor").forGetter(f -> f.floor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(f -> f.hills),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("valleys").forGetter(f -> f.valleys)
    ).apply(instance, CwaTerrain::new));
    public static final KeyDispatchDataCodec<CwaTerrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = 1000.0;
    // the karst plain: its grass a few blocks above the sea, barely undulating
    private static final double PLAIN = 66.0, PLAIN_ROLL = 1.5;
    // the hills: their crests between HILL_LOW and HILL_LOW + HILL_HIGH; the river valleys' floors at RIVER_FLOOR
    // (below the sea, so the rivers fill them), the dry valleys DRY_DEPTH below the hills around them
    private static final double HILL_LOW = 84.0, HILL_HIGH = 40.0, RIVER_FLOOR = 58.0, DRY_DEPTH = 30.0;
    // how wide the river valleys are (in the ridges noise) and the dry valleys (in the valleys noise)
    private static final double RIVER_VALLEY = 0.32, DRY_VALLEY = 0.11;
    // the cherry orchards' gentle ground: between ORCHARD_LOW and ORCHARD_LOW + ORCHARD_HIGH
    private static final double ORCHARD_LOW = 67.0, ORCHARD_HIGH = 9.0;
    // the band, in erosion, over which the karst plain gives way to the hills
    private static final double SPLIT_BAND = 0.06;
    // the band around the climate over which it eases out, and how far the ceiling rises / the floor sinks there
    private static final double EDGE = 0.03, EDGE_RISE = 120.0, EDGE_DROP = 60.0;

    private final boolean floor;
    private final DensityFunction continents, erosion, temperature, humidity, ridges, hills, valleys;

    public CwaTerrain(boolean floor, DensityFunction continents, DensityFunction erosion, DensityFunction temperature,
                      DensityFunction humidity, DensityFunction ridges, DensityFunction hills, DensityFunction valleys) {
        this.floor = floor;
        this.continents = continents;
        this.erosion = erosion;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ridges = ridges;
        this.hills = hills;
        this.valleys = valleys;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** 1 anywhere in the monsoon subtropics' climate, easing to 0 over a band just outside it. */
    public static double weight(double c, double t, double h) {
        return ease((t - (KoppenBiomeSource.CWA_MIN_TEMP - EDGE)) / EDGE)
                * ease(((KoppenBiomeSource.CWA_MAX_TEMP + EDGE) - t) / EDGE)
                * ease((h - (KoppenBiomeSource.CWA_MIN_HUMIDITY - EDGE)) / EDGE)
                * ease((c - (KoppenBiomeSource.CWA_MIN_CONTINENTALNESS - EDGE)) / EDGE);
    }

    /** How far this spot is the karst plain (1) rather than the hills (0). */
    static double karst(double e) {
        return ease((e - (KoppenBiomeSource.CWA_KARST_MIN_EROSION - SPLIT_BAND)) / (2 * SPLIT_BAND));
    }

    @Override
    public double compute(FunctionContext context) {
        double none = this.floor ? -NONE : NONE;
        double t = this.temperature.compute(context);
        if (t <= KoppenBiomeSource.CWA_MIN_TEMP - EDGE || t >= KoppenBiomeSource.CWA_MAX_TEMP + EDGE) return none;
        double h = this.humidity.compute(context);
        if (h <= KoppenBiomeSource.CWA_MIN_HUMIDITY - EDGE) return none;
        double c = this.continents.compute(context);
        double w = weight(c, t, h);
        if (w <= 0.0) return none;
        double e = this.erosion.compute(context);
        double k = karst(e);
        double r = Math.abs(this.ridges.compute(context));
        double n = Mth.clamp(this.hills.compute(context) * 0.5 + 0.5, 0.0, 1.0);

        // the karst plain
        double plain = PLAIN + PLAIN_ROLL * (n * 2.0 - 1.0);
        // the hills: long rolling crests, the dry valleys winding between them, the river valleys deepest
        double crest = HILL_LOW + HILL_HIGH * n * n * (3.0 - 2.0 * n);
        double dry = ease(Math.abs(this.valleys.compute(context)) / DRY_VALLEY);
        crest -= DRY_DEPTH * (1.0 - dry) * ease((crest - 70.0) / 20.0);
        double riverSide = ease(r / RIVER_VALLEY);
        double hills = Mth.lerp(riverSide * riverSide, RIVER_FLOOR, crest);
        // the coast: the hills come down to the shore
        double inland = ease((c - KoppenBiomeSource.CWA_MIN_CONTINENTALNESS) / 0.15);
        hills = Mth.lerp(inland, Math.min(hills, PLAIN + 2.0), hills);

        // the cherry orchards (the wettest of the hills): gentle, low rolling ground
        double gentle = Mth.lerp(riverSide * riverSide, RIVER_FLOOR, ORCHARD_LOW + ORCHARD_HIGH * n * n * (3.0 - 2.0 * n));
        gentle = Mth.lerp(inland, Math.min(gentle, PLAIN + 2.0), gentle);
        double orchard = ease((h - (KoppenBiomeSource.CWA_ORCHARD_MIN_HUMIDITY - SPLIT_BAND / 2)) / SPLIT_BAND);
        hills = Mth.lerp(orchard, hills, gentle);

        double level = Mth.lerp(k, hills, plain);
        if (!this.floor) return level + (1.0 - w) * EDGE_RISE;
        // the floor: never out at sea, and never in a river channel (the rivers keep their beds)
        double land = ease((c - KoppenBiomeSource.CWA_MIN_CONTINENTALNESS) / 0.04);
        double channel = ease(r / 0.12);
        double f = w * land * channel;
        if (f <= 0.0) return none;
        return level - 1.0 - (1.0 - f) * EDGE_DROP;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CwaTerrain(this.floor, this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.temperature.mapAll(visitor), this.humidity.mapAll(visitor), this.ridges.mapAll(visitor),
                this.hills.mapAll(visitor), this.valleys.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return this.floor ? -NONE : RIVER_FLOOR - 1.0;
    }

    @Override
    public double maxValue() {
        return this.floor ? HILL_LOW + HILL_HIGH : NONE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Value equality, so that the same function used twice (in the terrain and in the biomes' depth) is
    // recognised as one by a chunk's noise caches and worked out only once.
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CwaTerrain o && this.floor == o.floor
                && this.continents.equals(o.continents)
                && this.erosion.equals(o.erosion)
                && this.temperature.equals(o.temperature)
                && this.humidity.equals(o.humidity)
                && this.ridges.equals(o.ridges)
                && this.hills.equals(o.hills)
                && this.valleys.equals(o.valleys);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.floor, this.continents, this.erosion, this.temperature, this.humidity, this.ridges, this.hills, this.valleys);
    }
}
