package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.CattailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A little mud pond in the Temperate forest (Cfb): at most 7 x 7 blocks, sunk flush into level ground - a block or
 * two of (the biome's brown) water over a bed of mud, a muddy shore, cattails standing in the shallows round its edge,
 * lily pads on the open water, and a frog or two.
 */
public class MudPondFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_RADIUS = 3;

    public MudPondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int ox = origin.getX(), oz = origin.getZ();
        int y0 = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ox, oz) - 1;
        if (y0 < 63) return false;
        float rx = 1.6F + random.nextFloat() * (MAX_RADIUS + 0.4F - 1.6F);
        float rz = 1.6F + random.nextFloat() * (MAX_RADIUS + 0.4F - 1.6F);
        int size = 2 * MAX_RADIUS + 1;
        boolean[][] water = new boolean[size + 2][size + 2];
        float[][] depthAt = new float[size + 2][size + 2];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // the shape: an ellipse, a little ragged
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                float d = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
                if (d <= 1.0F - random.nextFloat() * 0.15F) {
                    water[dx + MAX_RADIUS + 1][dz + MAX_RADIUS + 1] = true;
                    depthAt[dx + MAX_RADIUS + 1][dz + MAX_RADIUS + 1] = d;
                }
            }
        }
        // only on level ground of soil, with a rim at least as high as the water all round, so none runs out
        for (int i = 0; i < size + 2; i++) {
            for (int k = 0; k < size + 2; k++) {
                int x = ox + i - MAX_RADIUS - 1, z = oz + k - MAX_RADIUS - 1;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (water[i][k]) {
                    if (top < y0 || top > y0 + 1) return false;
                    if (!level.getBlockState(pos.set(x, y0, z)).is(BlockTags.DIRT)) return false;
                } else if (nextToWater(water, i, k)) {
                    if (top < y0 || top > y0 + 2) return false;
                    if (!level.getBlockState(pos.set(x, y0, z)).isSolid()) return false;
                }
            }
        }
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState waterState = Blocks.WATER.defaultBlockState();
        for (int i = 0; i < size + 2; i++) {
            for (int k = 0; k < size + 2; k++) {
                int x = ox + i - MAX_RADIUS - 1, z = oz + k - MAX_RADIUS - 1;
                if (water[i][k]) {
                    // a block deep at the edge, two in the middle, on two blocks of mud
                    int depth = depthAt[i][k] < 0.35F ? 2 : 1;
                    for (int y = y0 + 1; y <= y0 + 3; y++) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    for (int j = 0; j < depth; j++) level.setBlock(pos.set(x, y0 - j, z), waterState, Block.UPDATE_CLIENTS);
                    level.setBlock(pos.set(x, y0 - depth, z), mud, Block.UPDATE_CLIENTS);
                    level.setBlock(pos.set(x, y0 - depth - 1, z), mud, Block.UPDATE_CLIENTS);
                    boolean edge = nextToLand(water, i, k);
                    if (edge && random.nextInt(5) < 2) {
                        // cattails standing in the shallows
                        BlockState lower = ModBlocks.CATTAIL.get().defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER).setValue(CattailBlock.WATERLOGGED, true);
                        level.setBlock(pos.set(x, y0, z), lower, Block.UPDATE_CLIENTS);
                        level.setBlock(pos.set(x, y0 + 1, z), lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)
                                .setValue(CattailBlock.WATERLOGGED, false), Block.UPDATE_CLIENTS);
                    } else if (!edge && random.nextInt(4) == 0) {
                        level.setBlock(pos.set(x, y0 + 1, z), Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                } else if (nextToWater(water, i, k) && random.nextInt(3) > 0) {
                    // the muddy shore
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    if (top == y0 && level.getBlockState(pos.set(x, y0, z)).is(BlockTags.DIRT)) {
                        level.setBlock(pos, mud, Block.UPDATE_CLIENTS);
                        BlockState above = level.getBlockState(pos.set(x, y0 + 1, z));
                        if (!above.isAir() && above.canBeReplaced()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // a frog or two
        int frogs = 1 + random.nextInt(2);
        for (int n = 0; n < frogs; n++) {
            Frog frog = EntityType.FROG.create(level.getLevel());
            if (frog == null) continue;
            frog.moveTo(ox + 0.5 + random.nextInt(3) - 1, y0 + 1.0, oz + 0.5 + random.nextInt(3) - 1, random.nextFloat() * 360.0F, 0.0F);
            frog.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), MobSpawnType.CHUNK_GENERATION, null);
            frog.setPersistenceRequired();
            level.addFreshEntityWithPassengers(frog);
        }
        return true;
    }

    private static boolean nextToWater(boolean[][] water, int i, int k) {
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                int ni = i + a, nk = k + b;
                if (ni >= 0 && nk >= 0 && ni < water.length && nk < water.length && water[ni][nk]) return true;
            }
        }
        return false;
    }

    private static boolean nextToLand(boolean[][] water, int i, int k) {
        return !water[i + 1][k] || !water[i - 1][k] || !water[i][k + 1] || !water[i][k - 1];
    }
}
