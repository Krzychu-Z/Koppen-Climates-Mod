package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A friendly giraffe, as tall as the savanna's acacias. Pat it with your right hand (use it with an empty main
 * hand) and it lowers its long neck, lets you climb onto its head and lifts you up to the top, where you ride
 * along wherever it wanders; sneak to climb down. It eats acacia leaves, which also breed it.
 */
public class GiraffeEntity extends Animal {
    private static final EntityDataAccessor<Integer> LIFT = SynchedEntityData.defineId(GiraffeEntity.class, EntityDataSerializers.INT);
    // ticks to raise a rider from the ground to the top of the neck
    public static final int LIFT_TICKS = 50;
    // where the rider sits: on the head when it's up, by the ground in front when it's down (in blocks)
    private static final double TOP_HEIGHT = 10.8, TOP_FORWARD = 2.7;
    private static final double LOW_HEIGHT = 2.4, LOW_FORWARD = 5.0;

    private int prevLift;

    public GiraffeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ticks until the giraffe can kick again
    private int kickCooldown;

    /**
     * A giraffe fends off lions the way an iron golem does: a lion that is after it and comes within reach is
     * kicked hard and flung up into the air.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.isBaby()) return;
        if (this.kickCooldown > 0) {
            this.kickCooldown--;
            return;
        }
        LionEntity lion = this.level().getNearestEntity(LionEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat(),
                this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(2.5, 1.0, 2.5));
        if (lion == null || (lion.getTarget() != this && this.getLastHurtByMob() != lion)) return;
        this.kickCooldown = 20;
        this.getLookControl().setLookAt(lion);
        if (lion.hurt(this.damageSources().mobAttack(this), 8.0F)) {
            lion.setDeltaMovement(lion.getDeltaMovement().add((lion.getX() - this.getX()) * 0.15, 0.8, (lion.getZ() - this.getZ()) * 0.15));
            lion.hurtMarked = true;
        }
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIFT, LIFT_TICKS);
    }

    /** 0 = neck down to the ground for a rider to climb on, 1 = up at full height. */
    public float getLiftProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevLift, this.entityData.get(LIFT)) / LIFT_TICKS;
    }

    @Override
    public void tick() {
        this.prevLift = this.entityData.get(LIFT);
        super.tick();
        if (!this.level().isClientSide) {
            int lift = this.entityData.get(LIFT);
            if (lift < LIFT_TICKS) this.entityData.set(LIFT, lift + 1);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && held.isEmpty() && !this.isBaby() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide) {
                // down comes the head, on climbs the rider, and up it goes
                this.entityData.set(LIFT, 0);
                this.prevLift = 0;
                player.startRiding(this);
                this.playSound(SoundEvents.CAMEL_SIT, 0.6F, 1.3F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float t = this.getLiftProgress(1.0F);
        double height = Mth.lerp(t, LOW_HEIGHT, TOP_HEIGHT);
        double forward = Mth.lerp(t, LOW_FORWARD, TOP_FORWARD);
        return new Vec3(0.0, height, forward).yRot(-this.yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ACACIA_LEAVES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.GIRAFFE.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }
}
