package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import net.minecraft.world.level.levelgen.RandomState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

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
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("offset").forGetter(f -> f.offset),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("base").forGetter(f -> f.base),
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
    static final double HALF_WIDTH = 30.0, WIDTH_SWING = 8.0, MAIN_RUN = 110.0, MAIN_WALL = 150.0;
    // the massif above the valley level: its least height, and how much more its crests and peaks reach
    static final double MASSIF_BASE = 85.0, MASSIF_PEAKS = 115.0, TOP = 314.0;
    // the side valleys: the noise's usual slope at its zero lines, their floor's half-width, walls' run and rise,
    // and how their floors climb (per block away from the main valley) up to at most SIDE_FLOOR_MAX above it
    static final double SIDE_SLOPE = 0.0042, SIDE_HALF = 3.0, SIDE_RUN = 60.0, SIDE_WALL = 55.0,
            SIDE_FLOOR_CLIMB = 0.4, SIDE_FLOOR_MAX = 110.0;
    // the gullies down the mountainsides: walls' run and rise, how their floors climb away from the side valleys
    static final double GULLY_RUN = 30.0, GULLY_WALL = 20.0, GULLY_FLOOR_CLIMB = 0.7, GULLY_FLOOR_MAX = 120.0;
    // the stream's half-width (and in a pond), in blocks
    private static final int GRADIENT_STEP = 2, BROAD_STEP = 24;

    private final DensityFunction continents, erosion, ridges, temperature, humidity, peaks, detail;
    // vanilla's terrain offset (its ground's broad height: 128 + 128 x offset) and its initial density (the ground
    // without caves, with every other climate's floors and ceilings), to find the real height of the ground we
    // blend into
    private final DensityFunction offset, base;
    private final String mode;
    private final ColumnCache cache;

    public AlpineTerrain(DensityFunction continents, DensityFunction erosion, DensityFunction ridges,
                         DensityFunction temperature, DensityFunction humidity, DensityFunction peaks,
                         DensityFunction detail, DensityFunction offset, DensityFunction base, String mode) {
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.temperature = temperature;
        this.humidity = humidity;
        this.peaks = peaks;
        this.detail = detail;
        this.offset = offset;
        this.base = base;
        this.mode = mode;
        this.cache = cacheFor(peaks);
    }

    static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * How far inside the Alps a place is, 0 (outside) to 1, from vanilla's climate noises (each from -1 to 1): the
     * heart of the cool temperate band (temperature -0.75 to -0.1), not dry (humidity from -0.1), inland
     * (continentalness from 0), and only in vanilla's hill and mountain country (erosion below -0.2). Each edge eases in
     * over a good way.
     */
    public static double mask(double t, double h, double c, double e) {
        return ease((t + 0.75) / 0.1) * ease((-0.1 - t) / 0.1) * ease((h + 0.1) / 0.1)
                * ease(c / 0.25) * ease((-0.2 - e) / 0.2);
    }

    /**
     * How far inside the Continental mountains (Dfc) a place is: the same, but everywhere looser (temperature -0.9
     * to 0.05, humidity from -0.25, continentalness from -0.15, erosion below 0), so they spread well beyond the Alps
     * all round - wherever the Alps are, they stand inside it, ringed by a broad belt of long rolling hills.
     */
    public static double beltMask(double t, double h, double c, double e) {
        return ease((t + 0.9) / 0.1) * ease((0.05 - t) / 0.1) * ease((h + 0.25) / 0.1)
                * ease((c + 0.15) / 0.3) * ease((0.05 - e) / 0.45);
    }

    /** From these in, the Continental mountains (by beltMask) and the Alps (by mask) take the biome. */
    public static final double BELT = 0.25, ALPS = 0.5;
    /**
     * From here in, the biome is the Alps' (the valley, or the altitude bands): a little before the ground is all
     * the Alps' (ALPS), where their mountains have already taken over from the hills - so the Alps' steep, rocky
     * outer mountains are the Alps' biomes, never the Continental mountains' forest.
     */
    public static final double ALPS_BIOME = 0.4;

    /**
     * How far the ground has turned from vanilla's into ours, from the belt's mask: the height blends over all of
     * this (so the land rises from vanilla's into the hills over a long way, without a step), while the density
     * itself is ours from early on (DENSITY_RAMP), where the two grounds are still at almost the same height.
     */
    public static double weight(double b) {
        return ease(b / 0.6);
    }

    static final double DENSITY_RAMP = 0.12;

    /** Smooth minimum of a and b (blending over k). */
    static double smin(double a, double b, double k) {
        double h = Mth.clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return Mth.lerp(h, b, a) - k * h * (1.0 - h);
    }

    // ================================================================ the column cache

    /** One column's answer: how much of the ground is ours (the density's weight), and its surface's height. */
    record Column(long key, double weight, double height) {
    }

    /**
     * The columns worked out so far in one world, shared by every copy of this function there (the height, the
     * weight and its inverse; the final and the initial density; every chunk's own copy, and the ones built to find
     * a column's height for structures). A column takes a good deal of work (the Alps' ridges and valleys, finding
     * vanilla's ground beneath the blend), and the game asks for the same one many times over - from each of the
     * three, from both densities, from the neighbouring chunks (which share their edge columns), and again for every
     * structure that looks at the ground. A fixed table, each slot holding one column (the latest), so it never
     * grows; its entries never change once made, so threads may share it without locks.
     */
    static final class ColumnCache {
        static final int SIZE = 1 << 16;
        final Column[] slots = new Column[SIZE];
    }

    // one table per world: keyed by the world's own copy of the peaks noise (each world makes its own)
    private static final Map<Object, ColumnCache> CACHES = Collections.synchronizedMap(new WeakHashMap<>());

    private static ColumnCache cacheFor(DensityFunction peaks) {
        Object[] found = {null};
        peaks.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder holder) {
                if (found[0] == null && holder.noise() != null) found[0] = holder.noise();
                return holder;
            }
        });
        // (not yet wired to a world's noises: as read from the data pack, never used to build terrain)
        if (found[0] == null) return null;
        return CACHES.computeIfAbsent(found[0], k -> new ColumnCache());
    }

    // ================================================================ the Subpolar taiga's fjords (Dfc)

    // the climates of the far north (as KoppenBiomeSource has them; unquantized): the Tundra (ET) below TUNDRA,
    // the Subpolar taiga (Dfc) from there to SUBPOLAR - but for the wet coasts there, the Scottish highland's (Cfc)
    public static final double TUNDRA = -0.78, SUBPOLAR = -0.62, SUBPOLAR_WET_COAST = -0.1;

    /**
     * How far a place is into the Subpolar taiga's coast (0 to 1): its climate (temperature -0.78 to -0.62, the
     * drier coasts there), from a little out at sea (where the ground still has to come up out of the sea floor)
     * to a way inland.
     */
    public static double fjordMask(double t, double h, double c) {
        return ease((t - TUNDRA) / 0.03) * ease((SUBPOLAR - t) / 0.03) * ease((SUBPOLAR_WET_COAST - 0.02 - h) / 0.04)
                * ease((c + 0.34) / 0.1) * ease((0.22 - c) / 0.12);
    }

    // the coast's mountains stand on ground this high (just above the sea) and rise HILL_RISE more; the fjords
    // run in along the valleys, their floors this deep, their walls this steep (up per across) from this far out
    // from their middle
    static final double FJORD_LAND = 68.0, FJORD_FLOOR = 26.0, FJORD_WALL = 2.2, FJORD_HALF = 9.0;

    /**
     * The Subpolar taiga's coast: steep, forested mountains rising straight out of the sea, and the fjords - long,
     * narrow, deep arms of the sea running far inland between them along the valleys (where vanilla would put its
     * rivers), their walls near sheer. Below the sea's level the game fills them with sea water of itself.
     */
    double fjords(FunctionContext context) {
        double mountains = this.hills(context, FJORD_LAND);
        double d = valleyDistance(this.ridges.compute(context), this.ridgeSlope(context, BROAD_STEP));
        double fjord = FJORD_FLOOR + FJORD_WALL * Math.max(0.0, d - FJORD_HALF);
        return smin(mountains, fjord, 6.0);
    }

    // ================================================================ the Ice cap (EF)

    /**
     * How far a place is into the Ice cap (0 to 1): the coldest of the sea, a little way out from the shore. The
     * terrain there is the sea's own; IceCapFeature lays the ice shelf over the water.
     */
    public static double iceCapMask(double t, double c) {
        return ease((-0.8 - t) / 0.04) * ease((-0.24 - c) / 0.06) * ease((c + 1.0) / 0.04);
    }

    // ================================================================ the Alpine valley's stream

    /**
     * How deep the stream runs at (x, z), in blocks of water (0: no stream here). The stream follows the valley's
     * middle line, but it meanders about it - swinging some 6-8 blocks either way in loops 40-60 blocks long - and
     * its width swells and narrows, here and there widening into a pool; it is deepest in its middle. Only ever on
     * the valley's flat floor, well clear of the mountains' feet. Worked out block by block (the terrain itself is
     * only worked out every 4 blocks), so the stream's banks are smooth curves.
     */
    public int streamDepth(int x, int z) {
        double distance = this.streamDistance(x, z);
        if (distance >= STREAM_MAX) return 0;
        double width = this.streamWidth(x, z);
        if (distance >= width) return 0;
        // never against the valley's walls: the floor must reach a good way round
        for (int[] o : new int[][]{{0, 0}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}}) {
            if (this.surface(x + o[0], z + o[1]) != SURFACE) return 0;
        }
        double k = distance / width;
        return width > STREAM_WIDTH + 1.5 && k < 0.4 ? 3 : k < 0.55 ? 2 : 1;
    }

    /** Whether (x, z) is in the stream or on its banks (within {@code margin} blocks of the water). */
    public boolean nearStream(int x, int z, double margin) {
        double distance = this.streamDistance(x, z);
        return distance < STREAM_MAX + margin && distance < this.streamWidth(x, z) + margin;
    }

    /** How far (x, z) lies from the stream's meandering middle line, in blocks. */
    double streamDistance(int x, int z) {
        double r = this.meanderingRidge(x, z);
        if (Math.abs(r) > 0.2) return Double.MAX_VALUE;
        double gx = (this.meanderingRidge(x + 3, z) - this.meanderingRidge(x - 3, z)) / 6.0;
        double gz = (this.meanderingRidge(x, z + 3) - this.meanderingRidge(x, z - 3)) / 6.0;
        return Math.abs(r) / Mth.clamp(Math.sqrt(gx * gx + gz * gz), 0.5 * TYPICAL_SLOPE, 4.0 * TYPICAL_SLOPE);
    }

    // the weirdness (whose zero line is the valley's middle), pushed this way and that by a finer noise: its zero
    // line, the stream's middle, meanders about the valley's
    private double meanderingRidge(int x, int z) {
        double r = this.ridges.compute(new SinglePointContext(x, 0, z));
        double m = this.detail.compute(new SinglePointContext((int) Math.round(x * MEANDER_SCALE) + 517, 0, (int) Math.round(z * MEANDER_SCALE) - 1033));
        return r + MEANDER * m;
    }

    // the stream's half-width, swelling and narrowing along it, and widening into pools here and there
    private double streamWidth(int x, int z) {
        double swell = this.detail.compute(new SinglePointContext(2 * x - 811, 0, 2 * z + 3307));
        double pool = ease((this.detail.compute(new SinglePointContext(x + 3011, 0, z - 2957)) - 0.45) / 0.2);
        return STREAM_WIDTH + 0.8 * Mth.clamp(swell, -1.0, 1.0) + (POOL_WIDTH - STREAM_WIDTH) * pool;
    }

    // the stream's half-width (blocks; so at least some 6 across), a pool's, and the most either ever is
    static final double STREAM_WIDTH = 4.4, POOL_WIDTH = 7.5, STREAM_MAX = 9.0;
    // how far the stream meanders (in weirdness: some 7 blocks), and how tight its loops (the noise's scale)
    static final double MEANDER = 0.04, MEANDER_SCALE = 2.0;

    // ================================================================ the ground's height, for structures

    private static final Map<RandomState, Optional<AlpineTerrain>> HEIGHTS = Collections.synchronizedMap(new WeakHashMap<>());

    /** This world's Alps' height function, found in its final density (if it has one). */
    public static Optional<AlpineTerrain> of(RandomState state) {
        return HEIGHTS.computeIfAbsent(state, s -> {
            AlpineTerrain[] found = {null};
            s.router().finalDensity().mapAll(new Visitor() {
                @Override
                public DensityFunction apply(DensityFunction function) {
                    if (found[0] == null && function instanceof AlpineTerrain a && a.mode.equals("height") && a.cache != null) found[0] = a;
                    return function;
                }
            });
            return Optional.ofNullable(found[0]);
        });
    }

    /**
     * The ground's surface at (x, z) where the ground is all ours (inside the Alps and the Cold lakeland), worked
     * out straight from the columns as the game does (it blends the four corners of each 4 x 4 cell): the first
     * block of air above is at ceil(surface). NaN where vanilla's ground still has a share in it - ask the game.
     * Far quicker than having the game build the column's whole density to find it.
     */
    public double surface(int x, int z) {
        int x0 = Math.floorDiv(x, 4) * 4, z0 = Math.floorDiv(z, 4) * 4;
        Column c00 = this.column(new SinglePointContext(x0, 0, z0)), c10 = this.column(new SinglePointContext(x0 + 4, 0, z0));
        Column c01 = this.column(new SinglePointContext(x0, 0, z0 + 4)), c11 = this.column(new SinglePointContext(x0 + 4, 0, z0 + 4));
        if (c00.weight() < 1.0 || c10.weight() < 1.0 || c01.weight() < 1.0 || c11.weight() < 1.0) return Double.NaN;
        return Mth.lerp2((x - x0) / 4.0, (z - z0) / 4.0, c00.height(), c10.height(), c01.height(), c11.height());
    }

    @Override
    public double compute(FunctionContext context) {
        Column column = this.column(context);
        return switch (this.mode) {
            case "weight" -> column.weight();
            case "inverse_weight" -> 1.0 - column.weight();
            case "height" -> column.height();
            default -> 0.0;
        };
    }

    Column column(FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        ColumnCache cache = this.cache;
        if (cache == null) return this.computeColumn(context, key);
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 48) & (ColumnCache.SIZE - 1);
        Column column = cache.slots[slot];
        if (column == null || column.key() != key) {
            column = this.computeColumn(context, key);
            cache.slots[slot] = column;
        }
        return column;
    }

    Column computeColumn(FunctionContext context, long key) {
        double t = this.temperature.compute(context), h = this.humidity.compute(context);
        double c = this.continents.compute(context), e = this.erosion.compute(context);
        double b = beltMask(t, h, c, e);
        // (all ours well inside the Alps, whatever the belt's mask: their valley floors must be exactly flat)
        double wl = lakelandWeight(t, h, c, e);
        double wa = b <= 0.0 ? 0.0 : Math.max(weight(b) * (1.0 - wl), alpsCore(mask(t, h, c, e)));
        double wf = fjordMask(t, h, c) * (1.0 - wa - wl);
        double w = wa + wl + wf;
        if (w <= 0.0) return new Column(key, 0.0, 64.0);
        double vanilla = 128.0 + 128.0 * this.offset.compute(context);
        double ground = w >= 1.0 ? 0.0 : this.ground(context, vanilla);
        double ours = 0.0;
        if (wa > 0.0) ours += wa * this.mountains(context, t, h, c, e, b, vanilla);
        if (wl > 0.0) ours += wl * this.lakeland(context);
        if (wf > 0.0) ours += wf * this.fjords(context);
        return new Column(key, ease(w / DENSITY_RAMP), (1.0 - w) * ground + ours);
    }

    /**
     * The real height of the ground vanilla (with every other climate's floors and ceilings) would build here: where
     * its initial density (no caves) turns from solid to air, starting from its broad height and refined to a
     * fraction of a block.
     */
    double ground(FunctionContext context, double guess) {
        int x = context.blockX(), z = context.blockZ();
        int y = Mth.clamp((int) Math.round(guess), -56, 312);
        double d = this.base.compute(new SinglePointContext(x, y, z));
        int lo, hi;
        if (d > 0.0) {
            hi = y;
            do {
                lo = hi;
                hi = lo + 8;
            } while (hi < 320 && this.base.compute(new SinglePointContext(x, hi, z)) > 0.0);
        } else {
            lo = y;
            do {
                hi = lo;
                lo = hi - 8;
            } while (lo > -64 && this.base.compute(new SinglePointContext(x, lo, z)) <= 0.0);
        }
        for (int i = 0; i < 3; i++) {
            int mid = (lo + hi) >> 1;
            if (this.base.compute(new SinglePointContext(x, mid, z)) > 0.0) lo = mid;
            else hi = mid;
        }
        return lo + 0.5;
    }

    /** The Continental mountains' and the Alps' ground. */
    double mountains(FunctionContext context, double t, double h, double c, double e, double b, double vanilla) {
        double m = mask(t, h, c, e);
        // the Continental mountains' hills, giving way to the Alps just before them (all Alps from ALPS in, so the
        // Alpine valley's floor is flat); the Alps' mountains grow in towards their middle
        double alps = ease((m - 0.3) / (ALPS - 0.3));
        double hills = 0.0, slope = -1.0;
        if (alps < 1.0) {
            // their valleys follow vanilla's land a little (half its broad height), so they never stand high over it
            hills = this.hills(context, Mth.clamp(0.5 * vanilla + 40.0, 72.0, 105.0));
            // near the Alps the Alpine valleys run on out through the hills (no wall round the Alps)
            if (m > 0.05) {
                slope = this.ridgeSlope(context, BROAD_STEP);
                // (the trough's floor kept dead flat at the valley's level, as in the Alps - smin alone would dip it)
                double cut = Math.max(smin(hills, this.trough(context, TROUGH_OUTSIDE, slope), 12.0), Math.min(hills, SURFACE));
                hills = Mth.lerp(ease((m - 0.05) / 0.25), hills, cut);
            }
        }
        // (already half their height at their very edge, so they rise from the hills round them - never a
        // sunken floor ringed by the hills)
        if (alps > 0.0 && slope < 0.0) slope = this.ridgeSlope(context, BROAD_STEP);
        double high = alps <= 0.0 ? 0.0 : this.height(context, ALPS_EDGE_SCALE + (1.0 - ALPS_EDGE_SCALE) * ease((m - 0.3) / 0.35), slope);
        return Mth.lerp(alps, hills, high);
    }

    /** The Alps' main valleys alone: their flat floor and their walls rising on either side. */
    double trough(FunctionContext context, double scale, double slope) {
        double n = Mth.clamp(this.detail.compute(context), -1.0, 1.0);
        double d = valleyDistance(this.ridges.compute(context), slope);
        double s1 = Math.max(0.0, d - HALF_WIDTH - WIDTH_SWING * n) / MAIN_RUN;
        return SURFACE + scale * MAIN_WALL * (s1 < 1.0 ? 0.6 * s1 * s1 * (3.0 - 2.0 * s1) + 0.4 * s1 : 1.0 + (s1 - 1.0));
    }

    // the Alps' mountains at their very edge, as a share of their full height; and how steep the Alpine valleys'
    // walls are where they run on out through the Continental mountains (as a share of their walls in the Alps)
    static final double ALPS_EDGE_SCALE = 0.5, TROUGH_OUTSIDE = 0.5;

    // ================================================================ the Cold lakeland (Dfc)

    /**
     * How far inside the Cold lakeland a place is, 0 to 1: the cold side of the Dfb's climate (temperature -0.62,
     * where the Subpolar taiga ends, to -0.38; humidity from 0.1, continentalness from 0.25: see KoppenBiomeSource),
     * away from vanilla's highest mountains (erosion from about -0.45), and never where the Continental mountains
     * are. Its edges are kept narrow, so nearly all of the biome is its flat ground, where the lakes may lie.
     */
    public static double lakelandMask(double t, double h, double c, double e) {
        return ease((-0.38 - t) / 0.025) * ease((t + 0.62) / 0.025) * ease((h - 0.1) / 0.03) * ease((c - 0.25) / 0.05)
                * ease((e + 0.45) / 0.1);
    }

    /**
     * How much of the ground is the Cold lakeland's own: all of it well inside, but for the Alps proper (which keep
     * their mountains); the Continental mountains' rolling hills give way to it (see computeColumn).
     */
    public static double lakelandWeight(double t, double h, double c, double e) {
        return lakelandMask(t, h, c, e) * (1.0 - alpsCore(mask(t, h, c, e)));
    }

    /** How far into the Alps proper (their own ground, not the Continental mountains' hills round them). */
    static double alpsCore(double m) {
        return ease((m - 0.25) / 0.2);
    }

    /** Where the Cold lakeland's biome takes over (its ground nearly all its own, so its lakes lie all through it). */
    public static final double LAKELAND = 0.7;

    // the lakeland's ground (a block or two over the sea) and its lakes' beds; the lakes fill with water to the
    // sea's level (63) of themselves
    static final double LAKELAND_GROUND = 65.0, SMALL_LAKE_BED = 58.0, BIG_LAKE_BED = 55.0;
    // the smaller lakes: one in most LAKE_CELL squares, 18-30 blocks' radius; the big ones: one in most
    // BIG_LAKE_CELL squares, 35-70 blocks' radius
    static final int LAKE_CELL = 80, BIG_LAKE_CELL = 160;

    /**
     * The Cold lakeland's ground: very flat, a block or two above the sea, low swells of a block or two, and
     * everywhere round lakes - sunk into it as bowls, their shores shelving gently, a few big ones among many small,
     * running into one another here and there (Finland's lake country).
     */
    double lakeland(FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        double roll = Mth.clamp(this.detail.compute(context), -1.0, 1.0);
        double ground = LAKELAND_GROUND + 1.5 * roll;
        ground = Math.min(ground, this.lakes(x, z, LAKE_CELL, 0.7, 18.0, 12.0, ground, SMALL_LAKE_BED, 0));
        ground = Math.min(ground, this.lakes(x, z, BIG_LAKE_CELL, 0.8, 35.0, 35.0, ground, BIG_LAKE_BED, 7));
        return ground;
    }

    /**
     * Whether a lake may lie at (x, z): only well inside the lakeland, where its ground is all the lakeland's own -
     * near the edge, where the ground still rises towards vanilla's, a lake's bowl would stand above the sea and
     * stay dry (a crater).
     */
    boolean lakeAllowed(int x, int z) {
        SinglePointContext at = new SinglePointContext(x, 0, z);
        double t = this.temperature.compute(at), h = this.humidity.compute(at);
        double c = this.continents.compute(at), e = this.erosion.compute(at);
        return lakelandWeight(t, h, c, e) >= LAKE_MIN_WEIGHT;
    }

    static final double LAKE_MIN_WEIGHT = 0.8;

    /** The ground with the lakes of one size sunk into it (the lowest where they overlap). */
    double lakes(int x, int z, int cell, double chance, double minR, double rangeR, double ground, double bed, int salt) {
        int ci = Math.floorDiv(x, cell), cj = Math.floorDiv(z, cell);
        double out = ground;
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                int a = i * 31 + salt * 1013, b2 = j * 17 - salt * 7919;
                if (cell(a, b2) >= chance) continue;
                double r = minR + rangeR * cell(a + 7919, b2);
                double cx = i * cell + cell * cell(a, b2 + 7919), cz = j * cell + cell * cell(a + 104729, b2 + 7919);
                double dx = x - cx, dz = z - cz;
                double angle = Math.atan2(dz, dx), phase = cell(a, b2 - 7919) * 6.28;
                double edge = r * (1.0 + 0.1 * Math.sin(angle * 3.0 + phase) + 0.05 * Math.sin(angle * 5.0 - phase));
                double q = Math.sqrt(dx * dx + dz * dz) / edge;
                if (q >= 1.0 || !this.lakeAllowed((int) cx, (int) cz)) continue;
                // shelving gently from the shore, the full depth from 70% of the way in
                double sink = ease((1.0 - q) / 0.3);
                out = Math.min(out, ground - sink * (ground - bed));
            }
        }
        return out;
    }

    static double cell(int i, int j) {
        long h = i * 0x9E3779B97F4A7C15L ^ j * 0xC2B2AE3D27D4EB4FL ^ 0x2545F4914F6CDD1DL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /**
     * How steeply the weirdness (whose zero lines the valleys follow) changes here, per block, measured over
     * {@code step} blocks either way: over a long step its broad slope (the weirdness wobbles a little from block to
     * block, so a short step would make the valleys' width wobble with it), over a short one its slope right here.
     */
    double ridgeSlope(FunctionContext context, int step) {
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        double gx = (this.ridges.compute(new SinglePointContext(x + step, y, z))
                - this.ridges.compute(new SinglePointContext(x - step, y, z))) / (2.0 * step);
        double gz = (this.ridges.compute(new SinglePointContext(x, y, z + step))
                - this.ridges.compute(new SinglePointContext(x, y, z - step))) / (2.0 * step);
        return Math.sqrt(gx * gx + gz * gz);
    }

    /**
     * How far from a valley's middle line a place is, in blocks: the weirdness over its slope (within limits, so
     * the valleys never balloon where the weirdness lies almost level, nor pinch where it is steep).
     */
    static double valleyDistance(double r, double slope) {
        return Math.abs(r) / Mth.clamp(slope, 0.35 * TYPICAL_SLOPE, 2.0 * TYPICAL_SLOPE);
    }

    /** The Alps' ground surface in this column, its mountains scaled by {@code scale}. */
    double height(FunctionContext context, double scale, double slope) {
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        double r = this.ridges.compute(context);
        double n = Mth.clamp(this.detail.compute(context), -1.0, 1.0);
        // how far from the valley's middle, in blocks, and the valley floor's half-width here
        double d = valleyDistance(r, slope);
        double half = HALF_WIDTH + WIDTH_SWING * n;
        // the valley floor: dead flat (its stream is cut into it block by block afterwards: AlpineStreamFeature)
        if (d <= half) return SURFACE;
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

    // the Continental mountains: rolling hills up to HILL_RISE above their valleys
    static final double HILL_RISE = 95.0;
    // their long ridges run this way (radians from the x axis), stretched HILL_STRETCH times their width
    static final double HILL_ANGLE = 0.45, HILL_STRETCH = 1.7;
    // how tightly the ridges are packed (more: narrower ridges, steeper slopes), and how full their flanks are
    // (below 1: convex, steep-sided)
    static final double HILL_SCALE = 1.4, HILL_FULLNESS = 0.85;

    /**
     * The Continental mountains' ground: long, high, rolling ridges like the Beskids' - rounded crests (the noise's
     * zero lines, smoothed broadly), broad valleys between them, all running roughly one way (the noise squeezed
     * across the ridges), the ridges rising and falling along their length.
     */
    double hills(FunctionContext context, double valleys) {
        int x = context.blockX(), y = context.blockY(), z = context.blockZ();
        double cos = Math.cos(HILL_ANGLE), sin = Math.sin(HILL_ANGLE);
        double along = (x * cos + z * sin) * HILL_SCALE, across = (-x * sin + z * cos) * HILL_SCALE;
        double n = this.peaks.compute(new SinglePointContext((int) Math.round(along), y, (int) Math.round(across * HILL_STRETCH)));
        // rounded crests along the ridge lines, broad slopes falling to the valleys between
        double crest = Mth.clamp((1.0 - Math.sqrt(n * n + 0.04)) / (1.0 - Math.sqrt(0.04)), 0.0, 1.0);
        crest = Math.pow(crest, HILL_FULLNESS);
        // the crests rise and fall along the ridges into rounded summits and saddles (never a long level top)
        double swell = this.peaks.compute(new SinglePointContext(x + 7331, y, z - 2711));
        double roll = Mth.clamp(this.detail.compute(context), -1.0, 1.0);
        return valleys + HILL_RISE * crest * (0.55 + 0.45 * Mth.clamp(swell, -1.0, 1.0)) + 5.0 * roll;
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
                this.peaks.mapAll(visitor), this.detail.mapAll(visitor), this.offset.mapAll(visitor),
                this.base.mapAll(visitor), this.mode));
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
