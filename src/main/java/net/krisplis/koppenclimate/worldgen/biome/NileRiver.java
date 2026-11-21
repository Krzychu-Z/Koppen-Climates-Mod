package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.krisplis.koppenclimate.worldgen.island.IslandMap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Nile's valley (NileMap), as a ceiling: a height per column, everything above it cut away - and nothing ever
 * filled in. The water's bed lies at y 54, shelving up to the water's edge; the floodplain either side stands a block
 * or two over the water (which is the sea's level, as every river's is); beyond it the valley's sides climb back up
 * to the desert, a block for every one and a half out. It only cuts in the desert climate (fading in over its edge)
 * and on land: out of the desert the river runs shallow and gives out.
 *
 * <p>The noise settings take the smaller of this and everything else (the floors included), both in the final
 * density and in the initial density the aquifers judge the ground's surface by - so the valley's water is the sea's.
 */
public final class NileRiver implements DensityFunction {
    public static final MapCodec<NileRiver> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(f -> f.continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("temperature").forGetter(f -> f.temperature),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("humidity").forGetter(f -> f.humidity)
    ).apply(instance, NileRiver::new));
    public static final KeyDispatchDataCodec<NileRiver> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    static final double NONE = 1000.0;
    static final double BED_Y = 54.0, EDGE_Y = 62.0, PLAIN_Y = 64.0;
    // the desert climate (KoppenBiomeSource.isDesertClimate): the hot band, or its dry edge; the river eases in over FADE
    static final double HOT_TEMP = 0.55, EDGE_TEMP = 0.40, EDGE_HUMIDITY = -0.35, FADE = 0.03, MIN_CONTINENTS = -0.11;

    private final DensityFunction continents, temperature, humidity;
    private volatile NileMap map;
    private final ThreadLocal<double[]> last = ThreadLocal.withInitial(() -> new double[]{Double.NaN, Double.NaN, 0.0});

    public NileRiver(DensityFunction continents, DensityFunction temperature, DensityFunction humidity) {
        this.continents = continents;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    private NileMap map() {
        NileMap m = this.map;
        if (m == null) {
            double t = this.temperature.compute(new SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            m = NileMap.forSeed(IslandMap.layoutSeed(t));
            this.map = m;
        }
        return m;
    }

    static double ease(double t) {
        return NileMap.ease(t);
    }

    /** How fully a spot is in the desert climate (0 outside, 1 well inside). */
    static double desert(double t, double h) {
        return Math.max(ease((t - HOT_TEMP) / FADE), ease((t - EDGE_TEMP) / FADE) * ease((EDGE_HUMIDITY - h) / FADE));
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        double[] c = this.last.get();
        if (c[0] == x && c[1] == z) return c[2];
        double v = this.column(x, z);
        c[0] = x;
        c[1] = z;
        c[2] = v;
        return v;
    }

    private double column(int x, int z) {
        NileMap.Reach r = this.map().reach(x, z);
        if (r.strength() <= 0.0) return NONE;
        SinglePointContext at = new SinglePointContext(x, 0, z);
        double climate = desert(this.temperature.compute(at), this.humidity.compute(at));
        if (climate <= 0.0) return NONE;
        double land = ease((this.continents.compute(at) - MIN_CONTINENTS) / 0.05);
        double strength = r.strength() * climate * land;
        if (strength <= 0.0) return NONE;
        double d = r.distance(), half = r.halfWidth();
        double level;
        if (d <= half - 6.0) level = BED_Y;
        else if (d <= half) level = Mth.lerp((d - (half - 6.0)) / 6.0, BED_Y, EDGE_Y);
        else if (d <= half + NileMap.PLAIN) level = EDGE_Y + Math.min(2.0, (d - half) * 0.5) + (d - half) / NileMap.PLAIN;
        else level = PLAIN_Y + 1.0 + (d - half - NileMap.PLAIN) / 1.5;
        // weakly held, it lifts off (so it never cuts where it shouldn't)
        return level + (1.0 - strength) * 120.0;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new NileRiver(this.continents.mapAll(visitor), this.temperature.mapAll(visitor), this.humidity.mapAll(visitor)));
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
        return this == other || other instanceof NileRiver o && this.continents.equals(o.continents)
                && this.temperature.equals(o.temperature) && this.humidity.equals(o.humidity);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.continents, this.temperature, this.humidity);
    }
}
