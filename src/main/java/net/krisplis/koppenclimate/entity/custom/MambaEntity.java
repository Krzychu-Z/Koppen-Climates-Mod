package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The black mamba of the African savanna: the deadliest snake in the mod. Long, grey and very fast, it hunts
 * rabbits, armadillos and termites, and strikes at any player who comes close (from farther off than a cobra
 * warns, and without warning). Its bite is a strong blow with a fast, deadly venom (strong poison and wither) -
 * an unarmoured player is dead within a few seconds of a couple of bites. It raises the front of its body
 * when it goes for something (it uses the cobra's body and rearing, without the hood).
 */
public class MambaEntity extends CobraEntity {
    private static final int HUNT_INTERVAL = 30;
    private static final double STRIKE_DISTANCE = 6.0;

    public MambaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)   // one blow of an iron (or better) sword or axe
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p.distanceTo(this) < (p.isDiscrete() ? STRIKE_DISTANCE / 2 : STRIKE_DISTANCE)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Rabbit.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Armadillo.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, TermiteEntity.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.AbstractVillager.class,
                HUNT_INTERVAL, true, false, v -> v.distanceTo(this) < STRIKE_DISTANCE));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            // the front of the body goes up as it closes on something
            this.setRearing(target != null && target.isAlive() && this.distanceToSqr(target) < 16.0);
        }
    }

    // a heavy bite and a fast, deadly venom
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof net.minecraft.world.entity.animal.Cat) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2), this);
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1), this);
        }
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 0.9F + this.random.nextFloat() * 0.1F;
    }
}
