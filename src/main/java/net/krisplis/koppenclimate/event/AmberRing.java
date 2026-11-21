package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.AmberRingItem;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What wearing the amber ring (in the off-hand slot) does: total immunity to fire - no fire, lava, magma, burning
 * or fireball hurts you, and you never stay alight - and full breathing under water (your air never runs out).
 *
 * <p>It works through Fire Resistance and Water Breathing kept topped up while the ring is worn (so lava is as easy
 * to see through as with the potion), marked ambient and shown without swirls - which is how the ring's own are told
 * apart from a drunk potion's or a beacon's, and taken away again when the ring comes off.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmberRing {
    private static final int EFFECT_TICKS = 300;
    private static final int EFFECT_REFRESH = 100;

    private AmberRing() {
    }

    /** The ring is worn by keeping it in the off-hand (left hand) slot (the Elder ring does all it does). */
    public static boolean wears(Player player) {
        return player.getOffhandItem().getItem() instanceof AmberRingItem || Rings.wearsElder(player);
    }

    // whatever gets past the effects (nothing should): no fire damage of any kind
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FIRE) && wears(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) return;
        boolean wearing = wears(player);
        if (wearing) {
            // never alight: the flames go out the moment they catch
            if (player.getRemainingFireTicks() > 0) player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
        }
        if (player.tickCount % 10 != 0) return;
        keep(player, MobEffects.FIRE_RESISTANCE, wearing);
        keep(player, MobEffects.WATER_BREATHING, wearing);
    }

    private static void keep(Player player, Holder<MobEffect> effect, boolean wearing) {
        MobEffectInstance active = player.getEffect(effect);
        boolean ours = active != null && active.isAmbient() && !active.isVisible();
        if (wearing) {
            if (active == null || ours && active.getDuration() < EFFECT_REFRESH) {
                player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, true));
            }
        } else if (ours) {
            player.removeEffect(effect);
        }
    }
}
