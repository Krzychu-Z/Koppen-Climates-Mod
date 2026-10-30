package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the ghost towns are: rare, abandoned Wild West towns out in the Lush desert (BSh).
 *
 * <p>The world is cut into square cells of {@link #CELL} blocks. In some of them, the first spot found with the Lush
 * desert all around it becomes a ghost town: a round patch of {@link #RADIUS} blocks there becomes the ghost town
 * biome (the yellow sky), and the town is built in the middle (GhostTownStructure). Each cell is worked out once,
 * from the world's seed, and remembered.
 */
public final class GhostTownSites {
    public static final int CELL = 640;
    private static final float CHANCE = 0.6F;
    private static final int TRIES = 24;
    private static final int MARGIN = 112;
    public static final int RADIUS = 72;

    public record Site(int x, int z) {
        boolean covers(int bx, int bz) {
            int dx = bx - this.x, dz = bz - this.z;
            return dx * dx + dz * dz <= RADIUS * RADIUS;
        }
    }

    /** Whether the biome source puts the Lush desert at a surface position. */
    public interface LushSurface {
        boolean isLush(int x, int z);
    }

    private final long seed;
    private final ConcurrentHashMap<Long, Optional<Site>> cells = new ConcurrentHashMap<>();

    public GhostTownSites(long seed) {
        this.seed = seed;
    }

    public Optional<Site> site(int cellX, int cellZ, LushSurface lush) {
        Optional<Site> known = this.cells.get(key(cellX, cellZ));
        if (known != null) return known;
        Optional<Site> found = this.find(cellX, cellZ, lush);
        this.cells.putIfAbsent(key(cellX, cellZ), found);
        return found;
    }

    /** Whether the block column lies in some ghost town. */
    public boolean inTown(int x, int z, LushSurface lush) {
        int cx0 = Math.floorDiv(x - RADIUS, CELL), cx1 = Math.floorDiv(x + RADIUS, CELL);
        int cz0 = Math.floorDiv(z - RADIUS, CELL), cz1 = Math.floorDiv(z + RADIUS, CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                // a cell's town lies MARGIN inside it: skip cells whose inside is too far away
                int nx = Mth.clamp(x, cx * CELL + MARGIN, (cx + 1) * CELL - MARGIN), nz = Mth.clamp(z, cz * CELL + MARGIN, (cz + 1) * CELL - MARGIN);
                if ((long) (nx - x) * (nx - x) + (long) (nz - z) * (nz - z) > (long) RADIUS * RADIUS) continue;
                Optional<Site> site = this.site(cx, cz, lush);
                if (site.isPresent() && site.get().covers(x, z)) return true;
            }
        }
        return false;
    }

    private Optional<Site> find(int cellX, int cellZ, LushSurface lush) {
        RandomSource random = RandomSource.create(this.seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x6057L);
        if (random.nextFloat() >= CHANCE) return Optional.empty();
        for (int i = 0; i < TRIES; i++) {
            int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            if (!lush.isLush(x, z)) continue;
            boolean all = true;
            for (int d = 0; d < 8 && all; d++) {
                float a = d * Mth.TWO_PI / 8;
                all = lush.isLush(x + Math.round(Mth.cos(a) * RADIUS), z + Math.round(Mth.sin(a) * RADIUS))
                        && lush.isLush(x + Math.round(Mth.cos(a) * 28), z + Math.round(Mth.sin(a) * 28));
            }
            if (all) return Optional.of(new Site(x, z));
        }
        return Optional.empty();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
