package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Tundra's (ET) warm geothermal pools: small, roundish hot springs 5-9 blocks across, sunk a block below their
 * lowest bank, 2-3 blocks deep, with a bed of black sand, gravel and basalt and one to three geothermal vents in the
 * middle steaming and bubbling up through the water (bathe in one and it warms you through). A rim of black sand,
 * gravel and mossy stones about them. Only on fairly flat ground, never into another pool, a river or the sea.
 */
public class GeothermalPoolFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEA_LEVEL = 63, MAX_RISE = 2;

    public GeothermalPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int ox = context.origin().getX(), oz = context.origin().getZ();
        double radius = 2.5 + random.nextDouble() * 2.0;
        double phase = random.nextDouble() * Math.PI * 2;
        int maxDepth = 2 + random.nextInt(2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI * 2 / 16;
            double r = radius * shape(a, phase) + 1.5;
            int x = ox + (int) Math.round(Math.cos(a) * r), z = oz + (int) Math.round(Math.sin(a) * r);
            int top = top(level, x, z);
            if (!level.getBlockState(pos.set(x, top, z)).getFluidState().isEmpty()) return false;
            low = Math.min(low, top);
            high = Math.max(high, top);
        }
        int centre = top(level, ox, oz);
        if (low <= SEA_LEVEL || high - low > MAX_RISE || centre > low + MAX_RISE) return false;
        int water = low - 1;
        int reach = (int) Math.ceil(radius * 1.2 + 2.0);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double q = Math.sqrt(dx * dx + dz * dz) / (radius * shape(Math.atan2(dz, dx), phase));
                int x = ox + dx, z = oz + dz;
                int top = top(level, x, z);
                if (q > 1.0) {
                    // the rim: built up to hold the water where the ground dips, and dressed with sand and stones
                    if (q > 1.6) continue;
                    if (top <= water) {
                        for (int y = top + 1; y <= water; y++) level.setBlock(pos.set(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                        top = water;
                    }
                    if (random.nextInt(q < 1.3 ? 2 : 5) == 0) {
                        level.setBlock(pos.set(x, top, z), this.rim(random), 2);
                        BlockState above = level.getBlockState(pos.set(x, top + 1, z));
                        if (!above.isAir() && above.canBeReplaced()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    continue;
                }
                int depth = Math.max(1, (int) Math.round(maxDepth * (1.0 - q * q * 0.7)));
                int floor = water - depth;
                for (int y = top + 3; y > water; y--) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                for (int y = water; y > floor; y--) level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(pos.set(x, floor, z), this.bed(q, random), 2);
                if (!level.getBlockState(pos.set(x, floor - 1, z)).isSolid()) {
                    level.setBlock(pos, Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
                }
            }
        }
        // the vents, in the deepest middle
        int vents = 1 + random.nextInt(3);
        for (int i = 0; i < vents; i++) {
            int x = ox + (i == 0 ? 0 : random.nextInt(3) - 1), z = oz + (i == 0 ? 0 : random.nextInt(3) - 1);
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            if (y < water - maxDepth - 1 || !level.getBlockState(pos.set(x, y + 1, z)).is(Blocks.WATER)) continue;
            level.setBlock(pos.set(x, y, z), ModBlocks.GEOTHERMAL_VENT.get().defaultBlockState(), 2);
            level.setBlock(pos.set(x, y - 1, z), Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
        }
        return true;
    }

    private static double shape(double angle, double phase) {
        return 1.0 + 0.12 * Math.sin(angle * 3.0 + phase) + 0.06 * Math.sin(angle * 5.0 - phase);
    }

    private static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
    }

    private BlockState bed(double q, RandomSource random) {
        int r = random.nextInt(10);
        if (q < 0.45) return r < 6 ? Blocks.SMOOTH_BASALT.defaultBlockState() : ModBlocks.BLACK_SAND.get().defaultBlockState();
        if (r < 5) return ModBlocks.BLACK_SAND.get().defaultBlockState();
        if (r < 8) return Blocks.GRAVEL.defaultBlockState();
        return Blocks.TUFF.defaultBlockState();
    }

    private BlockState rim(RandomSource random) {
        int r = random.nextInt(10);
        if (r < 4) return ModBlocks.BLACK_SAND.get().defaultBlockState();
        if (r < 7) return Blocks.GRAVEL.defaultBlockState();
        if (r < 9) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        return Blocks.TUFF.defaultBlockState();
    }
}
