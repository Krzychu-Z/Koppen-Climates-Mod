package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.AlpineTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Tirol village (Dfb) in an Alpine valley: see TirolVillagePiece. It finds the valley's flat floor and its stream
 * from the shape of the ground itself (the Alps' valley floor stands at a fixed height, AlpineTerrain.VALLEY_Y, and
 * the stream's bed a block or two below it), then strings its houses along both banks, each facing the water,
 * with the chapel a little back from the stream and a wooden bridge across it.
 */
public class TirolVillageStructure extends Structure {
    public static final MapCodec<TirolVillageStructure> CODEC = simpleCodec(TirolVillageStructure::new);

    // the ground is looked at on a grid this far round the middle, every STEP blocks
    static final int RADIUS = 60, STEP = 4, N = 2 * RADIUS / STEP + 1;
    static final int FLOOR = AlpineTerrain.VALLEY_Y + 1;
    // (a grid point on the stream's banks: neither floor to build on nor stream)
    static final int BANK = -1000;
    // houses stand this far (blocks) from the stream; the chapel a little further back
    static final int HOUSE_NEAR = 8, HOUSE_FAR = 18, CHAPEL_NEAR = 12, CHAPEL_FAR = 30;
    static final int MAX_HOUSES = 14, MIN_HOUSES = 3;
    // the first look: every LOOK_STEP blocks out to LOOK either way, at least MIN_LOOK spots on the floor
    static final int LOOK = 48, LOOK_STEP = 12, MIN_LOOK = 12;

    public TirolVillageStructure(StructureSettings settings) {
        super(settings);
    }

    /** One thing to build: a house, the chapel or the bridge, at (x, z) from the middle, facing a way. */
    record Plot(int kind, int x, int z, Direction facing, int size, int minX, int minZ, int maxX, int maxZ) {
        static final int HOUSE = 0, CHAPEL = 1, BRIDGE = 2;

        boolean overlaps(int x0, int z0, int x1, int z1, int margin) {
            return x0 <= this.maxX + margin && x1 >= this.minX - margin && z0 <= this.maxZ + margin && z1 >= this.minZ - margin;
        }
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heights = context.heightAccessor();
        RandomState randomState = context.randomState();
        // a first look round the chunk: is there valley floor (or stream) here at all? The valley is some 45-75
        // blocks across, so the look reaches well beyond the chunk, and the village is then moved onto the middle
        // of the floor found
        int x0 = chunk.getMiddleBlockX(), z0 = chunk.getMiddleBlockZ();
        // (cheaply, first: in the Alps at all? At the valley's height all the Alps are Alpine valley)
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x0), QuartPos.fromBlock(FLOOR),
                QuartPos.fromBlock(z0), randomState.sampler()))) return Optional.empty();
        Optional<AlpineTerrain> alps = AlpineTerrain.of(randomState);
        long sx = 0, sz = 0;
        int good = 0;
        for (int dx = -LOOK; dx <= LOOK; dx += LOOK_STEP) {
            for (int dz = -LOOK; dz <= LOOK; dz += LOOK_STEP) {
                int h = baseHeight(alps, generator, heights, randomState, x0 + dx, z0 + dz);
                if (h >= FLOOR - 3 && h <= FLOOR) {
                    good++;
                    sx += dx;
                    sz += dz;
                }
            }
        }
        if (good < MIN_LOOK) return Optional.empty();
        int cx = x0 + (int) Math.round((double) sx / good), cz = z0 + (int) Math.round((double) sz / good);
        // the middle must be in an Alpine valley
        var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(FLOOR),
                QuartPos.fromBlock(cz), randomState.sampler());
        if (!context.validBiome().test(biome)) return Optional.empty();
        // then the whole grid: the floor, the stream, and the rest (the mountainsides)
        int[][] h = new int[N][N];
        List<int[]> stream = new ArrayList<>();
        int floor = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int x = (i - N / 2) * STEP, z = (j - N / 2) * STEP;
                h[i][j] = baseHeight(alps, generator, heights, randomState, cx + x, cz + z);
                // the stream is cut into the flat floor afterwards (AlpineStreamFeature): mark where it will run,
                // and keep its banks clear of building
                if (h[i][j] == FLOOR && alps.isPresent()) {
                    if (alps.get().nearStream(cx + x, cz + z, 0.0)) h[i][j] = FLOOR - 1;
                    else if (alps.get().nearStream(cx + x, cz + z, 4.0)) h[i][j] = BANK;
                }
                if (h[i][j] == FLOOR) floor++;
                else if (h[i][j] >= FLOOR - 3 && h[i][j] < FLOOR) stream.add(new int[]{x, z});
            }
        }
        if (stream.size() < 4 || floor < N * N / 5) return Optional.empty();
        RandomSource random = context.random();
        List<Plot> plots = new ArrayList<>();
        // the bridge, over the stream nearest the middle, at right angles to it
        int[] mid = nearest(stream, 0, 0);
        double sxx = 0, szz = 0, sxz = 0;
        int n = 0;
        for (int[] s : stream) {
            int dx = s[0] - mid[0], dz = s[1] - mid[1];
            if (dx * dx + dz * dz > 24 * 24) continue;
            sxx += dx * dx;
            szz += dz * dz;
            sxz += dx * dz;
            n++;
        }
        // the stream runs mostly along x when its spread along x is the greater: the bridge then crosses along z
        boolean bridgeAlongZ = sxx >= szz;
        int halfLength = 4;
        for (int t = 4; t <= 16; t += STEP) {
            if (isFloor(h, mid[0] + (bridgeAlongZ ? 0 : t), mid[1] + (bridgeAlongZ ? t : 0))
                    && isFloor(h, mid[0] - (bridgeAlongZ ? 0 : t), mid[1] - (bridgeAlongZ ? t : 0))) {
                halfLength = t + 2;
                break;
            }
            halfLength = t + 2;
        }
        Direction bridgeAxis = bridgeAlongZ ? Direction.SOUTH : Direction.EAST;
        plots.add(new Plot(Plot.BRIDGE, mid[0], mid[1], bridgeAxis, halfLength,
                mid[0] - (bridgeAlongZ ? 2 : halfLength), mid[1] - (bridgeAlongZ ? halfLength : 2),
                mid[0] + (bridgeAlongZ ? 2 : halfLength), mid[1] + (bridgeAlongZ ? halfLength : 2)));
        // the floor cells near the stream, in random order
        List<int[]> spots = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (h[i][j] != FLOOR) continue;
                int x = (i - N / 2) * STEP, z = (j - N / 2) * STEP;
                int[] s = nearest(stream, x, z);
                double d = Math.sqrt((double) (s[0] - x) * (s[0] - x) + (double) (s[1] - z) * (s[1] - z));
                spots.add(new int[]{x, z, (int) Math.round(d), s[0], s[1]});
            }
        }
        for (int k = spots.size() - 1; k > 0; k--) {
            int r = random.nextInt(k + 1);
            int[] t = spots.get(k);
            spots.set(k, spots.get(r));
            spots.set(r, t);
        }
        // the chapel first, a little back from the water, then the houses along both banks
        for (int[] s : spots) {
            if (s[2] < CHAPEL_NEAR || s[2] > CHAPEL_FAR) continue;
            Plot p = this.tryPlot(h, plots, Plot.CHAPEL, s, 4, -6, 13, 3);
            if (p != null) {
                plots.add(p);
                break;
            }
        }
        int houses = 0;
        for (int[] s : spots) {
            if (houses >= MAX_HOUSES) break;
            if (s[2] < HOUSE_NEAR || s[2] > HOUSE_FAR) continue;
            Plot p = this.tryPlot(h, plots, Plot.HOUSE, s, 6, -3, 11, 2);
            if (p != null) {
                plots.add(p);
                houses++;
            }
        }
        if (houses < MIN_HOUSES) return Optional.empty();
        BlockPos centre = new BlockPos(cx, AlpineTerrain.VALLEY_Y, cz);
        long seed = random.nextLong();
        List<int[]> data = new ArrayList<>();
        for (Plot p : plots) data.add(new int[]{p.kind(), p.x(), p.z(), p.facing().get2DDataValue(), p.size()});
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new TirolVillagePiece(centre, seed, data))));
    }

    /**
     * A plot at a spot facing the stream (the way to its nearest stream sample), its footprint half-width across,
     * front (negative: the balcony and eaves) and back reach along: all on the flat floor, clear of the others.
     */
    private Plot tryPlot(int[][] h, List<Plot> plots, int kind, int[] s, int half, int front, int back, int margin) {
        int dx = s[3] - s[0], dz = s[4] - s[1];
        Direction facing = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        Direction right = facing.getCounterClockWise();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int u = -half; u <= half; u += Math.max(1, half / 2)) {
            for (int v = front; v <= back; v += 2) {
                int x = s[0] + u * right.getStepX() - v * facing.getStepX();
                int z = s[1] + u * right.getStepZ() - v * facing.getStepZ();
                if (!isFloor(h, x, z)) return null;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        for (Plot p : plots) if (p.overlaps(minX, minZ, maxX, maxZ, margin)) return null;
        return new Plot(kind, s[0], s[1], facing, half, minX, minZ, maxX, maxZ);
    }

    /**
     * The height of the first block of air above the ground (as the ocean floor heightmap has it): inside the Alps
     * straight from their terrain (quick), elsewhere from the game (which builds the column's whole density).
     */
    static int baseHeight(Optional<AlpineTerrain> alps, ChunkGenerator generator, LevelHeightAccessor heights,
                          RandomState randomState, int x, int z) {
        if (alps.isPresent()) {
            double surface = alps.get().surface(x, z);
            if (!Double.isNaN(surface)) return (int) Math.ceil(surface);
        }
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
    }

    static boolean isFloor(int[][] h, int x, int z) {
        int i = Math.round(x / (float) STEP) + N / 2, j = Math.round(z / (float) STEP) + N / 2;
        if (i < 0 || j < 0 || i >= N || j >= N) return false;
        return h[i][j] == FLOOR;
    }

    static int[] nearest(List<int[]> points, int x, int z) {
        int[] best = points.get(0);
        long bd = Long.MAX_VALUE;
        for (int[] p : points) {
            long d = (long) (p[0] - x) * (p[0] - x) + (long) (p[1] - z) * (p[1] - z);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.TIROL_VILLAGE.get();
    }
}
