package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * How the Mediterranean (Csa) is divided between its land biomes.
 *
 * <ul>
 *   <li>The shrubland is the Mediterranean itself: it covers the whole Csa climate, as vast as the savanna, except
 *       where one of the patches below lies.</li>
 *   <li>Patches: the land is split into a coarse grid of {@link #CELL}-block cells, and about half of them hold one
 *       patch - a rounded, lobed blob some 140-260 blocks across - of Mediterranean forest, olive grove or farmland,
 *       or a smaller lavender field. Farmland only takes a patch whose middle lies by a river valley; otherwise that
 *       patch is forest.</li>
 * </ul>
 *
 * <p>Both the biome source ({@link KoppenBiomeSource}) and the terrain ({@link CsaHills}) ask this, so the flat
 * farmland and olive groves lie exactly under their biomes. Both pass the world's
 * temperature and ridges noise, from which the same per-world salt and river valleys follow.
 */
public final class CsaZones {
    public enum Zone { SHRUBLAND, FOREST, OLIVE_GROVE, FARMLAND, LAVENDER_FIELD }

    // the patch grid: one patch at most per cell
    static final int CELL = 448;
    // the share of cells holding a patch, and the patch radius (before its lobes)
    private static final double PATCH_CHANCE = 0.5, MIN_RADIUS = 70.0, EXTRA_RADIUS = 60.0;
    // farmland only takes a patch if the ridges noise in its middle is this close to a river valley (0)
    static final double FARMLAND_RIVER = 0.25;

    /**
     * The zone here and, inside a patch, how far (in blocks) it is in from its edge, and the patch's middle.
     * The shrubland's edge is 0.
     */
    public record Spot(Zone zone, double edge, int centreX, int centreZ) {
        static final Spot SHRUBLAND = new Spot(Zone.SHRUBLAND, 0.0, 0, 0);
    }

    private CsaZones() {
    }

    /** A salt for this world, the same wherever it's worked out: from its temperature noise at the origin. */
    public static long salt(DensityFunction temperature) {
        double t = temperature.compute(new DensityFunction.SinglePointContext(0, 0, 0));
        return Double.doubleToLongBits(Math.floor(t * 1.0E7)) * 0x9E3779B97F4A7C15L + 0x5DEECE66DL;
    }

    private static long hash(long salt, int cx, int cz, int k) {
        long h = salt ^ (cx * 0x632BE59BD9B4E019L) ^ (cz * 0x8CB92BA72F3D8DD7L) ^ (k * 0x9E3779B97F4A7C15L);
        h ^= h >>> 31;
        h *= 0x7FB5D329728EA185L;
        h ^= h >>> 27;
        h *= 0x81DADEF4BC2DD44DL;
        h ^= h >>> 33;
        return h;
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    public static Spot at(int x, int z, long salt, DensityFunction ridges) {
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        Spot best = Spot.SHRUBLAND;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = gx + dx, cz = gz + dz;
                if (unit(hash(salt, cx, cz, 0)) >= PATCH_CHANCE) continue;
                // the patch's middle, kept well inside its cell
                int px = (int) ((cx + 0.3 + 0.4 * unit(hash(salt, cx, cz, 1))) * CELL);
                int pz = (int) ((cz + 0.3 + 0.4 * unit(hash(salt, cx, cz, 2))) * CELL);
                double roll = unit(hash(salt, cx, cz, 3));
                Zone zone = roll < 0.35 ? Zone.FOREST : roll < 0.6 ? Zone.OLIVE_GROVE : roll < 0.85 ? Zone.FARMLAND : Zone.LAVENDER_FIELD;
                // lavender fields are the smaller patches
                double r = (MIN_RADIUS + EXTRA_RADIUS * unit(hash(salt, cx, cz, 4))) * (zone == Zone.LAVENDER_FIELD ? 0.55 : 1.0);
                double a = Math.atan2(z - pz, x - px);
                double p1 = unit(hash(salt, cx, cz, 5)) * Mth.TWO_PI, p2 = unit(hash(salt, cx, cz, 6)) * Mth.TWO_PI;
                double boundary = r * (1.0 + 0.22 * Math.sin(3 * a + p1) + 0.12 * Math.sin(5 * a + p2));
                double edge = boundary - Mth.length(x - px, z - pz);
                if (edge <= best.edge()) continue;
                if (zone == Zone.FARMLAND
                        && Math.abs(ridges.compute(new DensityFunction.SinglePointContext(px, 0, pz))) > FARMLAND_RIVER) {
                    zone = Zone.FOREST;
                }
                best = new Spot(zone, edge, px, pz);
            }
        }
        return best;
    }
}
