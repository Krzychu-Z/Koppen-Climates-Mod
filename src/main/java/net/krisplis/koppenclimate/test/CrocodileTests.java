package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Spawns crocodiles and checks their stats, that they can't drown, and basking.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class CrocodileTests {
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_25x20x25";
    private static final BlockPos FLOOR = new BlockPos(12, 1, 12);

    @GameTest(template = TEMPLATE)
    public static void spawnsWithItsStats(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.MUD);
        CrocodileEntity croc = helper.spawn(ModEntities.CROCODILE.get(), FLOOR.above());
        helper.assertTrue(croc.getMaxHealth() == 40.0F, "crocodile should have 40 health, has " + croc.getMaxHealth());
        helper.assertFalse(croc.removeWhenFarAway(1000.0), "crocodiles should not despawn when the player walks away");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void neverDrowns(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.MUD);
        CrocodileEntity croc = helper.spawn(ModEntities.CROCODILE.get(), FLOOR.above());
        helper.assertFalse(croc.canDrownInFluidType(ForgeMod.WATER_TYPE.get()), "crocodiles should be able to stay underwater");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void baskingEndsWhenHitAndSurvivesReload(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.MUD);
        CrocodileEntity croc = helper.spawn(ModEntities.CROCODILE.get(), FLOOR.above());
        croc.setBasking(true);

        CompoundTag saved = croc.saveWithoutId(new CompoundTag());
        CrocodileEntity loaded = ModEntities.CROCODILE.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "could not create a crocodile");
        loaded.load(saved);
        helper.assertTrue(loaded.isBasking(), "a basking crocodile should still be basking after the world reloads");

        croc.hurt(helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertFalse(croc.isBasking(), "being hit should end basking");
        helper.succeed();
    }
}
