package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.ReindeerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Harnessing reindeer to a boat: with reindeer on your lead, sneak and click a boat and they are tied to it instead
 * (up to four). How the harnessed boat flies is the client's (client.ReindeerSleighClient: a boat is steered by the
 * one in it).
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReindeerSleigh {
    private ReindeerSleigh() {
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof Boat boat) || !player.isSecondaryUseActive()) return;
        List<ReindeerEntity> led = player.level().getEntitiesOfClass(ReindeerEntity.class, player.getBoundingBox().inflate(12.0),
                r -> r.getLeashHolder() == player);
        if (led.isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (player.level().isClientSide) return;
        int room = ReindeerEntity.MAX_TEAM - ReindeerEntity.team(boat).size();
        for (ReindeerEntity deer : led) {
            if (room-- <= 0) break;
            deer.setLeashedTo(boat, true);
        }
        player.level().playSound(null, boat.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
