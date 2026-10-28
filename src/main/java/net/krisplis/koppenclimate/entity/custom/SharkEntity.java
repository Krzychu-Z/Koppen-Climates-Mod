package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A shark of the warm seas. It's extremely aggressive: anything swimming within a long way - players, villagers,
 * animals, fish, squid, dolphins - is hunted down at speed and bitten hard, and a player bobbing in a boat over the
 * water is fair game too. Only getting out of the water saves you. Stranded on land, it flops and slowly suffocates.
 */
public class SharkEntity extends WaterAnimal {
    private static final EntityDataAccessor<Boolean> HUNTING = SynchedEntityData.defineId(SharkEntity.class, EntityDataSerializers.BOOLEAN);

    public SharkEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 1.4)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUNTING, false);
    }

    /** Whether it's chasing something (the model opens its jaws). */
    public boolean isHunting() {
        return this.entityData.get(HUNTING);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, true));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(SharkEntity.class));
        // no need to see its prey: it smells it from afar, and checks often
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 2, false, false, this::isPrey));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 4, false, false, this::isPrey));
    }

    /** Anything in (or floating right on) the water, except other sharks. */
    private boolean isPrey(LivingEntity entity) {
        if (entity instanceof SharkEntity || this.level().getDifficulty() == Difficulty.PEACEFUL) return false;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (!inOrOnWater(entity)) return false;
        return entity instanceof Player || entity instanceof AbstractVillager || entity instanceof Animal
                || entity instanceof AbstractFish || entity instanceof WaterAnimal;
    }

    private static boolean inOrOnWater(LivingEntity entity) {
        return entity.isInWater() || entity.level().getFluidState(entity.blockPosition().below()).is(FluidTags.WATER)
                && entity.level().getFluidState(entity.blockPosition()).isEmpty() && entity.getVehicle() != null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        // once its prey is out of the water, it gives up
        if (target != null && (!target.isAlive() || !inOrOnWater(target) && this.tickCount % 20 == 0)) {
            this.setTarget(null);
            target = null;
        }
        this.entityData.set(HUNTING, target != null);
    }

    // fast in the water, and faster still when it has something to chase
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(input);
        }
    }

    // stranded: it thrashes about, like a dolphin on the shore
    @Override
    public void aiStep() {
        if (!this.isInWater() && this.onGround() && this.verticalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.1F, 0.3F,
                    (this.random.nextFloat() * 2.0F - 1.0F) * 0.1F));
            this.setOnGround(false);
            this.hasImpulse = true;
            this.playSound(SoundEvents.COD_FLOP, 1.0F, 0.5F);
        }
        super.aiStep();
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }
}
