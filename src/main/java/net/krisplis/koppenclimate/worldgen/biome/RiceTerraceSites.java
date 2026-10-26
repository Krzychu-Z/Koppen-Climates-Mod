package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the Rice Terraces (Aw) are: rare little patches of hillside paddies, each by a river through the
 * monsoon forest, where a Thai village stands on the water.
 *
 * <p>The world is cut into square cells of {@link #CELL} blocks. In about half of them, the first spot found
 * on an Amazon jungle river that has monsoon forest on one bank becomes a site: the village goes on the river
 * there, and the terraces cover a roundish patch of the monsoon forest just beyond it, at most about five and
 * a half chunks in area. Each cell is worked out once, from the world's seed, and remembered.
 */
public final class RiceTerraceSites {
    public static final int CELL = 384;
    private static final float CHANCE = 0.5F;
    private static final int TRIES = 20;
    // keep spots this far inside their cell, so a site's terraces and village stay near it
    private static final int MARGIN = 64;
    // the village's lagoon and huts take this much room on the river before the terraces begin
    public static final int VILLAGE_ROOM = 24;
    private static final int MIN_RADIUS = 13;
    private static final int MAX_RADIUS = 18;
    // the patch's edge wobbles in and out by this fraction of its radius
    private static final float WOBBLE = 0.15F;
    private static final int REACH = MAX_RADIUS + 4;

    /** The village's spot on the river, and the terraces' centre, radius and edge wobble phase. */
    public record Site(int riverX, int riverZ, int centreX, int centreZ, int radius, float phase) {
        boolean covers(int x, int z) {
            int dx = x - this.centreX, dz = z - this.centreZ;
            float angle = (float) Mth.atan2(dz, dx);
            float edge = this.radius * (1.0F + WOBBLE * Mth.sin(3.0F * angle + this.phase));
            return dx * dx + dz * dz <= edge * edge;
        }
    }

    /** What the biome source would put at a surface position, before any terraces. */
    public interface SurfaceBiomes {
        boolean isRiver(int x, int z);

        boolean isMonsoon(int x, int z);
    }

    private final long seed;
    private final ConcurrentHashMap<Long, Optional<Site>> cells = new ConcurrentHashMap<>();

    public RiceTerraceSites(long seed) {
        this.seed = seed;
    }

    public Optional<Site> site(int cellX, int cellZ, SurfaceBiomes biomes) {
        return this.cells.computeIfAbsent(key(cellX, cellZ), k -> this.find(cellX, cellZ, biomes));
    }

    /** Whether the block column lies in some site's terraces. */
    public boolean inTerraces(int x, int z, SurfaceBiomes biomes) {
        int cx0 = Math.floorDiv(x - REACH, CELL), cx1 = Math.floorDiv(x + REACH, CELL);
        int cz0 = Math.floorDiv(z - REACH, CELL), cz1 = Math.floorDiv(z + REACH, CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Optional<Site> site = this.site(cx, cz, biomes);
                if (site.isPresent() && site.get().covers(x, z)) return true;
            }
        }
        return false;
    }

    private Optional<Site> find(int cellX, int cellZ, SurfaceBiomes biomes) {
        RandomSource random = RandomSource.create(this.seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x5EEDL);
        if (random.nextFloat() >= CHANCE) return Optional.empty();
        for (int i = 0; i < TRIES; i++) {
            int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            if (!biomes.isRiver(x, z)) continue;
            int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            float start = random.nextFloat() * Mth.TWO_PI;
            // a bank with monsoon forest all the way out to where the terraces will be
            for (int d = 0; d < 8; d++) {
                float angle = start + d * Mth.TWO_PI / 8;
                float ux = Mth.cos(angle), uz = Mth.sin(angle);
                int reach = VILLAGE_ROOM + radius;
                if (biomes.isMonsoon(x + Math.round(ux * 12), z + Math.round(uz * 12))
                        && biomes.isMonsoon(x + Math.round(ux * reach), z + Math.round(uz * reach))
                        && biomes.isMonsoon(x + Math.round(ux * (reach + radius)), z + Math.round(uz * (reach + radius)))) {
                    return Optional.of(new Site(x, z, x + Math.round(ux * reach), z + Math.round(uz * reach), radius,
                            random.nextFloat() * Mth.TWO_PI));
                }
            }
        }
        return Optional.empty();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
