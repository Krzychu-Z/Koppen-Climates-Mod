package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
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
 * The Cold lakeland's lakes: round lakes 10-24 blocks across, sunk into flat ground a block below its lowest bank,
 * a bowl deepest in the middle (3-5 blocks), sand at the shore and gravel, clay and mud further out, and lily pads
 * here and there near the edge. Only on flat ground, so the water never stands above a bank.
 */
public class RoundLakeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEA_LEVEL = 63, MAX_RISE = 4;

    public RoundLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int ox = origin.getX(), oz = origin.getZ();
        double radius = 5.0 + random.nextInt(8);
        double phase = random.nextDouble() * Math.PI * 2, wobble = 0.08 + random.nextDouble() * 0.08;
        int maxDepth = 3 + random.nextInt(3);
        // the banks: the ground's top all round, just outside the lake
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            double r = radius * this.shape(a, phase, wobble) + 2.0;
            int top = this.top(level, ox + (int) Math.round(Math.cos(a) * r), oz + (int) Math.round(Math.sin(a) * r));
            // never into another lake, a river or the sea
            if (!level.getBlockState(new BlockPos(ox + (int) Math.round(Math.cos(a) * r), top,
                    oz + (int) Math.round(Math.sin(a) * r))).getFluidState().isEmpty()) return false;
            low = Math.min(low, top);
            high = Math.max(high, top);
        }
        int centre = this.top(level, ox, oz);
        if (low < SEA_LEVEL || high - low > MAX_RISE || centre > low + MAX_RISE) return false;
        int water = low - 1;
        int reach = (int) Math.ceil(radius * (1.0 + wobble) + 3.0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                double edge = radius * this.shape(Math.atan2(dz, dx), phase, wobble);
                double q = d / edge;
                int x = ox + dx, z = oz + dz;
                int top = this.top(level, x, z);
                if (q > 1.0) {
                    // the bank: built up where the ground dips to the water's level, so the lake holds
                    if (q <= 1.35 && top <= water) {
                        for (int y = top + 1; y < water; y++) level.setBlock(pos.set(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                        level.setBlock(pos.set(x, water, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    }
                    continue;
                }
                int depth = Math.max(1, (int) Math.round(1.0 + (maxDepth - 1) * (1.0 - q * q)));
                int floor = water - depth;
                // clear the ground above the water, then the water, then the lake bed
                for (int y = top + 3; y > water; y--) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                for (int y = water; y > floor; y--) level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(pos.set(x, floor, z), this.bed(q, random), 2);
                if (level.getBlockState(pos.set(x, floor - 1, z)).getFluidState().isEmpty()
                        && !level.getBlockState(pos).isAir()) continue;
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }
        // lily pads near the shore
        int pads = random.nextInt(6);
        for (int i = 0; i < pads; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius * this.shape(a, phase, wobble) * (0.6 + 0.3 * random.nextDouble());
            pos.set(ox + (int) Math.round(Math.cos(a) * r), water + 1, oz + (int) Math.round(Math.sin(a) * r));
            if (level.getBlockState(pos.below()).is(Blocks.WATER) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.LILY_PAD.defaultBlockState(), 2);
            }
        }
        return true;
    }

    /** The lake's outline: a circle, a little wavy. */
    private double shape(double angle, double phase, double wobble) {
        return 1.0 + wobble * Math.sin(angle * 3.0 + phase) * 0.7 + wobble * 0.3 * Math.sin(angle * 5.0 - phase);
    }

    /** The top block's height in this column (the ground or a lake already there). */
    private int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    private BlockState bed(double q, RandomSource random) {
        if (q > 0.8) return random.nextInt(4) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
        int r = random.nextInt(10);
        if (r < 4) return Blocks.GRAVEL.defaultBlockState();
        if (r < 6) return Blocks.CLAY.defaultBlockState();
        if (r < 8) return Blocks.MUD.defaultBlockState();
        return Blocks.DIRT.defaultBlockState();
    }
}
