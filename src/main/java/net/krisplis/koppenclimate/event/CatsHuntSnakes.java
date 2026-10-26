package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CobraEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cats hunt snakes: every cat goes after cobras and black mambas it sees (wild or tame). The snakes never fight a
 * cat back - they only slither away (CobraEntity).
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CatsHuntSnakes {
    private CatsHuntSnakes() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Cat cat)) return;
        cat.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(cat, CobraEntity.class, 10, true, false,
                snake -> !cat.isOrderedToSit()));
    }
}
