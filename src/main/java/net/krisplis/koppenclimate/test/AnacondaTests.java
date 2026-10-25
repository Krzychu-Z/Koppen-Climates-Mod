package net.krisplis.koppenclimate.test;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Spawns anacondas and checks their stats, their constricting bite and that they don't despawn.
 * Run headless with {@code ./gradlew runGameTestServer}, or in game with {@code /test runall}.
 */
@GameTestHolder(KoppenClimateMod.MOD_ID)
public class AnacondaTests {
    private static final String TEMPLATE = KoppenClimateMod.MOD_ID + ":empty_25x20x25";
    private static final BlockPos FLOOR = new BlockPos(12, 1, 12);

    @GameTest(template = TEMPLATE)
    public static void spawnsWithItsStats(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.GRASS_BLOCK);
        AnacondaEntity snake = helper.spawn(ModEntities.ANACONDA.get(), FLOOR.above());
        helper.assertTrue(snake.getMaxHealth() == 30.0F, "anaconda should have 30 health, has " + snake.getMaxHealth());
        helper.assertFalse(snake.removeWhenFarAway(1000.0), "anacondas should not despawn when the player walks away");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void coilsUpAndWakesWhenHit(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.GRASS_BLOCK);
        AnacondaEntity snake = helper.spawn(ModEntities.ANACONDA.get(), FLOOR.above());
        snake.setCoiled(true);
        helper.assertTrue(snake.isCoiled(), "the anaconda should be coiled");
        snake.hurt(helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertFalse(snake.isCoiled(), "being hit should wake a coiled anaconda");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staysCoiledAfterSaveAndLoad(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.GRASS_BLOCK);
        AnacondaEntity snake = helper.spawn(ModEntities.ANACONDA.get(), FLOOR.above());
        snake.setCoiled(true);
        CompoundTag saved = snake.saveWithoutId(new CompoundTag());

        AnacondaEntity loaded = ModEntities.ANACONDA.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "could not create an anaconda");
        loaded.load(saved);
        helper.assertTrue(loaded.isCoiled(), "a coiled anaconda should still be coiled after the world reloads");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void biteSqueezesAndSlowsPrey(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.GRASS_BLOCK);
        helper.setBlock(FLOOR.east(2), Blocks.GRASS_BLOCK);
        AnacondaEntity snake = helper.spawn(ModEntities.ANACONDA.get(), FLOOR.above());
        Chicken chicken = helper.spawn(EntityType.CHICKEN, FLOOR.east(2).above());
        helper.assertTrue(snake.doHurtTarget(chicken), "the bite did not land");
        helper.assertTrue(chicken.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a bitten animal should be slowed by the squeeze");
        helper.succeed();
    }
}
