package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Nile: one great river, rare, running for thousands of blocks through the hot deserts - across the hot desert,
 * the continental desert, the steppe, the Australian desert, the Lush desert and their mountains alike - a broad
 * band of green floodplain either side of open water sixty to ninety blocks wide.
 *
 * <p>Its course is its own, laid out from the world's layout seed (the same seed the volcanic islands use, taken
 * from the temperature noise), so the terrain (NileRiver, which cuts the valley) and the biome source (which makes it
 * the Nile biome) agree on it exactly:
 * <ul>
 *   <li>the course: where a very slow, warped noise crosses zero - lines a couple of thousand blocks apart;</li>
 *   <li>its rarity: only where a slower noise still allows it (a third or so of those lines, in long stretches),
 *       and of course only where the desert climate is (checked by the callers);</li>
 *   <li>how far a spot is from the middle of the river: the noise's value over its slope there.</li>
 * </ul>
 */
public final class NileMap {
    // the course and its winding
    private static final double COURSE_SCALE = 1.0 / 4200.0, WARP_SCALE = 1.0 / 1100.0, WARP = 300.0;
    // where the river runs at all (and how it fades out at its ends)
    private static final double PRESENCE_SCALE = 1.0 / 9000.0, PRESENCE_MIN = 0.32, PRESENCE_FULL = 0.42;
    // the open water's half-width, swinging slowly along the river; the floodplain beyond it
    public static final double HALF_WIDTH = 36.0, WIDTH_SWING = 10.0, PLAIN = 48.0;
    private static final double WIDTH_SCALE = 1.0 / 800.0;
    // beyond the floodplain, the valley's sides rise back to the desert
    public static final double SIDE = 70.0;
    private static final int STEP = 4;
    private static final double MIN_SLOPE = 0.00042;

    private static final Map<Long, NileMap> MAPS = new ConcurrentHashMap<>();

    public static NileMap forSeed(long seed) {
        return MAPS.computeIfAbsent(seed, NileMap::new);
    }

    private final SimplexNoise course, warpX, warpZ, presence, width;

    private NileMap(long seed) {
        XoroshiroRandomSource random = new XoroshiroRandomSource(seed ^ 0x4E494C45L);
        this.course = new SimplexNoise(random);
        this.warpX = new SimplexNoise(random);
        this.warpZ = new SimplexNoise(random);
        this.presence = new SimplexNoise(random);
        this.width = new SimplexNoise(random);
    }

    /** The river at one column: how far from its middle, and how wide it runs there. */
    public record Reach(double distance, double halfWidth, double strength) {
        public static final Reach NONE = new Reach(Double.MAX_VALUE, 0.0, 0.0);

        /** On the river or its floodplain (the Nile biome). */
        public boolean inBiome() {
            return this.strength >= 0.5 && this.distance <= this.halfWidth + PLAIN;
        }
    }

    private double courseAt(double x, double z) {
        double wx = x + WARP * this.warpX.getValue(x * WARP_SCALE, z * WARP_SCALE);
        double wz = z + WARP * this.warpZ.getValue(x * WARP_SCALE + 31.7, z * WARP_SCALE - 12.9);
        return this.course.getValue(wx * COURSE_SCALE, wz * COURSE_SCALE);
    }

    static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    public Reach reach(int x, int z) {
        double p = this.presence.getValue(x * PRESENCE_SCALE, z * PRESENCE_SCALE);
        if (p <= PRESENCE_MIN) return Reach.NONE;
        double strength = ease((p - PRESENCE_MIN) / (PRESENCE_FULL - PRESENCE_MIN));
        double v = this.courseAt(x, z);
        // a quick bound before working out the slope: the course noise never changes faster than this per block
        if (Math.abs(v) > 0.6) return Reach.NONE;
        double gx = (this.courseAt(x + STEP, z) - this.courseAt(x - STEP, z)) / (2.0 * STEP);
        double gz = (this.courseAt(x, z + STEP) - this.courseAt(x, z - STEP)) / (2.0 * STEP);
        // (the slope taken no gentler than the course's usual, so the bends' insides - where the noise flattens - get
        // their floodplain too)
        double d = Math.abs(v) / Math.max(Math.sqrt(gx * gx + gz * gz), MIN_SLOPE);
        double half = (HALF_WIDTH + WIDTH_SWING * this.width.getValue(x * WIDTH_SCALE, z * WIDTH_SCALE)) * (0.35 + 0.65 * strength);
        if (d > half + PLAIN + SIDE) return Reach.NONE;
        return new Reach(d, half, strength);
    }
}
