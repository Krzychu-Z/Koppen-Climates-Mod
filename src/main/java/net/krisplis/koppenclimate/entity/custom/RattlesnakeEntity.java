package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
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
 * The rattlesnake of the continental deserts (BWk): a viper's size, and a cobra's ways - it hunts small animals,
 * slips away from people, and warns off anyone who comes too close - but its warning is the dry buzz of its rattle,
 * the tail raised and shaking, and its bite is deadly: a strong poison that withers a player to death unless it's
 * cured (milk). It never spits.
 */
public class RattlesnakeEntity extends CobraEntity {
    private static final int VENOM_TICKS = 900;

    public RattlesnakeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    boolean canSpit() {
        return false;
    }

    // the venom: it withers away whatever it bit, a player included, unless cured
    @Override
    protected void envenom(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, VENOM_TICKS, 1), this);
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), this);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1), this);
    }

    @Override
    protected SoundEvent warningSound() {
        return ModSounds.RATTLESNAKE_RATTLE.get();
    }

    @Override
    protected float warningPitch() {
        return 0.9F + this.random.nextFloat() * 0.2F;
    }

    // the rattle buzzes on and on while it's alarmed
    @Override
    protected int warningInterval() {
        return 18;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
