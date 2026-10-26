package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A termite of the African savanna. Termites live around their colony's mound (block/custom/TermiteMoundBlock):
 * they scurry about near it and harm nothing. Break into the mound, or hit one of them, and the colony pours out
 * to bite back for a while (weakly), then calms down again. They wander off and vanish when no one is near, and
 * the mound sends out more.
 */
public class TermiteEntity extends PathfinderMob {
    private static final int HOME_RADIUS = 7;
    // how long a termite stays angry after it's been roused, in ticks
    public static final int ANGER_TICKS = 300;

    @Nullable
    private BlockPos home;
    private int angerTicks;

    public TermiteEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // one bitten termite rouses the others
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(TermiteEntity.class));
    }

    public void setHome(BlockPos mound) {
        this.home = mound.immutable();
        this.restrictTo(this.home, HOME_RADIUS);
    }

    /** Roused: go for this one for a while. */
    public void rouse(LivingEntity attacker) {
        this.setTarget(attacker);
        this.angerTicks = ANGER_TICKS;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null && this.angerTicks <= 0) this.angerTicks = ANGER_TICKS;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.angerTicks > 0 && --this.angerTicks == 0) {
            this.setTarget(null);
            this.setLastHurtByMob(null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) tag.put("Home", NbtUtils.writeBlockPos(this.home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    // little rustling insect sounds, like a silverfish but higher
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.08F, 1.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }
}
