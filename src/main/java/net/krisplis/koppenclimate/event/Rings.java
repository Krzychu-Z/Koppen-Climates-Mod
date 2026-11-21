package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.krisplis.koppenclimate.item.custom.AmberRingItem;
import net.krisplis.koppenclimate.item.custom.ElderRingItem;
import net.krisplis.koppenclimate.item.custom.GraphiteRingItem;
import net.krisplis.koppenclimate.item.custom.JadeRingItem;
import net.krisplis.koppenclimate.item.custom.RubyRingItem;
import net.krisplis.koppenclimate.item.custom.SapphireRingItem;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * What all the rings have in common - the jade, amber, ruby, sapphire and graphite rings, and the Elder ring forged
 * from all five. Each is worn only in the off-hand (left hand) slot, so only one ring is ever worn at a time; what
 * each does while it's worn is in its own class (JadeRing, AmberRing, RubyRing, SapphireRing) or here (the graphite
 * ring's, the Elder ring's).
 * <ul>
 *   <li>Wear: each time a ring is put on it wears a little (one point of its fifty); worn out, it crumbles to
 *       nothing. The Elder ring never wears.</li>
 *   <li>Taking a ring off hurts: the jade ring leaves you seeing visions (the Potion of Delirium's psychedelia and
 *       nausea, 45 seconds); the amber ring sets you on fire - or, under water, empties your lungs; the ruby ring
 *       leaves you blind and withering; the sapphire ring flings you up as a shulker's bullet does (levitation, ten
 *       seconds); the graphite ring starves you - your hunger bar emptied and held empty for ten seconds, each one
 *       taking life from you, the light flaring blindingly bright (night vision) - and leaves it empty; the Elder ring
 *       brings on the Wither and roots you to the spot. (A ring used up - the ruby's one life, or worn out - is gone,
 *       not taken off: no price for that.)</li>
 *   <li>The graphite ring: whatever hits you is hit back - the same damage, the same knockback.</li>
 *   <li>The Elder ring: all of the above, and the strongest Regeneration as long as it's worn.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Rings {
    public static final int WEAR = 50;
    private static final String WORN = KoppenClimateMod.MOD_ID + ":ring_worn";
    private static final String SPENT = KoppenClimateMod.MOD_ID + ":ring_spent";
    private static final String STARVED = KoppenClimateMod.MOD_ID + ":starved_until";
    private static final int PENALTY = 200;
    // the Elder ring's Regeneration: the strongest that still shows as a level (V)
    private static final int HEALING = 4;

    private Rings() {
    }

    enum Ring {JADE, AMBER, RUBY, SAPPHIRE, GRAPHITE, ELDER}

    @Nullable
    static Ring ring(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof JadeRingItem) return Ring.JADE;
        if (item instanceof AmberRingItem) return Ring.AMBER;
        if (item instanceof RubyRingItem) return Ring.RUBY;
        if (item instanceof SapphireRingItem) return Ring.SAPPHIRE;
        if (item instanceof GraphiteRingItem) return Ring.GRAPHITE;
        if (item instanceof ElderRingItem) return Ring.ELDER;
        return null;
    }

    public static boolean wearsElder(Player player) {
        return player.getOffhandItem().getItem() instanceof ElderRingItem;
    }

    public static boolean wearsGraphite(Player player) {
        Item item = player.getOffhandItem().getItem();
        return item instanceof GraphiteRingItem || item instanceof ElderRingItem;
    }

    /** The ring in the off-hand was used up (not taken off): no price for its going. */
    public static void spent(Player player) {
        player.getPersistentData().putBoolean(SPENT, true);
    }

    // ---------------------------------------------------------------- putting rings on and taking them off

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        starving(player, data);
        if (player.isDeadOrDying()) {
            data.remove(WORN);
            return;
        }
        ItemStack held = player.getOffhandItem();
        Ring now = ring(held);
        Ring was = data.contains(WORN) ? Ring.values()[Math.min(data.getInt(WORN), Ring.values().length - 1)] : null;
        if (now != was) {
            if (was != null) {
                if (data.getBoolean(SPENT)) data.remove(SPENT);
                else takenOff(player, was, data);
            }
            if (now != null) {
                data.remove(SPENT);
                if (now != Ring.ELDER && !player.getAbilities().instabuild) {
                    held.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
                    // worn out: it crumbled the moment it went on
                    if (held.isEmpty()) {
                        data.remove(WORN);
                        return;
                    }
                }
            }
            if (now == null) data.remove(WORN);
            else data.putInt(WORN, now.ordinal());
        }
        if (now == Ring.ELDER && player.tickCount % 10 == 0) keep(player, MobEffects.REGENERATION, HEALING);
        if (now != Ring.ELDER && player.tickCount % 10 == 0) drop(player, MobEffects.REGENERATION, HEALING);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag data = event.getEntity().getPersistentData();
        data.remove(WORN);
        data.remove(SPENT);
        data.remove(STARVED);
    }

    static void takenOff(ServerPlayer player, Ring ring, CompoundTag data) {
        switch (ring) {
            case JADE -> {
                player.addEffect(new MobEffectInstance(ModEffects.PSYCHEDELIA.getHolder().orElseThrow(), 900, 0));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 900, 0));
            }
            case AMBER -> {
                if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) player.setAirSupply(0);
                else player.igniteForSeconds(8.0F);
            }
            case RUBY -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, PENALTY, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, PENALTY, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, PENALTY, 1));
            }
            // as a shulker's bullet does
            case SAPPHIRE -> player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, PENALTY, 0));
            case GRAPHITE -> {
                data.putLong(STARVED, player.level().getGameTime() + PENALTY);
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, PENALTY, 255));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, PENALTY, 0));
                player.getFoodData().setFoodLevel(0);
                player.getFoodData().setSaturation(0.0F);
            }
            case ELDER -> {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, PENALTY, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PENALTY, 127));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7F, 0.6F);
            }
        }
    }

    // the graphite ring's starving: nothing eaten stays down for ten seconds, and each takes life
    private static void starving(ServerPlayer player, CompoundTag data) {
        if (!data.contains(STARVED)) return;
        long left = data.getLong(STARVED) - player.level().getGameTime();
        if (left < 0 || left > PENALTY) {
            data.remove(STARVED);
            player.getFoodData().setFoodLevel(0);
            return;
        }
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        if (left % 20 == 0 && !player.isDeadOrDying()) player.hurt(player.damageSources().starve(), 1.0F);
    }

    private static void keep(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance active = player.getEffect(effect);
        boolean ours = active != null && active.isAmbient() && !active.isVisible();
        if (active == null || ours && active.getDuration() < 100) {
            player.addEffect(new MobEffectInstance(effect, 300, amplifier, true, false, true));
        }
    }

    private static void drop(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && active.isAmbient() && !active.isVisible() && active.getAmplifier() == amplifier) player.removeEffect(effect);
    }

    // ---------------------------------------------------------------- the graphite ring: every blow paid back

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !wearsGraphite(player)) return;
        DamageSource source = event.getSource();
        // the attacker (for an arrow or a fireball, whoever shot it) - but not thorns (two such rings would go on forever)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.THORNS) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == player || !attacker.isAlive()) return;
        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        attacker.hurt(player.damageSources().thorns(player), amount);
        // the knockback the blow gives the wearer, given back the other way
        double strength = 0.4 + (attacker.getAttributes().hasAttribute(Attributes.ATTACK_KNOCKBACK)
                ? attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK) * 0.5 : 0.0);
        Vec3 away = attacker.position().subtract(player.position());
        attacker.knockback(strength, -away.x, -away.z);
        attacker.hurtMarked = true;
    }
}
