package net.krisplis.koppenclimate.item.custom;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Nerium (oleander) twigs: straight, hard and poisonous, they'll do for arrows. A bow or crossbow shoots them (they're
 * in the arrows tag), and whatever one hits is poisoned - Poison II for eight seconds. They fly and hit like an arrow,
 * and can be picked up again as twigs. They don't keep their poison in the twigs themselves, so they still stack.
 */
public class NeriumTwigsItem extends ArrowItem {
    public static final int POISON_TICKS = 160, POISON_AMPLIFIER = 1;

    public NeriumTwigsItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new TwigArrow(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        TwigArrow arrow = new TwigArrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    /** An arrow that poisons what it hits (an ordinary arrow to the client; it's only the hit that differs). */
    static class TwigArrow extends Arrow {
        TwigArrow(Level level, LivingEntity shooter, ItemStack pickup, @Nullable ItemStack weapon) {
            super(level, shooter, pickup, weapon);
        }

        TwigArrow(Level level, double x, double y, double z, ItemStack pickup) {
            super(level, x, y, z, pickup, null);
        }

        @Override
        protected void doPostHurtEffects(LivingEntity target) {
            super.doPostHurtEffects(target);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, POISON_AMPLIFIER), this.getEffectSource());
        }
    }
}
