package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.BananaBunchBlock;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.function.Consumer;

/**
 * Grows banana plants and checks the bunches behave like cocoa: they hang only on banana stems,
 * ripen with bone meal, drop bananas when broken and fall off when the stem goes.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class BananaTests {
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_25x20x25";
    private static final int SIZE_XZ = 25, SIZE_Y = 20;
    private static final BlockPos ROOT = new BlockPos(12, 1, 12);
    private static final BlockPos STEM = ROOT.above();
    private static final BlockPos BESIDE_STEM = STEM.east();
    private static final int SEEDS = 5;
    // every plant gets the same fixed crown (BananaFoliagePlacer.CROWN)
    private static final int CROWN_LEAVES = 42;

    @GameTest(template = TEMPLATE)
    public static void plantGrowsWithCrownAndBunches(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> plant = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "banana_plant")));
        helper.assertTrue(plant != null, "banana_plant is not registered");

        int bunchesSeen = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            String label = "banana_plant seed " + seed;
            clear(helper);
            helper.setBlock(ROOT, Blocks.GRASS_BLOCK);
            helper.assertTrue(plant.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), helper.absolutePos(STEM)),
                    label + ": failed to place");

            int stems = countBlocks(helper, ModBlocks.BANANA_STEM.get());
            helper.assertTrue(stems >= 3 && stems <= 5, label + ": " + stems + " stem blocks, expected 3-5");
            int leaves = countBlocks(helper, ModBlocks.BANANA_LEAVES.get());
            helper.assertTrue(leaves == CROWN_LEAVES, label + ": " + leaves + " leaves, expected " + CROWN_LEAVES);
            int bunches = countBlocks(helper, ModBlocks.BANANA_BUNCH.get());
            helper.assertTrue(bunches <= 2, label + ": " + bunches + " bunches, expected at most 2");
            bunchesSeen += bunches;

            forEachPos(p -> {
                BlockState s = helper.getBlockState(p);
                if (s.is(ModBlocks.BANANA_LEAVES.get()) && s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
                    helper.fail(label + ": leaf at " + p + " is too far from the stem and will decay");
                }
                if (s.is(ModBlocks.BANANA_BUNCH.get()) && !s.canSurvive(helper.getLevel(), helper.absolutePos(p))) {
                    helper.fail(label + ": bunch at " + p + " is not hanging on a stem");
                }
            });
        }
        // with probability 0.9 per plant, five plants without a single bunch means the decorator is broken
        helper.assertTrue(bunchesSeen > 0, "no plant grew any bananas");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bunchHangsOnlyOnBananaStems(GameTestHelper helper) {
        BlockState bunch = bunchFacingStem(0);
        helper.setBlock(STEM, ModBlocks.BANANA_STEM.get());
        helper.assertTrue(bunch.canSurvive(helper.getLevel(), helper.absolutePos(BESIDE_STEM)), "bunch should hang on a banana stem");
        helper.setBlock(STEM, Blocks.JUNGLE_LOG);
        helper.assertFalse(bunch.canSurvive(helper.getLevel(), helper.absolutePos(BESIDE_STEM)), "bunch should not hang on a jungle log");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealRipensBunch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(STEM, ModBlocks.BANANA_STEM.get());
        helper.setBlock(BESIDE_STEM, bunchFacingStem(0));
        BlockPos abs = helper.absolutePos(BESIDE_STEM);
        BananaBunchBlock block = ModBlocks.BANANA_BUNCH.get();

        for (int expected = 1; expected <= BananaBunchBlock.MAX_AGE; expected++) {
            BlockState state = helper.getBlockState(BESIDE_STEM);
            helper.assertTrue(block.isValidBonemealTarget(level, abs, state), "unripe bunch should take bone meal");
            block.performBonemeal(level, RandomSource.create(1L), abs, state);
            helper.assertTrue(helper.getBlockState(BESIDE_STEM).getValue(BananaBunchBlock.AGE) == expected, "bunch did not grow to stage " + expected);
        }
        helper.assertFalse(block.isValidBonemealTarget(level, abs, helper.getBlockState(BESIDE_STEM)), "ripe bunch should not take bone meal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void brokenBunchDropsBananas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(STEM, ModBlocks.BANANA_STEM.get());
        BlockPos abs = helper.absolutePos(BESIDE_STEM);
        for (int attempt = 0; attempt < 10; attempt++) {
            helper.assertTrue(bananas(level, abs, 0) == 0, "a flower bud should drop nothing");
            helper.assertTrue(bananas(level, abs, 1) == 1, "a half-grown bunch should drop 1 banana");
            int ripe = bananas(level, abs, 2);
            helper.assertTrue(ripe >= 3 && ripe <= 5, "a ripe bunch dropped " + ripe + " bananas, expected 3-5");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bunchFallsOffWhenStemIsCut(GameTestHelper helper) {
        helper.setBlock(STEM, ModBlocks.BANANA_STEM.get());
        helper.setBlock(BESIDE_STEM, bunchFacingStem(2));
        helper.setBlock(STEM, Blocks.AIR);
        helper.assertBlockNotPresent(ModBlocks.BANANA_BUNCH.get(), BESIDE_STEM);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealGrowsSaplingIntoPlant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(3L);
        helper.setBlock(ROOT, Blocks.GRASS_BLOCK);
        helper.setBlock(STEM, ModBlocks.BANANA_SAPLING.get());
        // first bone meal moves the sapling to stage 1, the next one grows the plant
        for (int i = 0; i < 4 && helper.getBlockState(STEM).is(ModBlocks.BANANA_SAPLING.get()); i++) {
            BlockState state = helper.getBlockState(STEM);
            ((SaplingBlock) state.getBlock()).performBonemeal(level, random, helper.absolutePos(STEM), state);
        }
        helper.assertBlockPresent(ModBlocks.BANANA_STEM.get(), STEM);
        helper.assertTrue(countBlocks(helper, ModBlocks.BANANA_LEAVES.get()) == CROWN_LEAVES, "the plant grew without its full crown");
        helper.succeed();
    }

    // a bunch east of the stem, so FACING (towards the stem) is west
    private static BlockState bunchFacingStem(int age) {
        return ModBlocks.BANANA_BUNCH.get().defaultBlockState()
                .setValue(BananaBunchBlock.FACING, Direction.WEST)
                .setValue(BananaBunchBlock.AGE, age);
    }

    private static int bananas(ServerLevel level, BlockPos pos, int age) {
        return Block.getDrops(bunchFacingStem(age), level, pos, null).stream()
                .filter(s -> s.is(ModItems.BANANA.get())).mapToInt(ItemStack::getCount).sum();
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
