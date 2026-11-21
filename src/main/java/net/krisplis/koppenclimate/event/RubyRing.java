package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.RubyRingItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What wearing the ruby ring (in the off-hand slot) does: it saves you from dying once, as a totem of undying does.
 * When a player wearing it would die, the ring brings them back - half a heart left, their effects cleared, then
 * Regeneration II, Absorption II and Fire Resistance to see them through, in a burst of the totem's sparks and its
 * sound - and the ring is destroyed doing it. As with a totem, nothing saves you from the void or from /kill.
 * (A real totem held in the main hand is still used first, as vanilla does before anyone dies.)
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RubyRing {
    private RubyRing() {
    }

    /** The ring is worn by keeping it in the off-hand (left hand) slot (the Elder ring does all it does). */
    public static boolean wears(Player player) {
        return player.getOffhandItem().getItem() instanceof RubyRingItem || Rings.wearsElder(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !wears(player)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setCanceled(true);
        // the ring shatters, as a totem does (the Elder ring doesn't: it turns death away as often as it comes)
        if (!Rings.wearsElder(player)) {
            player.getOffhandItem().shrink(1);
            Rings.spent(player);
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(net.krisplis.koppenclimate.item.ModItems.RUBY_RING.get()));
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.clearFire();
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 80, 0.5, 0.6, 0.5, 0.5);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
