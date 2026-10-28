package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * A brown eagle of the hot desert. It soars in wide circles high over the sand, drifting slowly about, and when it
 * spots prey it stoops on it in a steep dive, strikes, and climbs back up to circle again. It hunts rabbits at any
 * hour, and players too by night. Hit it and it turns on you. It never lands.
 */
public class EagleEntity extends PathfinderMob {
    private static final double CIRCLE_SPEED = 0.32, DIVE_SPEED = 0.8, CLIMB_SPEED = 0.4;
    private static final int ALTITUDE_MIN = 18, ALTITUDE_EXTRA = 10;
    private static final double HUNT_RANGE = 36.0;
    // after a strike, how long it climbs before it looks for prey again
    private static final int RECOVER_TICKS = 80;

    private BlockPos anchor;
    private double radius = 14.0, angle;
    private int recover;

    public EagleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.angle = this.random.nextDouble() * Mth.TWO_PI;
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /** On open sand under the sky, like any desert animal; it takes to the air at once. */
    public static boolean checkEagleSpawnRules(EntityType<EagleEntity> type, LevelAccessor level, MobSpawnType reason,
                                               BlockPos pos, net.minecraft.util.RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.canSeeSky(pos);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    // ---------------------------------------------------------------- flight

    @Override
    public void travel(Vec3 input) {
        // it steers itself (see aiStep): just carry it along
        if (this.isControlledByLocalInstance()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        if (this.anchor == null || this.tickCount % 600 == 0) this.newAnchor();
        if (this.recover > 0) this.recover--;

        LivingEntity target = this.getTarget();
        if (target != null && !this.stillPrey(target)) {
            this.setTarget(null);
            target = null;
        }
        if (target == null && this.recover == 0 && this.tickCount % 20 == 0) {
            target = this.findPrey();
            this.setTarget(target);
        }

        Vec3 goal;
        double speed;
        if (target != null) {
            // the stoop: straight at the prey
            goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
            speed = DIVE_SPEED;
            if (this.getBoundingBox().inflate(0.6).intersects(target.getBoundingBox())) {
                this.doHurtTarget(target);
                this.playSound(ModSounds.EAGLE.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
                this.setTarget(null);
                this.recover = RECOVER_TICKS;
            }
        } else {
            // round and round, high up
            this.angle += CIRCLE_SPEED / this.radius;
            goal = new Vec3(this.anchor.getX() + 0.5 + Math.cos(this.angle) * this.radius,
                    this.anchor.getY() + Math.sin(this.angle * 0.5) * 1.5,
                    this.anchor.getZ() + 0.5 + Math.sin(this.angle) * this.radius);
            speed = this.getY() < this.anchor.getY() - 4 ? CLIMB_SPEED : CIRCLE_SPEED;
        }
        Vec3 desired = goal.subtract(this.position());
        if (desired.lengthSqr() > 1.0E-4) desired = desired.normalize().scale(speed);
        Vec3 v = this.getDeltaMovement().lerp(desired, target != null ? 0.2 : 0.08);
        this.setDeltaMovement(v);
        // face the way it flies, nose down in a dive
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        if (horizontal > 1.0E-3) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
        this.setXRot((float) (-Mth.atan2(v.y, horizontal) * Mth.RAD_TO_DEG));
    }

    private void newAnchor() {
        BlockPos at = this.anchor == null ? this.blockPosition()
                : this.anchor.offset(this.random.nextInt(49) - 24, 0, this.random.nextInt(49) - 24);
        int ground = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
        this.anchor = new BlockPos(at.getX(), ground + ALTITUDE_MIN + this.random.nextInt(ALTITUDE_EXTRA), at.getZ());
        this.radius = 10.0 + this.random.nextDouble() * 10.0;
    }

    private boolean night() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private boolean stillPrey(LivingEntity target) {
        if (!target.isAlive() || this.distanceToSqr(target) > HUNT_RANGE * HUNT_RANGE * 1.5) return false;
        if (target == this.getLastHurtByMob()) return true;
        if (target instanceof Player player) {
            return this.night() && !player.isCreative() && !player.isSpectator() && this.level().getDifficulty() != Difficulty.PEACEFUL;
        }
        return target instanceof Rabbit;
    }

    private LivingEntity findPrey() {
        AABB area = this.getBoundingBox().inflate(HUNT_RANGE, 48.0, HUNT_RANGE);
        return this.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != this && this.stillPrey(e) && this.hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player p && p.isCreative())) {
            this.setTarget(attacker);
            this.recover = 0;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return super.doHurtTarget(target);
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.anchor != null) tag.putLong("Anchor", this.anchor.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Anchor")) this.anchor = BlockPos.of(tag.getLong("Anchor"));
    }

    // ---------------------------------------------------------------- sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.EAGLE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    // high up, its cry carries far
    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
