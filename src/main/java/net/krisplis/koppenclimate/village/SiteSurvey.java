package net.krisplis.koppenclimate.village;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * The survey the big single-piece structures (the mansion, the castle, the estate, the cathedral, the French
 * village) make of a candidate site before taking it - done cheaply, because /locate (and world generation) runs it
 * for thousands of candidate chunks, and every surface height costs the generator a column of terrain noise:
 * <ol>
 *   <li>first the biome at every probe point (cheap): a core point in the wrong biome, or too many others, and the
 *       site's rejected before a single height is worked out;</li>
 *   <li>then the ground's height at the probes - the core first, then the farthest out, where the land differs most -
 *       giving up the moment it's under the sea or the highest and lowest are further apart than allowed;</li>
 *   <li>only for a site that's passed all that, a second look at each probe for standing water.</li>
 * </ol>
 */
final class SiteSurvey {
    private SiteSurvey() {
    }

    /**
     * Surveys the probes (in the structure's own frame, turned {@code turn}, about the chunk's middle).
     *
     * @return the ground heights at the probes, sorted; or null if the site won't do
     */
    @Nullable
    static int[] survey(Structure.GenerationContext context, int turn, int[][] probes, int core, int maxBad, int maxSpread) {
        int cx = context.chunkPos().getMiddleBlockX(), cz = context.chunkPos().getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        RandomState randomState = context.randomState();
        int sea = generator.getSeaLevel();
        int n = probes.length;
        int[] xs = new int[n], zs = new int[n];
        int bad = 0;
        for (int i = 0; i < n; i++) {
            xs[i] = cx + CathedralPiece.rx(turn, probes[i][0], probes[i][1]);
            zs[i] = cz + CathedralPiece.rz(turn, probes[i][0], probes[i][1]);
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(xs[i]), QuartPos.fromBlock(sea + 8), QuartPos.fromBlock(zs[i]),
                    randomState.sampler());
            if (!context.validBiome().test(biome) && (i == core || ++bad > maxBad)) return null;
        }
        // the core, then the rest from the farthest in
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        int px = probes[core][0], pz = probes[core][1];
        Arrays.sort(order, (a, b) -> {
            if (a == core) return -1;
            if (b == core) return 1;
            long da = sq(probes[a][0] - px) + sq(probes[a][1] - pz), db = sq(probes[b][0] - px) + sq(probes[b][1] - pz);
            return Long.compare(db, da);
        });
        int[] h = new int[n];
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i : order) {
            int floor = generator.getFirstOccupiedHeight(xs[i], zs[i], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), randomState);
            if (floor <= sea) return null;
            min = Math.min(min, floor);
            max = Math.max(max, floor);
            if (max - min > maxSpread) return null;
            h[i] = floor;
        }
        for (int i = 0; i < n; i++) {
            int surface = generator.getFirstOccupiedHeight(xs[i], zs[i], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), randomState);
            if (surface != h[i]) return null;
        }
        Arrays.sort(h);
        return h;
    }

    private static long sq(long v) {
        return v * v;
    }
}
