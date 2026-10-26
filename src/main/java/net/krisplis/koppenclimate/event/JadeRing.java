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

/** What wearing the jade ring (in the off-hand slot) does: invisibility, as from the potion; no poison takes hold, and two golden hearts that come back after a minute. */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JadeRing {
    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "jade_ring");
    // two golden hearts
    private static final float GOLDEN_HEALTH = 4.0F;
    // spent golden hearts come back this long after they were last topped up
    private static final int REFILL_TICKS = 1200;
    private static final int INVISIBILITY_TICKS = 300;
    private static final int INVISIBILITY_REFRESH = 100;
    private static final String LAST_REFILL = "koppenclimatemod:jade_ring_refill";

    private JadeRing() {
    }

    /** The ring is worn by keeping it in the off-hand (left hand) slot. */
    public static boolean wears(Player player) {
        return player.getOffhandItem().getItem() instanceof JadeRingItem;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.POISON && event.getEntity() instanceof Player player && wears(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide || player.tickCount % 10 != 0) return;
        AttributeInstance maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) return;
        boolean wearing = wears(player);
        // invisibility, as from a potion: kept topped up while the ring is worn. The ring's own effect is marked
        // ambient (and shows no swirls), which is how it is told apart from a drunk potion when the ring comes off.
        net.minecraft.world.effect.MobEffectInstance invisible = player.getEffect(MobEffects.INVISIBILITY);
        if (wearing) {
            if (invisible == null || (invisible.isAmbient() && invisible.getDuration() < INVISIBILITY_REFRESH)) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0,
                        true, false, true));
            }
        } else if (invisible != null && invisible.isAmbient()) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
        if (wearing) {
            if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
            if (!maxAbsorption.hasModifier(MODIFIER)) {
                maxAbsorption.addTransientModifier(new AttributeModifier(MODIFIER, GOLDEN_HEALTH, AttributeModifier.Operation.ADD_VALUE));
            }
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong(LAST_REFILL);
            if (player.getAbsorptionAmount() < GOLDEN_HEALTH && now - last >= REFILL_TICKS) {
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), GOLDEN_HEALTH));
                player.getPersistentData().putLong(LAST_REFILL, now);
            }
        } else if (maxAbsorption.hasModifier(MODIFIER)) {
            maxAbsorption.removeModifier(MODIFIER);
            // take away the ring's golden hearts with it
            player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - GOLDEN_HEALTH));
        }
    }
}
