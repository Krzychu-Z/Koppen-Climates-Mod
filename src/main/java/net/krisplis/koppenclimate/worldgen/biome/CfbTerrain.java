package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * NO LONGER USED: the Cfb keeps vanilla's own ground now (the noise settings don't call this). Kept registered only
 * so the old cfb_floor / cfb_ceiling density function files still load.
 *
 * <p>The ground of the temperate oceanic band (Cfb), as a 2D density function: a height per column. With {@code floor}
 * true it's the floor hollows are filled up to (from y 60, as the mod's noise settings combine it); with it false,
 * the ceiling everything above which is cut away.
 *
 * <ul>
 *   <li><b>The English countryside</b> (from the coast to continentalness 0.15): a broad, flat upland a dozen blocks
 *       above the sea, in long low swells. Where vanilla's ground is rugged (low erosion) the ceiling lets its hills
 *       stand up out of it as they are - no mountain is ever cut down; the floor only ever fills.</li>
 *   <li><b>The White cliffs</b>: the upland stops dead at the coast, along one line of continentalness, in a sheer
 *       wall of white limestone (at least ten blocks high) over a strip of flat sand just above the sea.</li>
 *   <li><b>The Flooded rivers</b>: along the valley lines (ridges 0) a broad channel, at least 14 blocks across and
 *       seven deep, with steep banks - only well inside the Cfb, and only on gentle ground.</li>
 *   <li>Further inland (the temperate forest) the ground is vanilla's again; the upland eases down into it.</li>
 * </ul>
 *
 * <p>Its inputs come smoothed to the block (Smooth2D) and it's worked out for every block column, not blended
 * between 4 x 4 cells, so the cliff and the river banks stand straight along smooth curves, without steps.
 */
