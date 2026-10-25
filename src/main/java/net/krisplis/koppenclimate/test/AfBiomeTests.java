package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.CoconutBlock;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.BlackPantherEntity;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Checks the Köppen Af setup: the biomes and the world preset that places them, coconuts, limestone, the panther.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class AfBiomeTests {
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_17x5x17";
    private static final BlockPos LOG = new BlockPos(8, 2, 8);
    private static final BlockPos BESIDE_LOG = LOG.east();

    @GameTest(template = TEMPLATE)
    public static void afBiomesAreRegistered(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> key : java.util.List.of(ModBiomes.AMAZON_JUNGLE, ModBiomes.AMAZON_JUNGLE_RIVER, ModBiomes.JUNGLE_BEACH)) {
            helper.assertTrue(biomes.containsKey(key), key.location() + " is not registered");
            helper.assertTrue(biomes.getHolderOrThrow(key).is(ModBiomes.AF), key.location() + " is missing from the af biome tag");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultWorldsUseTheKoppenBiomeSource(GameTestHelper helper) {
        WorldPreset preset = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET).get(WorldPresets.NORMAL);
        helper.assertTrue(preset != null, "the default world preset is missing");
        var overworld = preset.overworld();
        helper.assertTrue(overworld.isPresent(), "the default world preset has no overworld");
        helper.assertTrue(overworld.get().generator().getBiomeSource() instanceof KoppenBiomeSource,
                "new default worlds don't use the Köppen biome source, so the Af biomes would never generate");
        helper.assertTrue(overworld.get().generator().getBiomeSource().possibleBiomes().stream().anyMatch(b -> b.is(ModBiomes.AMAZON_JUNGLE)),
                "the Amazon jungle is not among the overworld's possible biomes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void coconutsHangOnlyOnPalmLogsAndDropWhenRipe(GameTestHelper helper) {
        BlockState coconut = ModBlocks.COCONUT.get().defaultBlockState().setValue(CoconutBlock.FACING, Direction.WEST);
        helper.setBlock(LOG, ModBlocks.COCONUT_PALM_LOG.get());
        helper.assertTrue(coconut.canSurvive(helper.getLevel(), helper.absolutePos(BESIDE_LOG)), "coconuts should hang on a palm log");
        helper.setBlock(LOG, Blocks.JUNGLE_LOG);
        helper.assertFalse(coconut.canSurvive(helper.getLevel(), helper.absolutePos(BESIDE_LOG)), "coconuts should not hang on a jungle log");

        int ripe = coconuts(helper, coconut.setValue(CoconutBlock.AGE, CoconutBlock.MAX_AGE));
        helper.assertTrue(ripe >= 1 && ripe <= 2, "a ripe cluster dropped " + ripe + " coconuts, expected 1-2");
        helper.assertTrue(coconuts(helper, coconut) == 0, "unripe coconuts should drop nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void limestoneStillGetsOres(GameTestHelper helper) {
        BlockState limestone = ModBlocks.YELLOW_LIMESTONE.get().defaultBlockState();
        helper.assertTrue(limestone.is(BlockTags.STONE_ORE_REPLACEABLES), "ores would not generate in yellow limestone");
        helper.assertTrue(limestone.is(BlockTags.MINEABLE_WITH_PICKAXE), "yellow limestone should be mined with a pickaxe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blackPantherSpawnsWithItsStats(GameTestHelper helper) {
        helper.setBlock(LOG.below(), Blocks.GRASS_BLOCK);
        BlackPantherEntity panther = helper.spawn(ModEntities.BLACK_PANTHER.get(), LOG);
        helper.assertTrue(panther.getMaxHealth() == 30.0F, "black panther should have 30 health, has " + panther.getMaxHealth());
        helper.assertFalse(panther.removeWhenFarAway(1000.0), "black panthers should not despawn when the player walks away");
        helper.succeed();
    }

    private static int coconuts(GameTestHelper helper, BlockState state) {
        return Block.getDrops(state, helper.getLevel(), helper.absolutePos(BESIDE_LOG), null).stream()
                .filter(s -> s.is(ModItems.COCONUT.get())).mapToInt(ItemStack::getCount).sum();
    }
}
