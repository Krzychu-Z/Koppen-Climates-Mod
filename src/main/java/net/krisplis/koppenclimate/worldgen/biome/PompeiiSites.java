package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where Pompeii burns: a rare, ever-burning Roman town on the even ground of the Mediterranean (Csa).
 *
 * <p>The world is cut into square cells of {@link #CELL} blocks. In some of them, the first spot found with the
 * Mediterranean's level land all around it becomes Pompeii: a round patch of {@link #RADIUS} blocks there (wherever
 * it is Mediterranean land) becomes the Pompeii biome - red sky, scorched ground - and the town is built in the
 * middle (PompeiiStructure). Each cell is worked out once, from the world's seed, and remembered.
 */
public final class PompeiiSites {
    public static final int CELL = 1024;
    private static final float CHANCE = 0.5F;
    private static final int TRIES = 24;
    private static final int MARGIN = 160;
    public static final int RADIUS = 96;

    public record Site(int x, int z) {
        boolean covers(int bx, int bz) {
            int dx = bx - this.x, dz = bz - this.z;
            return dx * dx + dz * dz <= RADIUS * RADIUS;
        }
    }

    /** Whether the biome source puts the Mediterranean's level land at a surface position. */
    public interface CsaSurface {
        boolean isCsa(int x, int z);
    }

    private final long seed;
    private final ConcurrentHashMap<Long, Optional<Site>> cells = new ConcurrentHashMap<>();

    public PompeiiSites(long seed) {
        this.seed = seed;
    }

    public Optional<Site> site(int cellX, int cellZ, CsaSurface csa) {
        Optional<Site> known = this.cells.get(key(cellX, cellZ));
        if (known != null) return known;
        Optional<Site> found = this.find(cellX, cellZ, csa);
        this.cells.putIfAbsent(key(cellX, cellZ), found);
        return found;
    }

    /** Whether the block column lies in Pompeii's burnt land. */
    public boolean inPompeii(int x, int z, CsaSurface csa) {
        int cx0 = Math.floorDiv(x - RADIUS, CELL), cx1 = Math.floorDiv(x + RADIUS, CELL);
        int cz0 = Math.floorDiv(z - RADIUS, CELL), cz1 = Math.floorDiv(z + RADIUS, CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                // a cell's town lies MARGIN inside it: skip cells whose inside is too far away
                int nx = Mth.clamp(x, cx * CELL + MARGIN, (cx + 1) * CELL - MARGIN), nz = Mth.clamp(z, cz * CELL + MARGIN, (cz + 1) * CELL - MARGIN);
                if ((long) (nx - x) * (nx - x) + (long) (nz - z) * (nz - z) > (long) RADIUS * RADIUS) continue;
                Optional<Site> site = this.site(cx, cz, csa);
                if (site.isPresent() && site.get().covers(x, z)) return true;
            }
        }
        return false;
    }

    private Optional<Site> find(int cellX, int cellZ, CsaSurface csa) {
        RandomSource random = RandomSource.create(this.seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x79A3L);
        if (random.nextFloat() >= CHANCE) return Optional.empty();
        for (int i = 0; i < TRIES; i++) {
            int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            if (!csa.isCsa(x, z)) continue;
            boolean all = true;
            // the town itself (some 60 blocks across with its margin) wholly on level Mediterranean land, and most
            // of the burnt land round it too
            for (int d = 0; d < 12 && all; d++) {
                float a = d * Mth.TWO_PI / 12;
                all = csa.isCsa(x + Math.round(Mth.cos(a) * 30), z + Math.round(Mth.sin(a) * 30))
                        && csa.isCsa(x + Math.round(Mth.cos(a) * 56), z + Math.round(Mth.sin(a) * 56));
            }
            if (all) return Optional.of(new Site(x, z));
        }
        return Optional.empty();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
