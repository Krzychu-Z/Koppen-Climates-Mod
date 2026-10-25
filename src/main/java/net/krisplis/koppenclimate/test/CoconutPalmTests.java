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

/**
 * Grows the three coconut palm shapes and the sapling, and checks the trees hold together.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class CoconutPalmTests {
    // tall enough for a 13-block trunk plus crown, wide enough for the crooked palm's lean
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_25x20x25";
    private static final int SIZE_XZ = 25, SIZE_Y = 20;
    private static final BlockPos ROOT = new BlockPos(12, 1, 12);
    private static final int SEEDS_PER_SHAPE = 5;
    // every palm gets the same fixed crown (PalmFoliagePlacer.CROWN); in open space all of it fits
    private static final int CROWN_LEAVES = 66;

    @GameTest(template = TEMPLATE)
    public static void saplingTakesRootOnSandAndDirt(GameTestHelper helper) {
        BlockState sapling = ModBlocks.COCONUT_PALM_SAPLING.get().defaultBlockState();
        BlockPos above = helper.absolutePos(ROOT.above());
        for (Block soil : new Block[]{ModBlocks.WHITE_SAND.get(), Blocks.SAND, Blocks.RED_SAND, Blocks.DIRT, Blocks.GRASS_BLOCK,
                ModBlocks.OXISOL.get(), ModBlocks.OXISOL_GRASS.get()}) {
            helper.setBlock(ROOT, soil);
            helper.assertTrue(sapling.canSurvive(helper.getLevel(), above), "sapling cannot survive on " + soil);
        }
        helper.setBlock(ROOT, Blocks.STONE);
        helper.assertFalse(sapling.canSurvive(helper.getLevel(), above), "sapling should not survive on stone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void smallPalmHoldsTogether(GameTestHelper helper) {
        growShape(helper, "coconut_palm_small", 4, 6);
    }

    @GameTest(template = TEMPLATE)
    public static void tallPalmHoldsTogether(GameTestHelper helper) {
        // up to 2 horizontal logs where it bends near the crown
        growShape(helper, "coconut_palm_tall", 8, 15);
    }

    @GameTest(template = TEMPLATE)
    public static void crookedPalmHoldsTogether(GameTestHelper helper) {
        // horizontal logs where it leans add to the count
        growShape(helper, "coconut_palm_crooked", 7, 16);
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealGrowsSaplingIntoPalm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(7L);
        helper.setBlock(ROOT, Blocks.SAND);
        helper.setBlock(ROOT.above(), ModBlocks.COCONUT_PALM_SAPLING.get());

        // first bone meal moves the sapling to stage 1, the next one grows the tree
        for (int i = 0; i < 4 && helper.getBlockState(ROOT.above()).is(ModBlocks.COCONUT_PALM_SAPLING.get()); i++) {
            BlockState state = helper.getBlockState(ROOT.above());
            ((SaplingBlock) state.getBlock()).performBonemeal(level, random, helper.absolutePos(ROOT.above()), state);
        }

        helper.assertBlockPresent(ModBlocks.COCONUT_PALM_LOG.get(), ROOT.above());
        helper.assertBlockPresent(Blocks.SAND, ROOT);
        helper.assertTrue(countBlocks(helper, ModBlocks.COCONUT_PALM_LEAVES.get()) == CROWN_LEAVES, "the palm grew without its full crown");
        helper.succeed();
    }

    private static void growShape(GameTestHelper helper, String feature, int minLogs, int maxLogs) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> palm = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, feature)));
        helper.assertTrue(palm != null, feature + " is not registered");

        for (long seed = 0; seed < SEEDS_PER_SHAPE; seed++) {
            String label = feature + " seed " + seed;
            clear(helper);
            helper.setBlock(ROOT, Blocks.SAND);
            boolean placed = palm.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), helper.absolutePos(ROOT.above()));
            helper.assertTrue(placed, label + ": failed to place");

            int logs = countBlocks(helper, ModBlocks.COCONUT_PALM_LOG.get());
            helper.assertTrue(logs >= minLogs && logs <= maxLogs, label + ": " + logs + " logs, expected " + minLogs + "-" + maxLogs);
            int leaves = countBlocks(helper, ModBlocks.COCONUT_PALM_LEAVES.get());
            helper.assertTrue(leaves == CROWN_LEAVES, label + ": " + leaves + " leaves, expected the full " + CROWN_LEAVES + "-leaf crown");
            helper.assertBlockPresent(Blocks.SAND, ROOT);   // palms keep the sand under them

            // every leaf must be within reach of a log, or the fresh tree would start decaying
            forEachPos(p -> {
                BlockState s = helper.getBlockState(p);
                if (s.is(ModBlocks.COCONUT_PALM_LEAVES.get()) && s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
                    helper.fail(label + ": leaf at " + p + " is too far from the trunk and will decay");
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

    private static void forEachPos(java.util.function.Consumer<BlockPos> action) {
        for (int y = 1; y < SIZE_Y; y++) {
            for (int x = 0; x < SIZE_XZ; x++) {
                for (int z = 0; z < SIZE_XZ; z++) {
                    action.accept(new BlockPos(x, y, z));
                }
            }
        }
    }
}
