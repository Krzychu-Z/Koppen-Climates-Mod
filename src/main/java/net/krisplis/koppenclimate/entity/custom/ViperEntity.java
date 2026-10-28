package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Mediterranean viper (asp viper): a short, thick, zigzag-backed snake of the Csa's dry hillsides. It behaves
 * like a cobra - hunts small animals, slips away from people, hisses a warning at whoever surprises or corners it
 * and strikes if pressed - but it never rears up or spits, and its bite is milder: a short, weak poison and no
 * paralysis. It is visibly smaller than a cobra, and cats hunt it just the same.
 */
public class ViperEntity extends CobraEntity {
    private static final int VENOM_TICKS = 100;

    public ViperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    // it hisses where it lies, but never rears up
    @Override
    void setRearing(boolean rearing) {
        super.setRearing(false);
    }

    @Override
    boolean canSpit() {
        return false;
    }

    @Override
    protected void envenom(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 0), this);
    }
}
