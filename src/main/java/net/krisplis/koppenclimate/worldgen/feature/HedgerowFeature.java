package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The English countryside's (Cfb) fields: its meadows divided into irregular fields by dry-stone walls of mossy
 * cobblestone (a stretch of plain cobblestone here and there), with a tall oak standing in one now and then - and
 * along some of the field edges a dirt track, beside the wall or on its own.
 *
 * <p>The fields are the cells of a jittered grid (a Voronoi pattern), the same for the whole world, worked out
 * column by column, so the hedges run on unbroken from chunk to chunk. Placed once per chunk, over the whole chunk.
 */
public class HedgerowFeature extends Feature<HedgerowFeature.Config> {
    /** The biome the hedges are laid in (they stop at its edge). */
    public record Config(Holder<Biome> biome) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("biome").forGetter(Config::biome)
        ).apply(i, Config::new));
    }

    // the fields: one to every FIELD x FIELD blocks, their corners jittered by up to JITTER of that
    private static final int FIELD = 34;
    private static final float JITTER = 0.8F;
    // how wide a hedge and a track are (blocks from the line between two fields)
    private static final double HEDGE = 0.7, TRACK = 2.2;
    // one hedge column in this many has a tall oak standing in it
    private static final int OAK_EVERY = 400;

    public HedgerowFeature(Codec<Config> codec) {
        super(codec);
    }

    private static long hash(long seed, long a, long b) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h;
    }

    /** A field's middle: its cell's corner, jittered. */
    private static double[] site(long seed, int i, int j) {
        long h = hash(seed, i, j);
        double fx = ((h & 0xFFFF) / 65535.0 - 0.5) * JITTER, fz = (((h >>> 16) & 0xFFFF) / 65535.0 - 0.5) * JITTER;
        return new double[]{(i + 0.5 + fx) * FIELD, (j + 0.5 + fz) * FIELD};
    }

    /** What lies along the edge between two fields: 0-5 a hedge, 6-7 a hedge with a track beside it, 8-9 a track. */
    private static int edgeKind(long seed, int ai, int aj, int bi, int bj) {
        long k1 = ((long) ai << 32) ^ (aj & 0xFFFFFFFFL), k2 = ((long) bi << 32) ^ (bj & 0xFFFFFFFFL);
        long lo = Math.min(k1, k2), hi = Math.max(k1, k2);
        return (int) Math.floorMod(hash(seed ^ 0x5DEECE66DL, lo, hi), 10L);
    }

    /** 0 nothing, 1 a wall, 2 a track, for column (x, z). */
    private static int what(long seed, int x, int z) {
        int ci = Math.floorDiv(x, FIELD), cj = Math.floorDiv(z, FIELD);
        double best = Double.MAX_VALUE, second = Double.MAX_VALUE;
        int bi = 0, bj = 0, si = 0, sj = 0;
        double[] bp = null, sp = null;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                double[] p = site(seed, ci + a, cj + b);
                double d = (p[0] - x - 0.5) * (p[0] - x - 0.5) + (p[1] - z - 0.5) * (p[1] - z - 0.5);
                if (d < best) {
                    second = best; si = bi; sj = bj; sp = bp;
                    best = d; bi = ci + a; bj = cj + b; bp = p;
                } else if (d < second) {
                    second = d; si = ci + a; sj = cj + b; sp = p;
                }
            }
        }
        if (sp == null) return 0;
        double gap = Math.sqrt((sp[0] - bp[0]) * (sp[0] - bp[0]) + (sp[1] - bp[1]) * (sp[1] - bp[1]));
        double off = (second - best) / (2.0 * gap);
        if (off >= TRACK) return 0;
        int kind = edgeKind(seed, bi, bj, si, sj);
        if (kind < 8 && off < HEDGE) return 1;
        boolean track = kind >= 8 ? off < 1.1 : kind >= 6 && off < TRACK && (bi < si || (bi == si && bj < sj));
        return track ? 2 : 0;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        Holder<Biome> biome = context.config().biome();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState track = Blocks.DIRT_PATH.defaultBlockState();
        boolean any = false;
        // no walls through villages (or any other building on the surface) here or next door
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                var chunk = level.getChunk((x0 >> 4) + cx, (z0 >> 4) + cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) continue;
                for (var e : chunk.getAllReferences().entrySet()) {
                    if (!e.getValue().isEmpty() && e.getKey().step() == net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES) {
                        return false;
                    }
                }
            }
        }
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int kind = what(seed, x, z);
                if (kind == 0) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (top < 63) continue;
                pos.set(x, top, z);
                if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;
                if (!level.getBiome(pos.setY(top + 1)).is(biome)) continue;
                BlockState above = level.getBlockState(pos.set(x, top + 1, z));
                if (!above.isAir() && !above.canBeReplaced()) continue;
                any = true;
                if (kind == 1) {
                    if (Math.floorMod(hash(seed, x, z), OAK_EVERY) == 0 && this.tallOak(level, context, pos.set(x, top + 1, z), random)) {
                        continue;
                    }
                    level.setBlock(pos.set(x, top + 1, z), wall(level, seed, x, top, z), Block.UPDATE_CLIENTS);
                } else {
                    if (!above.isAir()) level.setBlock(pos.set(x, top + 1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(pos.set(x, top, z), track, Block.UPDATE_CLIENTS);
                }
            }
        }
        return any;
    }

    /** The wall block for (x, top + 1, z), joined to the wall on each side that runs on at the same height. */
    private static BlockState wall(WorldGenLevel level, long seed, int x, int top, int z) {
        // mostly mossy, a stretch of plain cobblestone here and there
        boolean mossy = Math.floorMod(hash(seed ^ 0x51L, Math.floorDiv(x, 6), Math.floorDiv(z, 6)), 4) != 0;
        BlockState state = (mossy ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
        boolean n = joins(level, seed, x, z - 1, top), s = joins(level, seed, x, z + 1, top);
        boolean e = joins(level, seed, x + 1, z, top), w = joins(level, seed, x - 1, z, top);
        state = state.setValue(WallBlock.NORTH_WALL, n ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.SOUTH_WALL, s ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.EAST_WALL, e ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.WEST_WALL, w ? WallSide.LOW : WallSide.NONE);
        // a post, except along a straight run
        boolean straight = (n && s && !e && !w) || (e && w && !n && !s);
        return state.setValue(WallBlock.UP, !straight);
    }

    private static boolean joins(WorldGenLevel level, long seed, int x, int z, int top) {
        if (what(seed, x, z) != 1) return false;
        int t = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        // the wall there may already be built (it counts the ground, not the wall)
        if (level.getBlockState(new BlockPos(x, t, z)).getBlock() instanceof WallBlock) t--;
        return t == top;
    }

    /** A tall (vanilla's "fancy") oak standing in the hedge. */
    private boolean tallOak(WorldGenLevel level, FeaturePlaceContext<Config> context, BlockPos pos, RandomSource random) {
        Holder<ConfiguredFeature<?, ?>> oak = level.registryAccess().registry(Registries.CONFIGURED_FEATURE)
                .flatMap(r -> r.getHolder(TreeFeatures.FANCY_OAK)).orElse(null);
        if (oak == null) return false;
        BlockState above = level.getBlockState(pos);
        if (!above.isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        return oak.value().place(level, context.chunkGenerator(), random, pos.immutable());
    }

}
