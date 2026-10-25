package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.krisplis.koppenclimate.effect.ModPotions;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Checks the rafflesia behaves like a flower: where it can grow, what it drops, and its stew effect.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class RafflesiaTests {
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_17x5x17";
    private static final BlockPos SOIL = new BlockPos(8, 1, 8);

    @GameTest(template = TEMPLATE)
    public static void growsOnForestSoilOnly(GameTestHelper helper) {
        BlockState flower = ModBlocks.RAFFLESIA.get().defaultBlockState();
        BlockPos above = helper.absolutePos(SOIL.above());
        for (Block soil : new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, ModBlocks.OXISOL.get(), ModBlocks.OXISOL_GRASS.get()}) {
            helper.setBlock(SOIL, soil);
            helper.assertTrue(flower.canSurvive(helper.getLevel(), above), "rafflesia cannot grow on " + soil);
        }
        helper.setBlock(SOIL, Blocks.STONE);
        helper.assertFalse(flower.canSurvive(helper.getLevel(), above), "rafflesia should not grow on stone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void isAFlowerThatDropsItself(GameTestHelper helper) {
        BlockState flower = ModBlocks.RAFFLESIA.get().defaultBlockState();
        helper.assertTrue(flower.is(BlockTags.SMALL_FLOWERS), "rafflesia should count as a small flower");
        int drops = Block.getDrops(flower, helper.getLevel(), helper.absolutePos(SOIL.above()), null).stream()
                .filter(s -> s.is(ModBlocks.RAFFLESIA.get().asItem())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(drops == 1, "rafflesia dropped " + drops + " of itself, expected 1");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void petalBrewsIntoDelirium(GameTestHelper helper) {
        PotionBrewing brewing = helper.getLevel().potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack petal = new ItemStack(ModItems.RAFFLESIA_PETAL.get());
        helper.assertTrue(brewing.hasMix(awkward, petal), "an awkward potion and a rafflesia petal should brew");
        ItemStack brewed = brewing.mix(petal, awkward);
        helper.assertTrue(brewed.get(DataComponents.POTION_CONTENTS).is(ModPotions.DELIRIUM.getHolder().orElseThrow()),
                "the brew should be the Potion of Delirium");

        ItemStack delirium = PotionContents.createItemStack(Items.POTION, ModPotions.DELIRIUM.getHolder().orElseThrow());
        helper.assertTrue(brewing.hasMix(delirium, new ItemStack(Items.REDSTONE)), "redstone should lengthen Delirium");
        helper.assertTrue(brewing.hasMix(delirium, new ItemStack(Items.GLOWSTONE_DUST)), "glowstone should strengthen Delirium");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deliriumGivesVisionsNauseaAndPoison(GameTestHelper helper) {
        var effects = ModPotions.DELIRIUM.get().getEffects();
        helper.assertTrue(effects.stream().anyMatch(e -> e.getEffect().equals(ModEffects.PSYCHEDELIA.getHolder().orElseThrow())), "Delirium should give Psychedelia");
        helper.assertTrue(effects.stream().anyMatch(e -> e.getEffect().equals(MobEffects.CONFUSION)), "Delirium should give Nausea");
        helper.assertTrue(effects.stream().anyMatch(e -> e.getEffect().equals(MobEffects.POISON)), "Delirium should give Poison");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stewGivesNausea(GameTestHelper helper) {
        boolean nausea = ModBlocks.RAFFLESIA.get().getSuspiciousEffects().effects().stream()
                .anyMatch(e -> e.effect().equals(MobEffects.CONFUSION));
        helper.assertTrue(nausea, "suspicious stew from a rafflesia should give Nausea");
        helper.succeed();
    }
}
