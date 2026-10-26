package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowFlockLeaderGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The red-bellied piranha of the Amazon rivers. It swims in schools and is very fast. It attacks anything that
 * swims (players, animals and other fish, but never its own kind or the big river predators), and when one of
 * them is hurt, the whole school turns on whatever did it. It can't leave the water, so stepping out is safety.
 */
public class PiranhaEntity extends AbstractSchoolingFish {
    // how hard it accelerates, compared to an ordinary fish's 0.01: cruising, and chasing prey
    private static final float CRUISE_THRUST = 0.02F;
    private static final float CHASE_THRUST = 0.045F;
    private static final int MAX_SCHOOL = 12;
    private static final int HUNT_INTERVAL = 5;

    public PiranhaEntity(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        // no fleeing from players like other fish: a school of piranhas does the hunting
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 2.0, true));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 20));
        this.goalSelector.addGoal(5, new FollowFlockLeaderGoal(this));

        // one bitten piranha sets the whole school on the attacker
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(PiranhaEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, HUNT_INTERVAL, true, false,
                PiranhaEntity::isPrey));
    }

    /** Anything in the water that a school would go for: players, land animals and other fish. */
    private static boolean isPrey(LivingEntity entity) {
        if (!entity.isInWater() || entity instanceof PiranhaEntity) {
            return false;
        }
        return entity instanceof Player || entity instanceof Animal || entity instanceof AbstractFish
                || entity instanceof net.minecraft.world.entity.npc.AbstractVillager;
    }

    // Pushes harder than an ordinary fish, and harder still when it has something to chase
    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getTarget() != null ? CHASE_THRUST : CRUISE_THRUST, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(input);
        }
    }

    @Override
    public int getMaxSchoolSize() {
        return MAX_SCHOOL;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.PIRANHA_BUCKET.get());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }
}
