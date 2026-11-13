package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the Subtropical rice terraces (Cwa) are: great sweeps of paddies, a hundred times the area of the tropical
 * rice terraces (ten times their radius: 130 to 180 blocks), found only deep inside the Monsoon subtropics.
 *
 * <p>The world is cut into square cells of {@link #CELL} blocks. In some of them, the first spot found whose whole
 * patch - its middle, and nearly all of its edge - lies in the Monsoon subtropics becomes a site. Each cell
 * is worked out once, from the world's seed, and remembered.
 */
public final class CwaTerraceSites {
    public static final int CELL = 1024;
    private static final float CHANCE = 1.0F;
    private static final int TRIES = 60;
    private static final int MIN_RADIUS = 130;
    private static final int MAX_RADIUS = 180;
    private static final float WOBBLE = 0.18F;
    private static final int MARGIN = MAX_RADIUS + 32;
    private static final int REACH = (int) (MAX_RADIUS * (1.0F + WOBBLE)) + 4;

    public record Site(int centreX, int centreZ, int radius, float phase) {
        boolean covers(int x, int z) {
            int dx = x - this.centreX, dz = z - this.centreZ;
            float angle = (float) Mth.atan2(dz, dx);
            float edge = this.radius * (1.0F + WOBBLE * Mth.sin(3.0F * angle + this.phase)
                    + 0.06F * Mth.sin(7.0F * angle + 2.0F * this.phase));
            return (long) dx * dx + (long) dz * dz <= (long) (edge * edge);
        }
    }

    /** Whether the biome source would put the Monsoon subtropics at a surface position. */
    public interface CwaSurface {
        boolean isCwa(int x, int z);
    }

    private final long seed;
    private final ConcurrentHashMap<Long, Optional<Site>> cells = new ConcurrentHashMap<>();

    public CwaTerraceSites(long seed) {
        this.seed = seed;
    }

    public Optional<Site> site(int cellX, int cellZ, CwaSurface surface) {
        // (worked out outside the map's lock, so threads generating chunks never wait on each other; two threads
        // working out the same cell at once come to the same answer)
        long key = key(cellX, cellZ);
        Optional<Site> site = this.cells.get(key);
        if (site == null) {
            site = this.find(cellX, cellZ, surface);
            Optional<Site> raced = this.cells.putIfAbsent(key, site);
            if (raced != null) site = raced;
        }
        return site;
    }

    public boolean inTerraces(int x, int z, CwaSurface surface) {
        int cx0 = Math.floorDiv(x - REACH, CELL), cx1 = Math.floorDiv(x + REACH, CELL);
        int cz0 = Math.floorDiv(z - REACH, CELL), cz1 = Math.floorDiv(z + REACH, CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Optional<Site> site = this.site(cx, cz, surface);
                if (site.isPresent() && site.get().covers(x, z)) return true;
            }
        }
        return false;
    }

    private Optional<Site> find(int cellX, int cellZ, CwaSurface surface) {
        RandomSource random = RandomSource.create(this.seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0xC3A7L);
        if (random.nextFloat() >= CHANCE) return Optional.empty();
        for (int i = 0; i < TRIES; i++) {
            int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            if (!surface.isCwa(x, z)) continue;
            int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            // the middle of the patch wholly in the monsoon subtropics, and nearly all of its edge (the terraces
            // only ever take the monsoon subtropics' own ground, so a sliver past the edge is simply left out)
            boolean inside = true;
            int misses = 0;
            for (int d = 0; d < 12 && inside; d++) {
                float angle = d * Mth.TWO_PI / 12;
                float ux = Mth.cos(angle), uz = Mth.sin(angle);
                inside = surface.isCwa(x + Math.round(ux * radius * 0.55F), z + Math.round(uz * radius * 0.55F));
                if (!surface.isCwa(x + Math.round(ux * radius), z + Math.round(uz * radius))) misses++;
            }
            inside &= misses <= 2;
            if (inside) return Optional.of(new Site(x, z, radius, random.nextFloat() * Mth.TWO_PI));
        }
        return Optional.empty();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
