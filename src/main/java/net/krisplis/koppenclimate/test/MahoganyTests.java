package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.function.Consumer;

/**
 * Grows the three mahogany shapes and both sapling setups, and checks the trees hold together.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class MahoganyTests {
    // the giant is up to 24 logs tall plus crown, and its branches and crown reach about 10 blocks out
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_33x32x33";
    private static final int SIZE_XZ = 33, SIZE_Y = 32;
    private static final BlockPos ROOT = new BlockPos(16, 1, 16);
    private static final int SEEDS_PER_SHAPE = 5;

    @GameTest(template = TEMPLATE)
    public static void saplingTakesRootOnDirtAndOxisol(GameTestHelper helper) {
        BlockState sapling = ModBlocks.MAHOGANY_SAPLING.get().defaultBlockState();
        BlockPos above = helper.absolutePos(ROOT.above());
        for (Block soil : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, ModBlocks.OXISOL.get(), ModBlocks.OXISOL_GRASS.get()}) {
            helper.setBlock(ROOT, soil);
            helper.assertTrue(sapling.canSurvive(helper.getLevel(), above), "sapling cannot survive on " + soil);
        }
        helper.setBlock(ROOT, Blocks.SAND);
        helper.assertFalse(sapling.canSurvive(helper.getLevel(), above), "mahogany should not take root in sand");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tallMahoganyHoldsTogether(GameTestHelper helper) {
        // trunk, buttress fins and branches
        growShape(helper, "mahogany_tall", 1, 12, 60, 100);
    }

    @GameTest(template = TEMPLATE)
    public static void giantMahoganyHoldsTogether(GameTestHelper helper) {
        // 6-8 long branches, each with a full-size crown
        growShape(helper, "mahogany_giant", 2, 100, 220, 1000);
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealGrowsOneSaplingIntoMahogany(GameTestHelper helper) {
        helper.setBlock(ROOT, Blocks.GRASS_BLOCK);
        helper.setBlock(ROOT.above(), ModBlocks.MAHOGANY_SAPLING.get());
        bonemealUntilGrown(helper, ROOT.above());

        // a single sapling always grows the tall mahogany: at least 10 logs of trunk
        for (int y = 1; y <= 10; y++) {
            helper.assertBlockPresent(ModBlocks.MAHOGANY_LOG.get(), ROOT.above(y));
        }
        helper.assertTrue(countBlocks(helper, ModBlocks.MAHOGANY_LEAVES.get()) >= 100, "the mahogany grew without its crown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fourSaplingsGrowGiantMahogany(GameTestHelper helper) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                helper.setBlock(ROOT.offset(dx, 0, dz), Blocks.GRASS_BLOCK);
                helper.setBlock(ROOT.offset(dx, 1, dz), ModBlocks.MAHOGANY_SAPLING.get());
            }
        }
        // bone meal on any one of the four grows the giant from the whole square
        bonemealUntilGrown(helper, ROOT.offset(1, 1, 1));

        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int y = 1; y <= 18; y++) {
                    helper.assertBlockPresent(ModBlocks.MAHOGANY_LOG.get(), ROOT.offset(dx, y, dz));
                }
            }
        }
        helper.assertTrue(countBlocks(helper, ModBlocks.MAHOGANY_LEAVES.get()) >= 1000, "the giant grew without its full crown");
        helper.succeed();
    }

    private static void bonemealUntilGrown(GameTestHelper helper, BlockPos sapling) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(11L);
        // first bone meal moves the sapling to stage 1, the next one grows the tree
        for (int i = 0; i < 4 && helper.getBlockState(sapling).is(ModBlocks.MAHOGANY_SAPLING.get()); i++) {
            BlockState state = helper.getBlockState(sapling);
            ((SaplingBlock) state.getBlock()).performBonemeal(level, random, helper.absolutePos(sapling), state);
        }
    }

    private static void growShape(GameTestHelper helper, String feature, int width, int minLogs, int maxLogs, int minLeaves) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, feature)));
        helper.assertTrue(tree != null, feature + " is not registered");

        for (long seed = 0; seed < SEEDS_PER_SHAPE; seed++) {
            String label = feature + " seed " + seed;
            clear(helper);
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < width; dz++) {
                    helper.setBlock(ROOT.offset(dx, 0, dz), Blocks.GRASS_BLOCK);
                }
            }
            boolean placed = tree.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), helper.absolutePos(ROOT.above()));
            helper.assertTrue(placed, label + ": failed to place");

            int logs = countBlocks(helper, ModBlocks.MAHOGANY_LOG.get());
            helper.assertTrue(logs >= minLogs && logs <= maxLogs, label + ": " + logs + " logs, expected " + minLogs + "-" + maxLogs);
            int leaves = countBlocks(helper, ModBlocks.MAHOGANY_LEAVES.get());
            helper.assertTrue(leaves >= minLeaves, label + ": " + leaves + " leaves, expected at least " + minLeaves);
            // grass under the trunk turns to dirt, like under vanilla trees
            helper.assertBlockPresent(Blocks.DIRT, ROOT);

            // every leaf must be within reach of a log, or the fresh tree would start decaying
            forEachPos(p -> {
                BlockState s = helper.getBlockState(p);
                if (s.is(ModBlocks.MAHOGANY_LEAVES.get()) && s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
                    helper.fail(label + ": leaf at " + p + " is too far from the tree and will decay");
                }
            });
        }
        helper.succeed();
    }

    private static int countBlocks(GameTestHelper helper, Block block) {
        int[] count = {0};
        forEachPos(p -> { if (helper.getBlockState(p).is(block)) count[0]++; });
        return count[0];
    }

    private static void clear(GameTestHelper helper) {
        // top down so nothing gets a chance to drop or update into the cleared space
        for (int y = SIZE_Y - 1; y >= 1; y--) {
            for (int x = 0; x < SIZE_XZ; x++) {
                for (int z = 0; z < SIZE_XZ; z++) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void forEachPos(Consumer<BlockPos> action) {
        for (int y = 1; y < SIZE_Y; y++) {
            for (int x = 0; x < SIZE_XZ; x++) {
                for (int z = 0; z < SIZE_XZ; z++) {
                    action.accept(new BlockPos(x, y, z));
                }
            }
        }
    }
}
