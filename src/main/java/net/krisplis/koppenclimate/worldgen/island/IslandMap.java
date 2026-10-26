package net.krisplis.koppenclimate.worldgen.island;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the tropical volcanic islands are, and how tall the ground is on them.
 *
 * <p>The world is cut into square cells. A cell may hold one archipelago of one to four cone-shaped islands, and
 * it keeps it only if the sea at its centre is warm, humid open ocean (never near land, so never in a river).
 * Everything here is worked out from a layout seed and the climate alone, so the terrain (VolcanicIslands
 * density function), the biomes (KoppenBiomeSource) and the volcano structure all agree on the same islands.
 *
 * <p>An island's height is a concave cone: a gentle, sandy shore, jungle slopes, a grassy top and a crater, with
 * a shelf that drops away under the sea around it.
 */
public final class IslandMap {
    public static final int SEA_LEVEL = 63;
    // the island zones, as heights above the sea: beach, then jungle, then plains to the top
    public static final double BEACH_TOP = 3.0;
    public static final double JUNGLE_TOP = 40.0;

    // one archipelago at most per cell this many blocks across
    private static final int CELL = 1600;
    private static final float ARCHIPELAGO_CHANCE = 0.55F;
    // how far an archipelago's islands can reach from its centre, plus their underwater shelves
    private static final double MAX_REACH = 1100.0;
    // the shelf around an island reaches this far past the shore, as a fraction of its radius
    private static final double SHELF = 0.35;

    /** The climate at a point, the same values the overworld's noise router gives. */
    public interface ClimateProbe {
        double continentalness(int x, int z);

        double temperature(int x, int z);

        double humidity(int x, int z);
    }

    /** One volcano. Heights are blocks above the sea. */
    public record Island(int x, int z, double radius, double peak, double craterRadius, double craterDepth,
                         boolean active, double p1, double p2, double p3, double p4, long seed) {
        /** The rim of the crater, in blocks above the sea. */
        public double rimHeight() {
            return cone(this.craterRadius / this.radius, this.peak);
        }

        /** The middle of the crater floor, in blocks above the sea. */
        public double craterFloor() {
            return this.rimHeight() - this.craterDepth;
        }

        /** Height of the ground above the sea at a point, or NaN if this island doesn't reach it. */
        double height(double px, double pz) {
            double dx = px - this.x, dz = pz - this.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > this.radius * (1.2 + SHELF)) {
                return Double.NaN;
            }
            // a wavy, irregular shoreline
            double a = Math.atan2(dz, dx);
            double r = this.radius * (1.0 + 0.10 * Math.sin(3 * a + this.p1) + 0.06 * Math.sin(5 * a + this.p2)
                    + 0.04 * Math.sin(9 * a + this.p3));
            double t = d / r;
            double h;
            if (t >= 1.0) {
                // under the sea: the island's shelf slopes away to the sea floor
                if (t > 1.0 + SHELF) {
                    return Double.NaN;
                }
                h = -(t - 1.0) * r * 0.55;
            } else if (d < this.craterRadius) {
                // the crater: a bowl inside a slightly raised rim
                double rim = this.rimHeight();
                double f = d / this.craterRadius;
                h = rim - this.craterDepth * (1.0 - f * f);
            } else {
                h = cone(t, this.peak);
                // a raised lip round the crater
                double lip = (d - this.craterRadius) / 6.0;
                h += 3.0 * Math.exp(-lip * lip);
            }
            // gullies and knolls so the slopes aren't perfectly smooth
            double rough = 2.5 * Math.sin(px * 0.045 + this.p4) * Math.sin(pz * 0.05 + this.p1)
                    + 1.5 * Math.sin(px * 0.13 + this.p2) * Math.sin(pz * 0.11 + this.p3);
            return h + rough * (t < 1.0 ? Math.min(1.0, (1.0 - t) * 8.0) : 0.3);
        }

