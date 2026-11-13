package net.krisplis.koppenclimate.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Oleander poisoning: what a fall into a nerium bush does. Unlike vanilla's poison it doesn't stop at half a heart -
 * it takes half a heart every second and a half, slowly, for a minute, which is more than a full-health player has.
 * Milk (or a totem) saves you; eating and healing only buy time.
 */
public class OleanderPoisoningEffect extends MobEffect {
    /** Ticks between each half-heart lost. */
    public static final int INTERVAL = 30;
    /** How long a fall into the bush poisons for: 40 half-hearts' worth. */
    public static final int DURATION = 1200;

    protected OleanderPoisoningEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 1.0F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(5, INTERVAL >> amplifier);
        return duration % interval == 0;
    }
}
