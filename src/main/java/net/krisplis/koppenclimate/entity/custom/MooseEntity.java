package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The moose of the Cold lakeland (Dfc): huge, mostly placid, grazing by the lakes - but mildly aggressive. Walk up
 * close (within about 5 blocks, more if it has a calf about) and now and then it takes against you: it snorts,
 * lowers its antlers and charges. One good butt and it's made its point and goes back to grazing (or it gives up
 * after a few seconds). Hit it, though, and it fights you in earnest.
 */
public class MooseEntity extends Animal {
    private static final int CHARGE_TICKS = 160;
    private int charge;
    private boolean provoked;

    public MooseEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new IrritatedGoal());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity) this.provoked = true;
        return hurt;
    }

    // a warning butt: having landed one, a moose that wasn't provoked lets the matter drop
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.provoked) this.calmDown();
        return hit;
    }

    private void calmDown() {
        this.setTarget(null);
        this.setAggressive(false);
        this.charge = 0;
        this.provoked = false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.provoked = false;
            this.charge = 0;
        } else if (!this.provoked && this.charge > 0 && --this.charge == 0) {
            this.calmDown();
        }
    }

    private boolean calfNearby() {
        return !this.level().getEntitiesOfClass(MooseEntity.class, this.getBoundingBox().inflate(10.0), MooseEntity::isBaby).isEmpty();
    }

    /** Takes against a player who comes too close, now and then. */
    private class IrritatedGoal extends Goal {
        private Player victim;

        IrritatedGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            MooseEntity moose = MooseEntity.this;
            if (moose.getTarget() != null || moose.isBaby() || moose.level().getDifficulty() == Difficulty.PEACEFUL) return false;
            if (moose.getRandom().nextInt(20) != 0) return false;
            boolean calf = moose.calfNearby();
            Player player = moose.level().getNearestPlayer(moose, calf ? 8.0 : 5.0);
            if (player == null || player.isCreative() || player.isSpectator() || !moose.hasLineOfSight(player)) return false;
            if (moose.getRandom().nextInt(calf ? 2 : 6) != 0) return false;
            this.victim = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            MooseEntity moose = MooseEntity.this;
            moose.setTarget(this.victim);
            moose.charge = CHARGE_TICKS;
            moose.playSound(SoundEvents.HOGLIN_ANGRY, 1.2F, 0.6F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.MOOSE.get().create(level);
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
        return super.getVoicePitch() * 0.7F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
