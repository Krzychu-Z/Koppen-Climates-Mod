package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Alps: inside vanilla's own mountain country, where the climate is cool temperate and inland, the ground is
 * built anew as a great range of high mountains with long, flat valleys between them.
 *
 * <ul>
 *   <li>The valleys (Alpine valley, Dfb) all lie at the same height, {@link #VALLEY_Y} (their grass at y 120): flat,
 *       on average about 30 blocks across, following the lines where vanilla's own rivers would run (weirdness
 *       near 0), so they are long and wind through the range. Down the middle of each runs a stream three or four
 *       blocks wide, opening here and there into a small pond.</li>
 *   <li>From the valley floor the mountainsides rise at once, U-shaped like the Alps' glacial valleys, over some
 *       170 blocks (about one up for one across, the spruce forest on their lower part), to the mountains themselves: a ridged field of long,
 *       sharp crests (aretes) meeting in pointed peaks, up to y 314, with broad, high shoulders and cols between
 *       them - Alpine mid-altitude (Dfc) from y 150, Alpine high-altitude (ET) from y 240.</li>
 * </ul>
 *
 * <p>Everything here varies smoothly from block to block: no needles, no walls. The height is worked out per column
 * and blended with vanilla's own ground as a density (in the noise settings: vanilla's density times
 * {@code inverse_weight} plus the Alps' times {@code weight}), so across the edge of the Alps ({@link #mask}) the
 * land morphs from vanilla's mountains into these, and from halfway in (where the Alpine valley begins) it is the
 * Alps' alone, solid right down to the bottom of the world: no caves, no ravines.
 *
 * <p>{@link #mode}: {@code height} (the ground's surface), {@code weight} (how much of the Alps there is, 0 to 1),
 * {@code inverse_weight} (1 minus that).
 */
public final class AlpineTerrain implements DensityFunction {
    public static final MapCodec<AlpineTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("peaks").forGetter(f -> f.peaks),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(f -> f.detail),
            Codec.STRING.fieldOf("mode").forGetter(f -> f.mode)
    ).apply(instance, AlpineTerrain::new));
    public static final KeyDispatchDataCodec<AlpineTerrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The valleys' grass stands at this y (their ground's surface is a block above). */
    public static final int VALLEY_Y = 120;
    /** Where the Alpine mid-altitude (Dfc) and the Alpine high-altitude (ET) begin. */
    public static final int MID_Y = 150, HIGH_Y = 240;

    static final double SURFACE = VALLEY_Y + 1.0;
    // the weirdness' usual slope (per block) about its zero lines: |weirdness| / TYPICAL_SLOPE is roughly how far
    // from the valley's middle line a place is, in blocks - without ever dividing by a slope that may be near 0
    static final double TYPICAL_SLOPE = 0.0035;
    // the main valley floor's half-width (varying along the valley); its walls: their run and their rise
    static final double HALF_WIDTH = 18.0, WIDTH_SWING = 7.0, MAIN_RUN = 110.0, MAIN_WALL = 150.0;
    // the massif above the valley level: its least height, and how much more its crests and peaks reach
    static final double MASSIF_BASE = 85.0, MASSIF_PEAKS = 115.0, TOP = 314.0;
    // the side valleys: the noise's usual slope at its zero lines, their floor's half-width, walls' run and rise,
    // and how their floors climb (per block away from the main valley) up to at most SIDE_FLOOR_MAX above it
    static final double SIDE_SLOPE = 0.0042, SIDE_HALF = 3.0, SIDE_RUN = 60.0, SIDE_WALL = 55.0,
            SIDE_FLOOR_CLIMB = 0.4, SIDE_FLOOR_MAX = 110.0;
    // the gullies down the mountainsides: walls' run and rise, how their floors climb away from the side valleys
    static final double GULLY_RUN = 30.0, GULLY_WALL = 20.0, GULLY_FLOOR_CLIMB = 0.7, GULLY_FLOOR_MAX = 120.0;
    // the stream's half-width (and in a pond), in blocks
    static final double STREAM = 1.7, POND = 3.2;
    private static final int GRADIENT_STEP = 2;

    private final DensityFunction continents, erosion, ridges, temperature, humidity, peaks, detail;
    private final String mode;

    public AlpineTerrain(DensityFunction continents, DensityFunction erosion, DensityFunction ridges,
                         DensityFunction temperature, DensityFunction humidity, DensityFunction peaks,
                         DensityFunction detail, String mode) {
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.temperature = temperature;
        this.humidity = humidity;
        this.peaks = peaks;
        this.detail = detail;
        this.mode = mode;
    }

    static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * How far inside the Alps a place is, 0 (outside) to 1, from vanilla's climate noises (each from -1 to 1): the
     * cool temperate band (temperature -0.8 to -0.05), not dry (humidity from -0.15), inland (continentalness from
     * 0.05), and only in vanilla's hill and mountain country (erosion below -0.25, fully from -0.4), so the Alps rise out
     * of mountains, never out of flat land. Each edge eases in over a good way.
     */
    public static double mask(double t, double h, double c, double e) {
        return ease((t + 0.8) / 0.1) * ease((-0.05 - t) / 0.1) * ease((h + 0.15) / 0.1)
                * ease((c - 0.05) / 0.25) * ease((-0.25 - e) / 0.15);
    }

    /** How much of the ground is the Alps' (the rest vanilla's): all of it from halfway in (the Alpine valley). */
    public static double weight(double m) {
        return ease(m / 0.5);
    }

    /** Smooth minimum of a and b (blending over k). */
    static double smin(double a, double b, double k) {
        double h = Mth.clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return Mth.lerp(h, b, a) - k * h * (1.0 - h);
    }

    @Override
    public double compute(FunctionContext context) {
        double m = mask(this.temperature.compute(context), this.humidity.compute(context),
                this.continents.compute(context), this.erosion.compute(context));
        switch (this.mode) {
            case "weight":
                return weight(m);
            case "inverse_weight":
                return 1.0 - weight(m);
            case "height":
                // the mountains grow in towards the middle of the Alps; the valleys keep their level throughout
                return m <= 0.0 ? SURFACE : this.height(context, ease((m - 0.25) / 0.65));
            default:
                return 0.0;
        }
    }

    /** The Alps' ground surface in this column, its mountains scaled by {@code scale}. */
    double height(FunctionContext context, double scale) {
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        double r = this.ridges.compute(context);
        double n = Mth.clamp(this.detail.compute(context), -1.0, 1.0);
        // how far from the valley's middle, in blocks (roughly), and the valley floor's half-width here
        double d = Math.abs(r) / TYPICAL_SLOPE;
        double half = HALF_WIDTH + WIDTH_SWING * n;
        if (d <= half) {
            // the stream: measured with the local slope (so its width holds steady along it), never wider than the floor
            double gx = (this.ridges.compute(new SinglePointContext(x + GRADIENT_STEP, y, z))
                    - this.ridges.compute(new SinglePointContext(x - GRADIENT_STEP, y, z))) / (2.0 * GRADIENT_STEP);
            double gz = (this.ridges.compute(new SinglePointContext(x, y, z + GRADIENT_STEP))
                    - this.ridges.compute(new SinglePointContext(x, y, z - GRADIENT_STEP))) / (2.0 * GRADIENT_STEP);
            double local = Math.abs(r) / Math.max(Math.sqrt(gx * gx + gz * gz), 0.5 * TYPICAL_SLOPE);
            double pond = ease((this.detail.compute(new SinglePointContext(x + 3011, y, z - 2957)) - 0.45) / 0.2);
            double stream = STREAM + (POND - STREAM) * pond;
            if (local < stream && local < half - 2.0) {
                return pond > 0.5 && local < stream - 1.5 ? SURFACE - 2.0 : SURFACE - 1.0;
            }
            return SURFACE;
        }
        // ---- the massif: a high, deeply dissected mass of mountains - ridged multifractal noise, long sharp crests
        // (aretes) where the noise crosses 0, finer crests branching off them and breaking them into pointed peaks
        // and notches, gullies and cirques between; the absolute value is softened a little so crests are sharp,
        // not knife-thin
        // (the whole pattern a little warped, so nothing runs in straight lines)
        int wx = x + (int) Math.round(30.0 * this.detail.compute(new SinglePointContext(x - 1913, y, z + 4421)));
        int wz = z + (int) Math.round(30.0 * this.detail.compute(new SinglePointContext(x + 2687, y, z - 1301)));
        double crests = 0.0, weight = 1.0, amplitude = 0.5, total = 0.0;
        for (int octave = 0; octave < 4; octave++) {
            int f = 2 << octave;
            double v = this.peaks.compute(new SinglePointContext(f * wx + 5101 * octave, y, f * wz - 7243 * octave));
            double c = Mth.clamp(1.0 - Math.sqrt(v * v + 0.003), 0.0, 1.0);
            c = c * c * weight;
            crests += amplitude * c;
            total += amplitude;
            // finer crests stand out most on the coarser ones (ridged multifractal), but run down every slope
            weight = Mth.clamp(0.35 + c * 1.3, 0.0, 1.0);
            amplitude *= 0.5;
        }
        crests = Math.pow(Mth.clamp(crests / (total * 0.9), 0.0, 1.0), 1.2);
        double massif = SURFACE + scale * (MASSIF_BASE + MASSIF_PEAKS * crests);
        // ---- the main valley, carved down into it: a broad trough, its floor flat at the valley level, its walls
        // gentle at their foot (the spruce forest) and steepening upwards, to about one up for one across
        double s1 = Math.max(0.0, d - half) / MAIN_RUN;
        double main = SURFACE + scale * MAIN_WALL * (s1 < 1.0 ? 0.6 * s1 * s1 * (3.0 - 2.0 * s1) + 0.4 * s1 : 1.0 + (s1 - 1.0));
        // ---- the side valleys: a second network of narrower troughs branching off the main valleys, their floors
        // climbing away from the main valley (hanging valleys high up between the peaks), meeting it at its level
        double r2 = this.peaks.compute(new SinglePointContext(2 * x + 911, y, 2 * z + 1777));
        double d2 = Math.abs(r2) / SIDE_SLOPE;
        double sideFloor = SURFACE + scale * Math.min(SIDE_FLOOR_MAX, SIDE_FLOOR_CLIMB * Math.max(0.0, d - half));
        double s2 = Math.max(0.0, d2 - SIDE_HALF) / SIDE_RUN;
        double side = sideFloor + scale * SIDE_WALL * (s2 < 1.0 ? 1.0 - (1.0 - s2) * (1.0 - s2) : 1.0 + (s2 - 1.0));
        // ---- and the gullies: a third, finer network running down the mountainsides into the side valleys
        double r3 = this.peaks.compute(new SinglePointContext(4 * wx + 3331, y, 4 * wz - 2203));
        double d3 = Math.abs(r3) / (2.0 * SIDE_SLOPE);
        double gullyFloor = sideFloor + scale * Math.min(GULLY_FLOOR_MAX, GULLY_FLOOR_CLIMB * Math.max(0.0, d2 - SIDE_HALF));
        double s3 = Math.max(0.0, d3 - 1.0) / GULLY_RUN;
        double gully = gullyFloor + scale * GULLY_WALL * (s3 < 1.0 ? 1.0 - (1.0 - s3) * (1.0 - s3) : 1.0 + (s3 - 1.0));
        double surface = smin(smin(smin(massif, main, 14.0), side, 10.0), gully, 6.0);
        // a little roughness on the slopes
        surface += 2.0 * n * ease((surface - SURFACE - 20.0) / 40.0);
        return Math.max(SURFACE, smin(surface, TOP, 10.0));
    }

    private static double sq(double v) {
        return v * v;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new AlpineTerrain(this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.ridges.mapAll(visitor), this.temperature.mapAll(visitor), this.humidity.mapAll(visitor),
                this.peaks.mapAll(visitor), this.detail.mapAll(visitor), this.mode));
    }

    @Override
    public double minValue() {
        return -2000.0;
    }

    @Override
    public double maxValue() {
        return 2000.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
