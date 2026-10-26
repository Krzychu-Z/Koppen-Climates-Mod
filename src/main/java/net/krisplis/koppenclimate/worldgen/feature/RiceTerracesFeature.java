package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.RiceBlock;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The paddies of the Rice Terraces (Aw), laid out a chunk at a time as parallel strips. In each chunk the strips
 * run along the hillside (across the slope, as terraces do) and are four blocks wide, with a one-block earth bank
 * between neighbours; along its length a strip is cut into separate paddies wherever the ground steps up or down
 * by more than a block, so the paddies step down the hill. Each paddy is a still pond one block deep on a mud bed,
 * at the height of the ground where it starts (dug down or banked up a block to it), with banks built up wherever
 * the ground beside it is lower. The chunk's edges are always banks, so the strips line up chunk to chunk but no
 * two paddies ever share water. Most of each paddy is planted with rice at every stage of growth, and a few cod
 * live in it.
 */
public class RiceTerracesFeature extends Feature<NoneFeatureConfiguration> {
    // across the strips: bank, four blocks of paddy, bank...; along a strip, paddies at most this long
    private static final int PERIOD = 5;
    private static final int MAX_LENGTH = 14;
    private static final float RICE_CHANCE = 0.8F;
    private static final int CELLS_PER_COD = 12;

    public RiceTerracesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Cod may spawn in any shallow paddy water of the rice terraces, however high up the hill. */
    public static boolean codInPaddy(EntityType<Cod> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getBiome(pos).is(ModBiomes.RICE_TERRACES);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;

        int[][] ground = new int[16][16];
        boolean[][] usable = new boolean[16][16];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int x = x0 + i, z = z0 + j;
                int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                ground[i][j] = g;
                pos.set(x, g, z);
                usable[i][j] = level.getBiome(pos).is(ModBiomes.RICE_TERRACES)
                        && level.getBlockState(pos).is(BlockTags.DIRT)
                        && level.getFluidState(pos.above()).isEmpty()
                        && !this.waterBeside(level, x, z, g);
            }
        }

        // the strips run across the slope: along x if the ground falls mostly north-south, else along z
        int fallX = Math.abs(ground[15][0] + ground[15][15] - ground[0][0] - ground[0][15]);
        int fallZ = Math.abs(ground[0][15] + ground[15][15] - ground[0][0] - ground[15][0]);
        boolean alongX = fallZ >= fallX;

        int[][] level2 = new int[16][16];   // water level per column, or MIN_VALUE when not paddy
        for (int[] row : level2) java.util.Arrays.fill(row, Integer.MIN_VALUE);
        int cells = 0;
        // across: strips at 1-4, 6-9, 11-14 (0, 5, 10, 15 are banks)
        for (int stripStart = 1; stripStart + 3 <= 14; stripStart += PERIOD) {
            int along = 1;
            while (along <= 14) {
                // a paddy starts at the first column all four blocks of which can take one
                if (!this.columnUsable(usable, alongX, along, stripStart)) {
                    along++;
                    continue;
                }
                int water = this.cell(ground, alongX, along, stripStart);
                int end = along;
                while (end + 1 <= 14 && end + 1 - along < MAX_LENGTH && this.columnUsable(usable, alongX, end + 1, stripStart)
                        && this.columnFits(ground, alongX, end + 1, stripStart, water)
                        && this.columnFits(ground, alongX, along, stripStart, water)) {
                    end++;
                }
                if (!this.columnFits(ground, alongX, along, stripStart, water)) {
                    along++;
                    continue;
                }
                if (end - along >= 1) {
                    for (int a = along; a <= end; a++) {
                        for (int c = stripStart; c < stripStart + 4; c++) {
                            int i = alongX ? a : c, j = alongX ? c : a;
                            level2[i][j] = water;
                            cells++;
                        }
                    }
                }
                // the column after a paddy is a bank between it and the next
                along = end + 2;
            }
        }
        if (cells == 0) return false;

        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState bank = ModBlocks.OXISOL.get().defaultBlockState();
        BlockState bankTop = ModBlocks.OXISOL_GRASS.get().defaultBlockState();
        RiceBlock rice = ModBlocks.RICE.get();
        java.util.List<BlockPos> waterCells = new java.util.ArrayList<>();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int water = level2[i][j];
                if (water == Integer.MIN_VALUE) continue;
                int x = x0 + i, z = z0 + j;
                for (int y = water + 1; y <= Math.max(ground[i][j], water) + 2; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(pos.set(x, water - 1, z), mud, Block.UPDATE_CLIENTS);
                BlockState fill = random.nextFloat() < RICE_CHANCE
                        ? rice.getStateForAge(2 + random.nextInt(rice.getMaxAge() - 1))
                        : Blocks.WATER.defaultBlockState();
                level.setBlock(pos.set(x, water, z), fill, Block.UPDATE_CLIENTS);
                waterCells.add(new BlockPos(x, water, z));
            }
        }
        // banks: every non-paddy column beside a paddy is built up to the paddy's water level
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (level2[i][j] != Integer.MIN_VALUE) continue;
                int need = Integer.MIN_VALUE;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int ni = i + d.getStepX(), nj = j + d.getStepZ();
                    if (ni >= 0 && ni < 16 && nj >= 0 && nj < 16) need = Math.max(need, level2[ni][nj]);
                }
                if (need == Integer.MIN_VALUE || ground[i][j] >= need) continue;
                int x = x0 + i, z = z0 + j;
                for (int y = ground[i][j] + 1; y <= need; y++) {
                    level.setBlock(pos.set(x, y, z), y == need ? bankTop : bank, Block.UPDATE_CLIENTS);
                }
                pos.set(x, need + 1, z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        // a few cod in the water, there to stay
        int cod = Math.max(1, waterCells.size() / CELLS_PER_COD);
        for (int n = 0; n < cod; n++) {
            BlockPos at = waterCells.get(random.nextInt(waterCells.size()));
            Cod fish = EntityType.COD.create(level.getLevel());
            if (fish == null) continue;
            fish.moveTo(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            fish.setFromBucket(true);
            level.addFreshEntity(fish);
        }
        return true;
    }

    private int cell(int[][] ground, boolean alongX, int along, int across) {
        return alongX ? ground[along][across] : ground[across][along];
    }

    private boolean columnUsable(boolean[][] usable, boolean alongX, int along, int stripStart) {
        for (int c = stripStart; c < stripStart + 4; c++) {
            if (!(alongX ? usable[along][c] : usable[c][along])) return false;
        }
        return true;
    }

    // every block of the cross-section within a block of the water level
    private boolean columnFits(int[][] ground, boolean alongX, int along, int stripStart, int water) {
        for (int c = stripStart; c < stripStart + 4; c++) {
            int g = alongX ? ground[along][c] : ground[c][along];
            if (g < water - 1 || g > water + 1) return false;
        }
        return true;
    }

    // other water right beside the column (a river, a lake, another chunk's paddy)
    private boolean waterBeside(WorldGenLevel level, int x, int z, int g) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int y = g; y <= g + 2; y++) {
                if (level.getFluidState(pos.set(x + d.getStepX(), y, z + d.getStepZ())).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }
}