public final class CfbTerrain implements DensityFunction {
    public static final MapCodec<CfbTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("floor").forGetter(f -> f.floor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("undulation").forGetter(f -> f.undulation)
    ).apply(instance, CfbTerrain::new));
    public static final KeyDispatchDataCodec<CfbTerrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double NONE = 1000.0;
    static final double CFB_MIN_TEMP = -0.55, CFB_MAX_TEMP = -0.15, CFB_MIN_HUMIDITY = 0.2;
    // the upland: its grass at UPLAND - 1 +- ROLL (so the cliff's top is never below y 73)
    static final double UPLAND = 76.0, ROLL = 2.0;
    // the beach under the cliffs: sand up to y 63
    static final double BEACH = 64.0;
    // the cliff's foot runs along this continentalness; the beach reaches out to BEACH_EDGE, and below it the sea
    // floor slopes away; the ceiling flattens the shore out to SHORE_EDGE
    static final double CLIFF = -0.17, BEACH_EDGE = -0.19, SHORE_EDGE = -0.25;
    // inland, the upland eases down into vanilla's ground between these
    static final double UPLAND_INLAND = 0.15, UPLAND_INLAND_EDGE = 0.22;
    // the climate's easing band (outside it), and how far the floor sinks / the ceiling rises at its very edge
    static final double EDGE = 0.04, EDGE_DROP = 60.0, EDGE_RISE = 250.0;
    // rugged ground (vanilla's two most mountainous erosion bands): from this erosion down the ceiling lifts off, fully by ROUGH_EROSION - ROUGH_BAND
    static final double ROUGH_EROSION = -0.375, ROUGH_BAND = 0.1;
    // the flooded rivers: their bed, flat for RIVER_BED blocks either side of the middle, then the banks rising to
    // just above the water at RIVER_BANK, and on up BANK_SLOPE blocks for every block out
    static final double RIVER_BED_Y = 56.0, RIVER_BED = 7.0, RIVER_BANK = 10.0, RIVER_BANK_Y = 64.0, BANK_SLOPE = 4.0;
    // ...only so far inside the climate (so they never leave it), and on ground no rougher than this
    static final double RIVER_INSET = 0.03, RIVER_MIN_EROSION = -0.4;
    // the biome counts as the river out to this many blocks from the middle
    public static final double RIVER_BIOME = 9.5;
    // where the ridges are sampled either side of a column, to find how far it is from the valley line
    private static final int GRADIENT_STEP = 2;

    private final boolean floor;
    private final DensityFunction continents, erosion, ridges, temperature, humidity, undulation;
    // the columns of the 4 x 4 cell worked out last, per thread: terrain is generated a cell at a time, every block of
    // each of its 16 columns in turn, the cells of one column of cells one after the other
    private final ThreadLocal<CellCache> cache = ThreadLocal.withInitial(CellCache::new);

    private static final class CellCache {
        int x0 = Integer.MIN_VALUE, z0 = Integer.MIN_VALUE;
        final double[] values = new double[16];
        final boolean[] known = new boolean[16];
    }

    public CfbTerrain(boolean floor, DensityFunction continents, DensityFunction erosion, DensityFunction ridges,
                      DensityFunction temperature, DensityFunction humidity, DensityFunction undulation) {
        this.floor = floor;
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.temperature = temperature;
        this.humidity = humidity;
        this.undulation = undulation;
    }

    static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** 1 in the Cfb's climate, easing to 0 over a band just outside it. */
    static double weight(double t, double h) {
        return ease((t - (CFB_MIN_TEMP - EDGE)) / EDGE)
                * ease(((CFB_MAX_TEMP + EDGE) - t) / EDGE)
                * ease((h - (CFB_MIN_HUMIDITY - EDGE)) / EDGE);
    }

    /** 1 well inside the Cfb's climate, easing to 0 at its edge: where the flooded rivers may run. */
    static double innerWeight(double t, double h) {
        return ease((t - CFB_MIN_TEMP) / RIVER_INSET)
                * ease((CFB_MAX_TEMP - t) / RIVER_INSET)
                * ease((h - CFB_MIN_HUMIDITY) / RIVER_INSET);
    }

    /** How strongly a flooded river cuts its channel here (0 to 1). */
    public static double riverStrength(double t, double h, double e) {
        return innerWeight(t, h) * ease((e - RIVER_MIN_EROSION) / 0.1);
    }

    /** The height (a density reference) the flooded river's channel and banks are cut down to, d blocks out from its middle. */
    static double riverProfile(double d) {
        if (d <= RIVER_BED) return RIVER_BED_Y;
        if (d <= RIVER_BANK) return Mth.lerp((d - RIVER_BED) / (RIVER_BANK - RIVER_BED), RIVER_BED_Y, RIVER_BANK_Y);
        return RIVER_BANK_Y + (d - RIVER_BANK) * BANK_SLOPE;
    }

    /** Blocks from the valley line (ridges 0), from the ridges here and their slope. */
    public static double riverDistance(double ridges, double gradientPerBlock) {
        return Math.abs(ridges) / Math.max(gradientPerBlock, 1.0E-5);
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        int x0 = x & ~3, z0 = z & ~3;
        CellCache c = this.cache.get();
        if (c.x0 != x0 || c.z0 != z0) {
            c.x0 = x0;
            c.z0 = z0;
            java.util.Arrays.fill(c.known, false);
        }
        int i = (x - x0) * 4 + (z - z0);
        if (!c.known[i]) {
            c.values[i] = this.column(context);
            c.known[i] = true;
        }
        return c.values[i];
    }

    private double column(FunctionContext context) {
        double t = this.temperature.compute(context);
        double none = this.floor ? -NONE : NONE;
        if (t <= CFB_MIN_TEMP - EDGE || t >= CFB_MAX_TEMP + EDGE) return none;
        double h = this.humidity.compute(context);
        if (h <= CFB_MIN_HUMIDITY - EDGE) return none;
        double w = weight(t, h);
        if (w <= 0.0) return none;
        double c = this.continents.compute(context);
        if (c < SHORE_EDGE) return none;
        double e = this.erosion.compute(context);
        double u = Mth.clamp(this.undulation.compute(context), -1.0, 1.0);
        double upland = UPLAND + ROLL * u;
        // the river channel, where one runs
        double river = NONE;
        double strength = riverStrength(t, h, e);
        if (strength > 0.0) {
            double r = this.ridges.compute(context);
            // cheap test first: well away from any valley line, nothing to cut
            if (Math.abs(r) < 0.2) {
                int x = context.blockX(), y = context.blockY(), z = context.blockZ();
                double gx = (this.ridges.compute(new SinglePointContext(x + GRADIENT_STEP, y, z))
                        - this.ridges.compute(new SinglePointContext(x - GRADIENT_STEP, y, z))) / (2.0 * GRADIENT_STEP);
                double gz = (this.ridges.compute(new SinglePointContext(x, y, z + GRADIENT_STEP))
                        - this.ridges.compute(new SinglePointContext(x, y, z - GRADIENT_STEP))) / (2.0 * GRADIENT_STEP);
                double d = riverDistance(r, Math.sqrt(gx * gx + gz * gz));
                if (d < 40.0) river = riverProfile(d) + (1.0 - strength) * EDGE_RISE;
            }
        }
        if (this.floor) {
            double level;
            if (c >= CLIFF) {
                // the upland, easing down inland
                level = upland - (1.0 - ease((UPLAND_INLAND_EDGE - c) / (UPLAND_INLAND_EDGE - UPLAND_INLAND))) * EDGE_DROP;
            } else if (c >= BEACH_EDGE) {
                level = BEACH;
            } else {
                // the sea floor slopes away below the beach, about a block for every block out
                level = BEACH - (BEACH_EDGE - c) / 0.002;
            }
            level -= (1.0 - w) * EDGE_DROP;
            return Math.min(level, river);
        }
        double ceiling;
        if (c >= CLIFF) {
            ceiling = upland + 2.0 + (1.0 - ease((UPLAND_INLAND_EDGE - c) / (UPLAND_INLAND_EDGE - UPLAND_INLAND))) * EDGE_RISE;
        } else {
            ceiling = BEACH;
        }
        // rugged ground keeps its hills
        ceiling += ease((ROUGH_EROSION - e) / ROUGH_BAND) * EDGE_RISE;
        ceiling += (1.0 - w) * EDGE_RISE;
        return Math.min(ceiling, river);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CfbTerrain(this.floor, this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.ridges.mapAll(visitor), this.temperature.mapAll(visitor), this.humidity.mapAll(visitor),
                this.undulation.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -NONE;
    }

    @Override
    public double maxValue() {
        return NONE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // Value equality, so that the same function used twice is recognised as one by a chunk's noise caches.
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CfbTerrain o && this.floor == o.floor
                && this.continents.equals(o.continents) && this.erosion.equals(o.erosion) && this.ridges.equals(o.ridges)
                && this.temperature.equals(o.temperature) && this.humidity.equals(o.humidity)
                && this.undulation.equals(o.undulation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.floor, this.continents, this.erosion, this.ridges, this.temperature,
                this.humidity, this.undulation);
    }
}
