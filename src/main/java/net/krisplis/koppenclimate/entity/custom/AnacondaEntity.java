package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

// A green anaconda: neutral to players (it fights back when hurt), hunts small animals,
// and its bite constricts, slowing whatever it hits. It swims at the surface.
// When idle it curls up in a coil to rest, like a sleeping fox: hitting it, prey to hunt,
// or a player walking right up (not sneaking) wakes it.
public class AnacondaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> COILED = SynchedEntityData.defineId(AnacondaEntity.class, EntityDataSerializers.BOOLEAN);
    // how fast the model curls up or stretches out, per tick
    private static final float COIL_BLEND_SPEED = 0.12F;

    // client side: 0 = stretched out, 1 = fully coiled, eased toward the synced state
    private float coilAmount;
    private float prevCoilAmount;
    // Slowness II for 3 seconds after a hit
    private static final int SQUEEZE_TICKS = 60;
    private static final int SQUEEZE_AMPLIFIER = 1;
    // how often (1 in N ticks) it looks for prey
    private static final int HUNT_INTERVAL = 40;
    // the model is about 5.5 blocks long but the hitbox is short, so widen the box used for culling
    private static final double MODEL_REACH = 3.5;

    public AnacondaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COILED, false);
    }

    public boolean isCoiled() {
        return this.entityData.get(COILED);
    }

    public void setCoiled(boolean coiled) {
        this.entityData.set(COILED, coiled);
    }

    public float getCoilAmount(float partialTick) {
        return Mth.lerp(partialTick, this.prevCoilAmount, this.coilAmount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Coiled", this.isCoiled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCoiled(tag.getBoolean("Coiled"));
    }

    @Override
    public void tick() {
        super.tick();
        this.prevCoilAmount = this.coilAmount;
        this.coilAmount += ((this.isCoiled() ? 1.0F : 0.0F) - this.coilAmount) * COIL_BLEND_SPEED;
        // something to hunt or fight: uncoil
        if (!this.level().isClientSide && this.isCoiled() && this.getTarget() != null) {
            this.setCoiled(false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.setCoiled(false);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(2, new CoilUpGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Chicken.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Rabbit.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Frog.class, HUNT_INTERVAL, true, false, null));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SQUEEZE_TICKS, SQUEEZE_AMPLIFIER), this);
        }
        return hit;
    }

    // stays put like animals do; creature spawns are rare, so a despawned anaconda wouldn't come back
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(MODEL_REACH, 1.0, MODEL_REACH);
    }

    // a low hiss, now and then
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_HISS;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HISS;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F + this.random.nextFloat() * 0.1F;
    }

    // snakes don't make footsteps
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // Curls up to rest when there is nothing to do, more readily by day. While it runs it holds
    // movement and looking, so the snake stays put; it ends on its own now and then, or when disturbed.
    static class CoilUpGoal extends Goal {
        // chance per tick (1 in N) to settle down when idle
        private static final int SETTLE_CHANCE_DAY = 100;
        private static final int SETTLE_CHANCE_NIGHT = 400;
        // chance per tick (1 in N) to stir on its own: about two minutes on average
        private static final int STIR_CHANCE = 2400;
        // a player this close who isn't sneaking wakes it, like foxes
        private static final double DISTURB_RANGE = 3.0;

        private final AnacondaEntity snake;

        CoilUpGoal(AnacondaEntity snake) {
            this.snake = snake;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!this.canRest()) return false;
            // a snake that was coiled when the world was saved goes straight back to resting
            if (this.snake.isCoiled()) return true;
            int chance = this.snake.level().isDay() ? SETTLE_CHANCE_DAY : SETTLE_CHANCE_NIGHT;
            return this.snake.getNavigation().isDone() && this.snake.getRandom().nextInt(chance) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canRest() && this.snake.getRandom().nextInt(STIR_CHANCE) != 0;
        }

        @Override
        public void start() {
            this.snake.getNavigation().stop();
            this.snake.setCoiled(true);
        }

        @Override
        public void stop() {
            this.snake.setCoiled(false);
        }

        private boolean canRest() {
            return this.snake.getTarget() == null && this.snake.onGround() && !this.snake.isInWater() && !this.playerClose();
        }

        private boolean playerClose() {
            return this.snake.level().getNearestPlayer(this.snake.getX(), this.snake.getY(), this.snake.getZ(), DISTURB_RANGE,
                    e -> !e.isSpectator() && !e.isDiscrete()) != null;
        }
    }
}
