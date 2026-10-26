package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The African elephant: huge, slow and peaceful - until it's hurt. Then it trumpets, and it and the rest of its
 * herd charge whoever did it; a charging elephant hits hard enough to kill. It drops ivory tusks.
 */
public class ElephantEntity extends Animal {
    public ElephantEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // the charge
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.8, true));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // hurt one, and the herd turns on you
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ElephantEntity.class));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            this.playSound(ModSounds.ELEPHANT.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        return hurt;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SUGAR_CANE) || stack.is(Items.MELON_SLICE);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.ELEPHANT.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ELEPHANT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ELEPHANT.get();
    }
}
