package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A field of the Mediterranean farmland: an irregular, lobed patch of tilled ground some 12-28 blocks across, sown
 * with wheat or barley in strips, with irrigation channels running through it every few rows so all of it stays
 * watered. Only the field's level ground is tilled: any column that isn't at the field's own height, isn't soil or
 * isn't open to the sky is left as it is.
 */
public class CropFieldFeature extends Feature<NoneFeatureConfiguration> {
    // irrigation channels every this many blocks (farmland within 4 blocks of water stays wet)
    private static final int CHANNEL_SPACING = 9;

    public CropFieldFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int y = origin.getY() - 1;   // the ground under the heightmap spot
        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) return false;
        // the outline: a radius that swells and shrinks with direction, so no two fields look alike
        float radius = 6.0F + random.nextFloat() * 8.0F;
        float[] lobes = {random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI};
        float stretch = 0.7F + random.nextFloat() * 0.6F;   // fields are often longer one way than the other
        boolean alongX = random.nextBoolean();
        // two crops, in strips across the field
        BlockState first = random.nextBoolean() ? Blocks.WHEAT.defaultBlockState() : ModBlocks.BARLEY.get().defaultBlockState();
        BlockState second = random.nextFloat() < 0.4F ? (first.is(Blocks.WHEAT) ? ModBlocks.BARLEY.get().defaultBlockState()
                : Blocks.WHEAT.defaultBlockState()) : first;
        int ripeness = 3 + random.nextInt(5);
        int reach = Mth.ceil(radius * 1.5F) + 1;
        int channelOffset = random.nextInt(CHANNEL_SPACING);
        boolean placed = false;
        // a feature may only touch its own chunk and the ones right around it
        int minX = (origin.getX() & ~15) - 16, maxX = (origin.getX() & ~15) + 31;
        int minZ = (origin.getZ() & ~15) - 16, maxZ = (origin.getZ() & ~15) + 31;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                float fx = dx / stretch, fz = dz * stretch;
                float angle = (float) Mth.atan2(fz, fx);
                float edge = radius * (1.0F + 0.25F * Mth.sin(2 * angle + lobes[0]) + 0.15F * Mth.sin(3 * angle + lobes[1])
                        + 0.08F * Mth.sin(5 * angle + lobes[2]));
                if (Mth.sqrt(fx * fx + fz * fz) > edge) continue;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1 != y) continue;
                p.set(x, y, z);
                if (!level.getBlockState(p).is(BlockTags.DIRT) || !level.getFluidState(p.above()).isEmpty()) continue;
                int across = alongX ? dz : dx;
                if (Math.floorMod(across + channelOffset, CHANNEL_SPACING) == 0 && this.contained(level, p, minX, maxX, minZ, maxZ)) {
                    level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 2);
                } else {
                    level.setBlock(p, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
                    BlockState crop = Math.floorDiv(across + channelOffset, CHANNEL_SPACING) % 2 == 0 ? first : second;
                    int age = Mth.clamp(ripeness + random.nextInt(3) - 1, 0, CropBlock.MAX_AGE);
                    level.setBlock(p.above(), crop.setValue(CropBlock.AGE, age), 2);
                }
                placed = true;
            }
        }
        return placed;
    }

    // a channel's water must not spill: every side of it is solid ground (or more of the field), within reach
    private boolean contained(WorldGenLevel level, BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
        for (BlockPos n : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west(), pos.below()}) {
            if (n.getX() < minX || n.getX() > maxX || n.getZ() < minZ || n.getZ() > maxZ) return false;
            BlockState state = level.getBlockState(n);
            if (!state.isSolid() && state.getFluidState().isEmpty()) return false;
        }
        return true;
    }
}
