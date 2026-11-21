package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pompeii's scorched ground, once per chunk: over every column of the chunk that is Pompeii's, the soil is burnt
 * to ash and lava rock - fields of basalt and blackstone, grey ash and tuff, glowing magma - and in great patches of
 * netherrack it burns, and never stops burning (fire on netherrack never goes out, and it never rains here). Here and
 * there a little lava lake glows in a basalt rim, and pillars of basalt stand up out of the ash.
 */
public class PompeiiGroundFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<Long, SimplexNoise> NOISE = new ConcurrentHashMap<>();
    // how deep the burnt crust goes
    private static final int CRUST = 3;

    public PompeiiGroundFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static SimplexNoise noise(long seed) {
        return NOISE.computeIfAbsent(seed, s -> new SimplexNoise(new LegacyRandomSource(s ^ 0x50A1E11L)));
    }

    private static int hash(int x, int z, int salt) {
        long h = x * 341873128712L + z * 132897987541L + salt * 6971L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) (h & 0x7FFFFFFF);
    }

    static boolean isSoil(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.PLANKS);
    }

    static boolean inPompeii(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomes.POMPEII);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        SimplexNoise noise = noise(level.getSeed());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState fire = Blocks.FIRE.defaultBlockState(), netherrack = Blocks.NETHERRACK.defaultBlockState();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (!inPompeii(level, pos.set(x, top, z))) continue;
                // anything still growing here burnt away
                int floor = top - 24;
                while (top > floor && !isSoil(level.getBlockState(pos.set(x, top, z)))
                        && level.getBlockState(pos).getFluidState().isEmpty()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    top--;
                }
                if (!isSoil(level.getBlockState(pos.set(x, top, z)))) continue;
                any = true;
                double big = noise.getValue(x / 22.0, z / 22.0), small = noise.getValue(x / 5.0 + 300, z / 5.0 - 300);
                double n = big * 0.75 + small * 0.25;
                int r = hash(x, z, 1) % 100;
                boolean burning;
                BlockState surface;
                if (n > 0.28 || r < 9) {
                    // the fire fields
                    surface = r % 11 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : netherrack;
                    burning = r % 11 != 0 && hash(x, z, 2) % 100 < 70;
                } else if (n > -0.05) {
                    surface = r < 60 ? Blocks.BASALT.defaultBlockState() : r < 80 ? Blocks.SMOOTH_BASALT.defaultBlockState()
                            : r < 95 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                    burning = false;
                } else if (n > -0.4) {
                    // ash
                    surface = r < 45 ? Blocks.COARSE_DIRT.defaultBlockState() : r < 75 ? Blocks.GRAVEL.defaultBlockState()
                            : r < 92 ? Blocks.TUFF.defaultBlockState() : Blocks.BASALT.defaultBlockState();
                    burning = false;
                } else {
                    surface = r < 55 ? Blocks.TUFF.defaultBlockState() : r < 85 ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState();
                    burning = false;
                }
                level.setBlock(pos.set(x, top, z), surface, 2);
                for (int y = top - 1; y >= top - CRUST; y--) {
                    BlockState below = level.getBlockState(pos.set(x, y, z));
                    if (below.is(BlockTags.DIRT) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL) || below.is(Blocks.CLAY)) {
                        level.setBlock(pos, y == top - 1 && surface.is(Blocks.NETHERRACK) ? netherrack
                                : hash(x * 7 + y, z, 3) % 3 == 0 ? Blocks.BASALT.defaultBlockState() : Blocks.TUFF.defaultBlockState(), 2);
                    }
                }
                if (burning && level.getBlockState(pos.set(x, top + 1, z)).isAir()) level.setBlock(pos, fire, 2);
            }
        }
        if (!any) return false;
        RandomSource random = RandomSource.create(level.getSeed() ^ ((long) x0 * 3418731287L) ^ ((long) z0 * 1328979875L) ^ 0x7E11L);
        if (random.nextInt(5) == 0) this.lavaLake(level, x0 + 5 + random.nextInt(6), z0 + 5 + random.nextInt(6), random);
        int pillars = random.nextInt(3);
        for (int i = 0; i < pillars; i++) this.pillar(level, x0 + 1 + random.nextInt(14), z0 + 1 + random.nextInt(14), random);
        return true;
    }

    /** A little lava lake, two blocks deep in the middle, in a rim of basalt and magma. */
    private void lavaLake(WorldGenLevel level, int cx, int cz, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        if (level.getBlockState(pos.set(cx, cy, cz)).is(Blocks.FIRE)) cy--;
        double radius = 2.0 + random.nextDouble() * 2.2;
        int reach = (int) Math.ceil(radius) + 1;
        // only on level ground, all of it Pompeii's
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > reach * reach) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + dx, cz + dz) - 1;
                if (level.getBlockState(pos.set(cx + dx, top, cz + dz)).is(Blocks.FIRE)) top--;
                if (Math.abs(top - cy) > 1 || !inPompeii(level, pos)) return;
                if (!level.getBlockState(pos).getFluidState().isEmpty()) return;
            }
        }
        double phase = random.nextDouble() * Math.PI * 2;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double a = Math.atan2(dz, dx), d = Math.sqrt(dx * dx + dz * dz);
                double edge = radius * (1.0 + 0.18 * Math.sin(a * 3 + phase));
                int x = cx + dx, z = cz + dz;
                if (d <= edge) {
                    int depth = d < edge * 0.5 ? 2 : 1;
                    for (int y = cy + 3; y > cy; y--) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    for (int y = cy; y > cy - depth; y--) level.setBlock(pos.set(x, y, z), Blocks.LAVA.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, cy - depth, z), random.nextInt(3) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : Blocks.BASALT.defaultBlockState(), 2);
                    // and nothing open beside it, for the lava to run off into
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        for (int y = cy; y > cy - depth; y--) {
                            pos.set(x + side.getStepX(), y, z + side.getStepZ());
                            BlockState next = level.getBlockState(pos);
                            if (!next.is(Blocks.LAVA) && !next.blocksMotion()) {
                                double nd = Math.sqrt((dx + side.getStepX()) * (dx + side.getStepX()) + (dz + side.getStepZ()) * (dz + side.getStepZ()));
                                if (nd > edge) level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 2);
                            }
                        }
                    }
                } else if (d <= edge + 1.3) {
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    if (level.getBlockState(pos.set(x, top, z)).is(Blocks.FIRE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        top--;
                    }
                    if (top >= cy - 1 && top <= cy + 1) {
                        level.setBlock(pos.set(x, top, z), random.nextInt(4) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                : random.nextInt(3) == 0 ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.BASALT.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** A pillar of basalt standing up out of the ash, sometimes burning at its top. */
    private void pillar(WorldGenLevel level, int x, int z, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (level.getBlockState(pos.set(x, top, z)).is(Blocks.FIRE)) top--;
        BlockState ground = level.getBlockState(pos.set(x, top, z));
        if (!isSoil(ground) || !inPompeii(level, pos)) return;
        int height = 1 + random.nextInt(5);
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int y = 1; y <= height; y++) level.setBlock(pos.set(x, top + y, z), basalt, 2);
        // a wider foot to the taller ones
        if (height >= 3) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(3) == 0) continue;
                int sx = x + side.getStepX(), sz = z + side.getStepZ();
                int st = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sx, sz) - 1;
                if (level.getBlockState(pos.set(sx, st, sz)).is(Blocks.FIRE)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    st--;
                }
                if (Math.abs(st - top) > 1) continue;
                for (int y = st + 1; y <= top + 1 + random.nextInt(2); y++) level.setBlock(pos.set(sx, y, sz), basalt, 2);
            }
        }
        if (random.nextInt(3) == 0) {
            level.setBlock(pos.set(x, top + height, z), Blocks.NETHERRACK.defaultBlockState(), 2);
            level.setBlock(pos.set(x, top + height + 1, z), Blocks.FIRE.defaultBlockState(), 2);
        }
    }
}
