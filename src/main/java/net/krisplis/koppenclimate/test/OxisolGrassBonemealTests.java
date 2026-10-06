package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks that bone meal on oxisol grass behaves like it does on vanilla grass_block.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class OxisolGrassBonemealTests {
    // a namespaced id is used as-is by Forge, without the class-name prefix
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_17x5x17";
    private static final int SIZE = 17;
    // 9x9 floor in the middle of the template; the bone meal random walk reaches at most 7 blocks out,
    // so it never leaves the template
    private static final int FLOOR_MIN = 4, FLOOR_MAX = 12, FLOOR_Y = 1;
    private static final BlockPos CENTER = new BlockPos(8, FLOOR_Y, 8);
    private static final long SEED = 42L;

    @GameTest(template = TEMPLATE)
    public static void plantsSurviveOnOxisolSoils(GameTestHelper helper) {
        for (Block soil : new Block[]{ModBlocks.OXISOL_GRASS.get(), ModBlocks.OXISOL.get()}) {
            helper.setBlock(CENTER, soil);
            BlockPos above = helper.absolutePos(CENTER.above());
            for (Block plant : new Block[]{Blocks.SHORT_GRASS, Blocks.POPPY, Blocks.DANDELION}) {
                helper.assertTrue(plant.defaultBlockState().canSurvive(helper.getLevel(), above),
                        plant + " cannot survive on " + soil);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void validTargetOnlyWithAirAbove(GameTestHelper helper) {
        helper.setBlock(CENTER, ModBlocks.OXISOL_GRASS.get());
        BonemealableBlock block = (BonemealableBlock) ModBlocks.OXISOL_GRASS.get();
        BlockState state = helper.getBlockState(CENTER);
        BlockPos abs = helper.absolutePos(CENTER);

        helper.assertTrue(block.isValidBonemealTarget(helper.getLevel(), abs, state), "should accept air above");
        helper.setBlock(CENTER.above(), Blocks.STONE);
        helper.assertFalse(block.isValidBonemealTarget(helper.getLevel(), abs, state), "should reject stone above");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealSpawnsVegetation(GameTestHelper helper) {
        fillFloor(helper, ModBlocks.OXISOL_GRASS.get());
        bonemealCenter(helper, RandomSource.create(SEED));

        Map<BlockPos, BlockState> plants = snapshotAboveFloor(helper);
        helper.assertTrue(plants.size() >= 10, "expected at least 10 plants, got " + plants.size());

        // nothing may grow outside the floor, nor float above it (y=0 holds the test's structure block)
        for (int x = 0; x < SIZE; x++) {
            for (int y = FLOOR_Y; y < 5; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    boolean onFloor = isFloorColumn(x, z) && (y == FLOOR_Y || y == FLOOR_Y + 1 || y == FLOOR_Y + 2);
                    if (!onFloor) helper.assertBlockPresent(Blocks.AIR, p);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealMatchesVanillaGrass(GameTestHelper helper) {
        fillFloor(helper, ModBlocks.OXISOL_GRASS.get());
        bonemealCenter(helper, RandomSource.create(SEED));
        Map<BlockPos, BlockState> oxisol = snapshotAboveFloor(helper);

        clearAboveFloor(helper);
        fillFloor(helper, Blocks.GRASS_BLOCK);
        bonemealCenter(helper, RandomSource.create(SEED));
        Map<BlockPos, BlockState> vanilla = snapshotAboveFloor(helper);

        helper.assertTrue(!vanilla.isEmpty(), "vanilla grass grew nothing; test setup is broken");
        helper.assertTrue(oxisol.equals(vanilla),
                "oxisol grass grew " + oxisol.size() + " plants, vanilla grass grew " + vanilla.size()
                        + " with the same seed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bonemealGrowsShortGrassIntoTallGrass(GameTestHelper helper) {
        fillFloor(helper, ModBlocks.OXISOL_GRASS.get());
        helper.setBlock(CENTER.above(), Blocks.SHORT_GRASS);
        RandomSource random = RandomSource.create(SEED);

        for (int attempt = 0; attempt < 5; attempt++) {
            bonemealCenter(helper, random);
            if (snapshotAboveFloor(helper).containsValue(Blocks.TALL_GRASS.defaultBlockState())) {
                helper.succeed();
                return;
            }
        }
        helper.fail("short grass never turned into tall grass after 5 bone meals");
    }

    private static void bonemealCenter(GameTestHelper helper, RandomSource random) {
        ServerLevel level = helper.getLevel();
        BlockState state = helper.getBlockState(CENTER);
        ((BonemealableBlock) state.getBlock()).performBonemeal(level, random, helper.absolutePos(CENTER), state);
    }

    private static boolean isFloorColumn(int x, int z) {
        return x >= FLOOR_MIN && x <= FLOOR_MAX && z >= FLOOR_MIN && z <= FLOOR_MAX;
    }

    private static void fillFloor(GameTestHelper helper, Block block) {
        for (int x = FLOOR_MIN; x <= FLOOR_MAX; x++) {
            for (int z = FLOOR_MIN; z <= FLOOR_MAX; z++) {
                helper.setBlock(new BlockPos(x, FLOOR_Y, z), block);
            }
        }
    }

    private static void clearAboveFloor(GameTestHelper helper) {
        // top halves first, so double plants don't drop items or leave halves behind
        for (int y = FLOOR_Y + 2; y > FLOOR_Y; y--) {
            for (int x = FLOOR_MIN; x <= FLOOR_MAX; x++) {
                for (int z = FLOOR_MIN; z <= FLOOR_MAX; z++) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static Map<BlockPos, BlockState> snapshotAboveFloor(GameTestHelper helper) {
        Map<BlockPos, BlockState> plants = new HashMap<>();
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 2; y++) {
            for (int x = FLOOR_MIN; x <= FLOOR_MAX; x++) {
                for (int z = FLOOR_MIN; z <= FLOOR_MAX; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = helper.getBlockState(p);
                    if (!s.isAir()) plants.put(p, s);
                }
            }
        }
        return plants;
    }
}