        // the cone: steep towards the top, flattening out into a gentle shore
        private static double cone(double t, double peak) {
            double u = Math.max(0.0, 1.0 - t);
            return peak * Math.pow(u, 2.2) + 18.0 * u;
        }
    }

    private static final Map<Long, IslandMap> MAPS = new ConcurrentHashMap<>();

    private final long seed;
    private final Map<Long, List<Island>> cells = new ConcurrentHashMap<>();
    // the last column asked about, per thread: noise asks for the same column many times over
    private final ThreadLocal<double[]> lastColumn = ThreadLocal.withInitial(() -> new double[]{Double.NaN, Double.NaN, Double.NaN});

    private IslandMap(long seed) {
        this.seed = seed;
    }

    public static IslandMap forSeed(long seed) {
        return MAPS.computeIfAbsent(seed, IslandMap::new);
    }

    /**
     * The layout seed, taken from the world's own temperature noise at a fixed point. The terrain, the biomes and
     * the structures all have that noise to hand, so they all arrive at the same seed.
     */
    public static long layoutSeed(double temperatureAtProbe) {
        long bits = Double.doubleToLongBits(temperatureAtProbe);
        return bits * 0x9E3779B97F4A7C15L ^ (bits >>> 29);
    }

    public static final int SEED_PROBE_X = 13_337;
    public static final int SEED_PROBE_Z = -27_011;

    /** Height of the island ground above the sea at a point, or NaN if no island is there. */
    public double height(int x, int z, ClimateProbe probe) {
        double[] last = this.lastColumn.get();
        if (last[0] == x && last[1] == z) {
            return last[2];
        }
        double best = Double.NaN;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int ox = cx + i, oz = cz + j;
                // skip cells too far away to reach this point
                double nearX = Mth.clamp(x, ox * CELL, ox * CELL + CELL), nearZ = Mth.clamp(z, oz * CELL, oz * CELL + CELL);
                if ((nearX - x) * (nearX - x) + (nearZ - z) * (nearZ - z) > MAX_REACH * MAX_REACH) {
                    continue;
                }
                for (Island island : this.islands(ox, oz, probe)) {
                    double h = island.height(x + 0.5, z + 0.5);
                    if (!Double.isNaN(h) && (Double.isNaN(best) || h > best)) {
                        best = h;
                    }
                }
            }
        }
        last[0] = x;
        last[1] = z;
        last[2] = best;
        return best;
    }

    /** The islands whose crater centres lie inside the given block square (inclusive). */
    public List<Island> islandsCentredIn(int minX, int minZ, int maxX, int maxZ, ClimateProbe probe) {
        List<Island> found = new ArrayList<>();
        for (int cx = Math.floorDiv(minX, CELL); cx <= Math.floorDiv(maxX, CELL); cx++) {
            for (int cz = Math.floorDiv(minZ, CELL); cz <= Math.floorDiv(maxZ, CELL); cz++) {
                for (Island island : this.islands(cx, cz, probe)) {
                    if (island.x() >= minX && island.x() <= maxX && island.z() >= minZ && island.z() <= maxZ) {
                        found.add(island);
                    }
                }
            }
        }
        return found;
    }

    private List<Island> islands(int cx, int cz, ClimateProbe probe) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        List<Island> cached = this.cells.get(key);
        if (cached != null) {
            return cached;
        }
        List<Island> made = this.makeArchipelago(cx, cz, probe);
        this.cells.put(key, made);
        return made;
    }

    private List<Island> makeArchipelago(int cx, int cz, ClimateProbe probe) {
        RandomSource random = RandomSource.create(this.seed ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x5DEECE66DL);
        if (random.nextFloat() >= ARCHIPELAGO_CHANCE) {
            return List.of();
        }
        // the archipelago's middle, well inside the cell
        int x = cx * CELL + 400 + random.nextInt(CELL - 800);
        int z = cz * CELL + 400 + random.nextInt(CELL - 800);
        if (!warmOpenOcean(probe, x, z)) {
            return List.of();
        }
        List<Island> islands = new ArrayList<>();
        // the main volcano: 300 to 600 blocks across, its top 100 to 150 blocks above the sea
        double radius = 150 + random.nextDouble() * 150;
        double peak = 100 + random.nextDouble() * 50;
        islands.add(island(random, x, z, radius, peak));
        // up to three smaller ones around it, with open water between
        int extra = random.nextInt(4);
        for (int i = 0; i < extra; i++) {
            double r = radius * (0.35 + random.nextDouble() * 0.35);
            double p = peak * (0.5 + random.nextDouble() * 0.35);
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = radius + r + 60 + random.nextDouble() * 160;
            int ix = x + (int) (Math.cos(angle) * dist), iz = z + (int) (Math.sin(angle) * dist);
            Island candidate = island(random, ix, iz, r, p);
            boolean clear = warmOpenOcean(probe, ix, iz);
            for (Island other : islands) {
                double gap = Math.hypot(other.x() - ix, other.z() - iz) - other.radius() - r;
                clear &= gap > 40;
            }
            if (clear) {
                islands.add(candidate);
            }
        }
        return List.copyOf(islands);
    }

    private static Island island(RandomSource random, int x, int z, double radius, double peak) {
        // about one volcano in four is still active: a deeper crater with lava in it
        boolean active = random.nextInt(4) == 0;
        double craterRadius = radius * (0.09 + random.nextDouble() * 0.04);
        double craterDepth = peak * (active ? 0.2 : 0.1);
        double tau = Math.PI * 2;
        return new Island(x, z, radius, peak, craterRadius, craterDepth, active,
                random.nextDouble() * tau, random.nextDouble() * tau, random.nextDouble() * tau, random.nextDouble() * tau,
                random.nextLong());
    }

    // Warm, humid ocean well away from any coast (so no river, beach or land nearby), and not the mushroom
    // islands' deep sea. These match the climate where the Köppen biome source makes warm ocean.
    private static boolean warmOpenOcean(ClimateProbe probe, int x, int z) {
        double c = probe.continentalness(x, z);
        return c > -0.95 && c < -0.35
                && probe.temperature(x, z) >= 0.2
                && probe.humidity(x, z) >= -0.1;
    }
}
