package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.JadeRingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What wearing the jade ring (in the off-hand slot) does: Speed V - far beyond any potion of Swiftness - and total immunity to
 * poisoning - no poison of any kind takes hold (vanilla's, from spiders, pufferfish, potions and snake bites, nor
 * the oleander's), and any already in the blood is gone the moment the ring goes on.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JadeRing {
    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "jade_ring");
    // (the two golden hearts the ring used to give)
    private static final float GOLDEN_HEALTH = 4.0F;
    private static final int EFFECT_TICKS = 300;
    private static final int EFFECT_REFRESH = 100;
    // Speed V: twice a walking pace (a potion of Swiftness gives at most Speed II)
    private static final int SPEED_LEVEL = 4;

    private JadeRing() {
    }

    /** The ring is worn by keeping it in the off-hand (left hand) slot (the Elder ring does all it does). */
    public static boolean wears(Player player) {
        return player.getOffhandItem().getItem() instanceof JadeRingItem || Rings.wearsElder(player);
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (isPoison(event.getEffectInstance().getEffect()) && event.getEntity() instanceof Player player && wears(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean isPoison(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect == MobEffects.POISON || effect.is(net.krisplis.koppenclimate.effect.ModEffects.OLEANDER_POISONING.getId());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide || player.tickCount % 10 != 0) return;
        AttributeInstance maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) return;
        boolean wearing = wears(player);
        // speed, as from the strongest potion of Swiftness (II): kept topped up while the ring is worn. The ring's own
        // effect is marked ambient (and shows no swirls), which is how it is told apart from a drunk potion when the
        // ring comes off (a beacon's speed shows swirls). (The ring used to make you invisible: any of that still about goes too.)
        net.minecraft.world.effect.MobEffectInstance fast = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (wearing) {
            if (fast == null || (fast.isAmbient() && !fast.isVisible() && fast.getDuration() < EFFECT_REFRESH) || fast.getAmplifier() < SPEED_LEVEL) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, SPEED_LEVEL,
                        true, false, true));
            }
        } else if (fast != null && fast.isAmbient() && !fast.isVisible() && fast.getAmplifier() == SPEED_LEVEL) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
        net.minecraft.world.effect.MobEffectInstance invisible = player.getEffect(MobEffects.INVISIBILITY);
        if (invisible != null && invisible.isAmbient()) player.removeEffect(MobEffects.INVISIBILITY);
        if (wearing) {
            for (net.minecraft.world.effect.MobEffectInstance active : java.util.List.copyOf(player.getActiveEffects())) {
                if (isPoison(active.getEffect())) player.removeEffect(active.getEffect());
            }
        }
        // (the ring once gave two golden hearts: take them away from anyone who still has them)
        if (maxAbsorption.hasModifier(MODIFIER)) {
            maxAbsorption.removeModifier(MODIFIER);
            player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - GOLDEN_HEALTH));
        }
    }
}
