package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.SapphireRingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What wearing the sapphire ring (in the off-hand slot) does: no fall ever hurts you, however far, and you can fly
 * just as in creative mode - double-tap jump to take off or land, jump and sneak to go up and down.
 *
 * <p>The flight is the player's own "may fly" ability, switched on while the ring is worn and marked in the player's
 * data as the ring's doing, so that taking the ring off takes the flight away again (and lands you gently: the fall
 * you're in is forgiven) - but never from a player in creative or spectator mode, whose flight is their own.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SapphireRing {
    private static final String GRANTED = KoppenClimateMod.MOD_ID + ":sapphire_flight";

    private SapphireRing() {
    }

    /** The ring is worn by keeping it in the off-hand (left hand) slot (the Elder ring does all it does). */
    public static boolean wears(Player player) {
        return player.getOffhandItem().getItem() instanceof SapphireRingItem || Rings.wearsElder(player);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && wears(player)) {
            event.setDistance(0.0F);
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
        }
    }

    // whatever gets past (flying into a wall too fast, a fall that lands in a powder snow drift...): no fall damage at all
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FALL) && wears(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        boolean wearing = wears(player);
        CompoundTag data = player.getPersistentData();
        boolean granted = data.getBoolean(GRANTED);
        boolean ownFlight = player.isCreative() || player.isSpectator();
        if (wearing) {
            player.resetFallDistance();
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                data.putBoolean(GRANTED, true);
            }
        } else if (granted) {
            data.remove(GRANTED);
            if (!ownFlight && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            player.resetFallDistance();
        }
    }
}
