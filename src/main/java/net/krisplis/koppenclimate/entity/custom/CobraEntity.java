package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * A cobra, behaving like the real snake:
 * <ul>
 *   <li>It hunts small animals (rabbits, chickens, frogs) and leaves people alone if it can.</li>
 *   <li>A player who comes near makes it slip away into cover. One who surprises it up close (sneaking gets you
 *       closer before it notices) or corners it gets the warning: it rears up, spreads its hood and hisses,
 *       swaying to keep facing the threat.</li>
 *   <li>Back off and it calms down and goes on its way. Keep pressing it - come within striking range, or
 *       stand your ground too long - and it strikes: a venomous bite (strong poison and creeping paralysis).
 *       Now and then, from a few blocks away, it spits venom at your eyes instead and blinds you for a moment.</li>
 *   <li>After a strike, or when it's hurt badly, it gives up the stand-off and flees. Hit it while it's still
 *       fit and it strikes back first.</li>
 * </ul>
 */
public class CobraEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> REARING = SynchedEntityData.defineId(CobraEntity.class, EntityDataSerializers.BOOLEAN);
    // how fast the model rears up or settles down, per tick
    private static final float REAR_BLEND_SPEED = 0.2F;

    // a player this close makes it slither away; closer still and it stands its ground and warns
    private static final double NOTICE_RANGE = 8.0;
    private static final double WARN_RANGE = 4.5;
    // sneaking players are noticed only this close
    private static final double SNEAK_WARN_RANGE = 2.5;
    // comes this close while it's warning, and it strikes
    private static final double STRIKE_RANGE = 2.3;
    // holds the warning this long before striking someone who won't leave
    private static final int WARN_PATIENCE = 70;
    // spitting venom: from this far, with this chance per tick, at most this often
    private static final double SPIT_MIN = 2.5;
    private static final double SPIT_MAX = 6.0;
    private static final int SPIT_CHANCE = 30;
    private static final int SPIT_COOLDOWN = 100;
    // runs from what threatened it for this long after a strike or a bad wound
    private static final int FLEE_TICKS = 100;

    // the bite: strong poison and the slowing paralysis of neurotoxic venom
    private static final int VENOM_TICKS = 200;
    private static final int POISON_AMPLIFIER = 1;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int BLIND_TICKS = 100;
    private static final int HUNT_INTERVAL = 60;

    // client side: 0 = lying flat, 1 = reared with the hood spread, eased toward the synced state
    private float rearAmount;
    private float prevRearAmount;

    // server side
    private int fleeTicks;
    private int spitCooldown;
    @Nullable
    private LivingEntity fleeFrom;

    public CobraEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)   // one blow of an iron (or better) sword or axe
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REARING, false);
    }

    public boolean isRearing() {
        return this.entityData.get(REARING);
    }

    void setRearing(boolean rearing) {
        this.entityData.set(REARING, rearing);
    }

    public float getRearAmount(float partialTick) {
        return Mth.lerp(partialTick, this.prevRearAmount, this.rearAmount);
    }

    boolean isFleeing() {
        return this.fleeTicks > 0;
    }

    @Override
    protected void registerGoals() {
        // cobras swim well, at the surface
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal(this));
        this.goalSelector.addGoal(2, new WarnAndStrikeGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(4, new SlipAwayGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // only small prey is ever hunted; people are only warned off and bitten in self-defence
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Rabbit.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Chicken.class, HUNT_INTERVAL, true, false, null));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Frog.class, HUNT_INTERVAL, true, false, null));
        // villagers who come near are struck
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.AbstractVillager.class,
                HUNT_INTERVAL, true, false, v -> v.distanceTo(this) < 6.0F));
    }

    @Override
    public void tick() {
        super.tick();
        this.prevRearAmount = this.rearAmount;
        this.rearAmount += ((this.isRearing() ? 1.0F : 0.0F) - this.rearAmount) * REAR_BLEND_SPEED;
        if (!this.level().isClientSide) {
            if (this.fleeTicks > 0 && --this.fleeTicks == 0) this.fleeFrom = null;
            if (this.spitCooldown > 0) this.spitCooldown--;
        }
    }

    // a venomous bite on anything it strikes, prey or threat
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, POISON_AMPLIFIER), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, VENOM_TICKS, SLOWNESS_AMPLIFIER), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat && this.isAlive()) {
            // a cat is the one thing a snake won't fight: it just gets away
            this.startFleeing(cat);
            return true;
        }
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && this.isAlive()) {
            // badly hurt: flee; otherwise strike back at once if the attacker is in reach
            if (this.getHealth() < this.getMaxHealth() * 0.4F) {
                this.startFleeing(attacker);
            } else if (this.distanceToSqr(attacker) <= STRIKE_RANGE * STRIKE_RANGE * 1.5) {
                this.strike(attacker);
            }
        }
        return hurt;
    }

    // snakes never go for cats
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof net.minecraft.world.entity.animal.Cat ? null : target);
    }

    void startFleeing(LivingEntity from) {
        this.fleeFrom = from;
        this.fleeTicks = FLEE_TICKS;
        this.setRearing(false);
        this.setTarget(null);
    }

    /** Lunges at the threat and bites it if it's in reach, then makes off. */
    void strike(LivingEntity threat) {
        this.getLookControl().setLookAt(threat, 90.0F, 90.0F);
        Vec3 lunge = threat.position().subtract(this.position()).normalize().scale(0.45);
        this.setDeltaMovement(lunge.x, 0.15, lunge.z);
        this.swing(this.getUsedItemHand());
        this.playSound(SoundEvents.CAT_HISS, 1.2F, 0.7F);
        if (this.distanceToSqr(threat) <= STRIKE_RANGE * STRIKE_RANGE * 1.5) {
            this.doHurtTarget(threat);
        }
        this.startFleeing(threat);
    }

    /** Spits venom at the threat's eyes: a moment's blindness, no damage. */
    void spit(LivingEntity threat) {
        this.spitCooldown = SPIT_COOLDOWN;
        this.playSound(SoundEvents.LLAMA_SPIT, 1.0F, 1.6F);
        Vec3 from = this.position().add(0.0, 0.6, 0.0);
        Vec3 to = threat.getEyePosition();
        if (this.level() instanceof ServerLevel server) {
            for (int i = 1; i <= 8; i++) {
                Vec3 p = from.lerp(to, i / 8.0);
                server.sendParticles(ParticleTypes.SPIT, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        if (this.hasLineOfSight(threat)) {
            threat.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS), this);
        }
    }

    boolean canSpit() {
        return this.spitCooldown == 0;
    }

    /** The nearest player close enough to alarm it, at the given range (shorter for sneaking players). */
    @Nullable
    Player threatWithin(double range, double sneakRange) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : this.level().players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
            double r = player.isDiscrete() ? sneakRange : range;
            double d = this.distanceToSqr(player);
            if (d <= r * r && d < best) {
                best = d;
                nearest = player;
            }
        }
        return nearest;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    // a low hiss now and then, louder when it rears
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isRearing() ? null : SoundEvents.CAT_HISS;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HISS;
    }

    @Override
    public float getVoicePitch() {
        return 0.65F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // ------------------------------------------------------------------------------------------ goals

    /** Rears up, spreads its hood and hisses at a player who surprises or corners it; strikes if pressed. */
    static class WarnAndStrikeGoal extends Goal {
        private final CobraEntity cobra;
        @Nullable
        private Player threat;
        private int warnTicks;

        WarnAndStrikeGoal(CobraEntity cobra) {
            this.cobra = cobra;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.cobra.isFleeing() || this.cobra.isInWater()) return false;
            this.threat = this.cobra.threatWithin(WARN_RANGE, SNEAK_WARN_RANGE);
            return this.threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            // calms down once the threat has backed off a little further than where it got alarmed
            return this.threat != null && this.threat.isAlive() && !this.threat.isSpectator() && !this.threat.isCreative()
                    && !this.cobra.isFleeing() && this.cobra.distanceToSqr(this.threat) <= (WARN_RANGE + 1.5) * (WARN_RANGE + 1.5);
        }

        @Override
        public void start() {
            this.warnTicks = 0;
            this.cobra.getNavigation().stop();
            this.cobra.setRearing(true);
            this.cobra.playSound(SoundEvents.CAT_HISS, 1.0F, 0.6F);
        }

        @Override
        public void stop() {
            this.cobra.setRearing(false);
            this.threat = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player threat = this.threat;
            if (threat == null) return;
            this.cobra.getNavigation().stop();
            this.cobra.getLookControl().setLookAt(threat, 30.0F, 30.0F);
            this.warnTicks++;
            if (this.warnTicks % 30 == 0) {
                this.cobra.playSound(SoundEvents.CAT_HISS, 1.0F, 0.6F);
            }
            double distance = Math.sqrt(this.cobra.distanceToSqr(threat));
            // came closer than it allows, or wouldn't leave: strike
            if (distance <= STRIKE_RANGE || (this.warnTicks > WARN_PATIENCE && distance <= WARN_RANGE * 0.75)) {
                this.cobra.strike(threat);
                return;
            }
            if (distance >= SPIT_MIN && distance <= SPIT_MAX && this.cobra.canSpit()
                    && this.warnTicks > 20 && this.cobra.getRandom().nextInt(SPIT_CHANCE) == 0) {
                this.cobra.spit(threat);
            }
        }
    }

    /** Slips quietly away from a player who comes near but hasn't surprised it yet. */
    static class SlipAwayGoal extends Goal {
        private final CobraEntity cobra;
        @Nullable
        private Player threat;

        SlipAwayGoal(CobraEntity cobra) {
            this.cobra = cobra;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cobra.getTarget() != null) return false;
            this.threat = this.cobra.threatWithin(NOTICE_RANGE, SNEAK_WARN_RANGE);
            return this.threat != null && this.moveAway();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.cobra.getNavigation().isDone();
        }

        @Override
        public void stop() {
            this.threat = null;
        }

        private boolean moveAway() {
            Vec3 away = DefaultRandomPos.getPosAway(this.cobra, 10, 4, this.threat.position());
            return away != null && this.cobra.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
    }

    /** Makes off after a strike or a bad wound. */
    static class FleeGoal extends Goal {
        private final CobraEntity cobra;

        FleeGoal(CobraEntity cobra) {
            this.cobra = cobra;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.cobra.isFleeing() && this.cobra.fleeFrom != null && this.cobra.onGround() && this.run();
        }

        @Override
        public boolean canContinueToUse() {
            return this.cobra.isFleeing() && !this.cobra.getNavigation().isDone();
        }

        private boolean run() {
            Vec3 away = DefaultRandomPos.getPosAway(this.cobra, 14, 5, this.cobra.fleeFrom.position());
            return away != null && this.cobra.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
        }
    }
}
