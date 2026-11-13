package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Flooded rivers of the temperate oceanic band (Cfb): vanilla's own lowland rivers, broadened into slow, wide
 * rivers like the Thames or the Loire. A ceiling (a height per column, everything above it cut away), and a very
 * cautious one:
 *
 * <ul>
 *   <li>it only ever works inside the low valley vanilla itself has already shaped round its river (ridges
 *       within {@link #VALLEY} of 0, where vanilla lowers the ground towards the sea) - never anywhere else;</li>
 *   <li>only on gentle, low ground (erosion from -0.15 up, fading in from -0.25), never in hills or mountains, and
 *       only inland of the coast (so never out at sea);</li>
 *   <li>it never fills anything in, and never digs below y 58: a broad bed a few blocks under the water (which
 *       stands at sea level, as vanilla's rivers do), with low, gently sloping banks - a block up for every two out,
 *       then a block for every block - easing up and away at the valley's edge.</li>
 * </ul>
 *
 * <p>The river is {@link #HALF_WIDTH} blocks either side of its middle (more or less, slowly varying along its
 * course): 20 to 30 blocks of open water. How far a column is from the middle is worked out from the ridges and
 * their slope there; the inputs come smoothed to the block (Smooth2D) so the banks follow smooth curves.
 */
public final class CfbRiver implements DensityFunction {
    public static final MapCodec<CfbRiver> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(f -> f.erosion),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(f -> f.ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("width").forGetter(f -> f.width)
    ).apply(instance, CfbRiver::new));
    public static final KeyDispatchDataCodec<CfbRiver> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    static final double NONE = 1000.0;
    // the Cfb's climate (only well inside it: the rivers ease in over INSET)
    static final double MIN_TEMP = -0.6, MAX_TEMP = -0.15, MIN_HUMIDITY = 0.2, INSET = 0.03;
    // vanilla's river valley: ridges within this of 0, where vanilla's own ground is lowered towards the river (its
    // river biome is within 0.05, its low-lying valley floor out to about 0.1); the cut fades out over the last 0.03
    static final double VALLEY = 0.1, VALLEY_FADE = 0.03;
    // gentle ground only
    static final double MIN_EROSION = -0.25, FULL_EROSION = -0.15;
    // inland only: from the coast's inner edge
    static final double MIN_CONTINENTS = -0.11, FULL_CONTINENTS = -0.06;
    // the bed, the half-width of the open water (varying by +-WIDTH_SWING along the river), and the banks
    static final double BED_Y = 59.0, HALF_WIDTH = 12.0, WIDTH_SWING = 3.0, BANK_Y = 64.0, BANK = 6.0;
    private static final int GRADIENT_STEP = 2;

    private final DensityFunction continents, erosion, ridges, temperature, humidity, width;
    private final ThreadLocal<CellCache> cache = ThreadLocal.withInitial(CellCache::new);

    private static final class CellCache {
        int x0 = Integer.MIN_VALUE, z0 = Integer.MIN_VALUE;
        final double[] values = new double[16];
        final boolean[] known = new boolean[16];
    }

    public CfbRiver(DensityFunction continents, DensityFunction erosion, DensityFunction ridges,
                    DensityFunction temperature, DensityFunction humidity, DensityFunction width) {
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.temperature = temperature;
        this.humidity = humidity;
        this.width = width;
    }

    static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
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
        double r = this.ridges.compute(context);
        if (Math.abs(r) >= VALLEY) return NONE;
        double t = this.temperature.compute(context);
        if (t <= MIN_TEMP || t >= MAX_TEMP) return NONE;
        double h = this.humidity.compute(context);
        if (h <= MIN_HUMIDITY) return NONE;
        double c = this.continents.compute(context);
        if (c <= MIN_CONTINENTS) return NONE;
        double e = this.erosion.compute(context);
        if (e <= MIN_EROSION) return NONE;
        double strength = ease((t - MIN_TEMP) / INSET) * ease((MAX_TEMP - t) / INSET) * ease((h - MIN_HUMIDITY) / INSET)
                * ease((c - MIN_CONTINENTS) / (FULL_CONTINENTS - MIN_CONTINENTS))
                * ease((e - MIN_EROSION) / (FULL_EROSION - MIN_EROSION))
                // easing up and away at the valley's edge
                * ease((VALLEY - Math.abs(r)) / VALLEY_FADE);
        if (strength <= 0.0) return NONE;
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        double gx = (this.ridges.compute(new SinglePointContext(x + GRADIENT_STEP, y, z))
                - this.ridges.compute(new SinglePointContext(x - GRADIENT_STEP, y, z))) / (2.0 * GRADIENT_STEP);
        double gz = (this.ridges.compute(new SinglePointContext(x, y, z + GRADIENT_STEP))
                - this.ridges.compute(new SinglePointContext(x, y, z - GRADIENT_STEP))) / (2.0 * GRADIENT_STEP);
        double d = Math.abs(r) / Math.max(Math.sqrt(gx * gx + gz * gz), 1.0E-5);
        double half = HALF_WIDTH + WIDTH_SWING * Mth.clamp(this.width.compute(context), -1.0, 1.0);
        double level;
        if (d <= half - 3.0) {
            level = BED_Y;
        } else if (d <= half) {
            // shelving up to the water's edge
            level = Mth.lerp((d - (half - 3.0)) / 3.0, BED_Y, 62.0);
        } else if (d <= half + BANK) {
            // a low bank, a block up for every two out
            level = 62.0 + (d - half) * 0.5;
        } else {
            level = 62.0 + BANK * 0.5 + (d - half - BANK);
        }
        // weakly held, it lifts off (never cutting where it shouldn't)
        return level + (1.0 - strength) * 200.0;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CfbRiver(this.continents.mapAll(visitor), this.erosion.mapAll(visitor),
                this.ridges.mapAll(visitor), this.temperature.mapAll(visitor), this.humidity.mapAll(visitor),
                this.width.mapAll(visitor)));
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

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CfbRiver o && this.continents.equals(o.continents)
                && this.erosion.equals(o.erosion) && this.ridges.equals(o.ridges) && this.temperature.equals(o.temperature)
                && this.humidity.equals(o.humidity) && this.width.equals(o.width);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.continents, this.erosion, this.ridges, this.temperature, this.humidity, this.width);
    }
}
