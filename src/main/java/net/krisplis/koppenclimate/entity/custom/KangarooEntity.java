package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.MoveControl;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The kangaroo of the Australian desert: a big red, standing tall on its hind legs. It never walks: it gets about
 * only by leaping - a big bound, then a stand on the ground before the next (HopMoveControl). Leave it be and it
 * grazes, with a small hop now and then; come within a few blocks of one, though, and it comes for you in long,
 * quick bounds, about as fast as you can sprint. It kicks with both hind legs at once, so hard it throws you about five blocks away and takes half
 * your life (no armour helps: koppenclimatemod:kangaroo_kick). Carry a koala on your shoulder and the kangaroos
 * leave you in peace, even if you hit one.
 */
public class KangarooEntity extends Animal {
    public static final ResourceKey<DamageType> KICK = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "kangaroo_kick"));
    // how close you may come before it attacks, and how far it chases you
    private static final double AGGRO_RANGE = 7.0, CHASE_RANGE = 24.0;
    // the chase (MeleeAttackGoal's speed, which HopMoveControl reads as "bound at full stretch")
    private static final double CHASE_SPEED = 2.0;
    // a leap: how fast up, how fast forward (blocks per tick), and how long it stands after landing (ticks).
    // Chasing: 14 ticks in the air, ~5.6 blocks a bound, 6 ticks' stand - about a sprinting player's 0.28 blocks/tick.
    // Wandering: 12 ticks in the air, ~2.6 blocks, then a long stand.
    private static final double CHASE_LEAP_UP = 0.5, CHASE_LEAP_FORWARD = 0.40, CHASE_AIR_TICKS = 14;
    private static final int CHASE_PAUSE = 6;
    private static final double WANDER_LEAP_UP = 0.42, WANDER_LEAP_FORWARD = 0.22, WANDER_AIR_TICKS = 12;
    private static final int WANDER_PAUSE_MIN = 14, WANDER_PAUSE_RANDOM = 22;
    // the kick throws you this fast (horizontally, then upward): about five blocks
    private static final double KICK_THROW = 0.75, KICK_LIFT = 0.5;
    private static final byte KICK_EVENT = 4;

    // the kick, as the model shows it (ticks left)
    public int kickTicks;
    // the bound, as the model shows it: ticks in the air, and ticks on the ground since landing (both sides)
    public int airTicks, groundTicks = 100;
    // the leap in progress (server): its forward speed, whether it has left the ground, and the stand that follows
    private boolean leaping, leftGround;
    private double leapX, leapZ;
    private int leapAge, hopCooldown, nextPause;

    public KangarooEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, CHASE_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
                // it bounds straight up a block without stopping to jump
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // very fast: it runs you down faster than you can sprint, straight at you
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, CHASE_SPEED, true));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, Ingredient.of(Items.SHORT_GRASS, Items.WHEAT), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // whoever comes too close (but never anyone carrying a koala)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                p -> p.distanceToSqr(this) < AGGRO_RANGE * AGGRO_RANGE && !KoalaEntity.isCarriedBy(p)));
    }

    // never at anyone with a koala on their shoulder
    @Override
    public boolean canAttack(LivingEntity target) {
        return !KoalaEntity.isCarriedBy(target) && super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.kickTicks > 0) this.kickTicks--;
        if (this.onGround()) {
            this.groundTicks++;
            this.airTicks = 0;
        } else {
            this.airTicks++;
            this.groundTicks = 0;
        }
        if (this.level().isClientSide) return;
        LivingEntity target = this.getTarget();
        if (target != null && (KoalaEntity.isCarriedBy(target) || this.distanceToSqr(target) > CHASE_RANGE * CHASE_RANGE)) {
            this.setTarget(null);
        }
    }

    // the leap: kept at its full forward speed all the way through the air, then a dead stop on landing
    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            if (this.hopCooldown > 0 && this.onGround()) this.hopCooldown--;
            if (this.leaping) {
                this.leapAge++;
                Vec3 m = this.getDeltaMovement();
                if (this.isInWater() || this.isInLava() || this.leapAge > 40 || this.leftGround && this.onGround()) {
                    // down: it plants its feet and stands
                    this.leaping = false;
                    this.hopCooldown = this.nextPause;
                    this.setDeltaMovement(m.x * 0.2, m.y, m.z * 0.2);
                } else {
                    if (!this.onGround()) this.leftGround = true;
                    this.setDeltaMovement(this.leapX, m.y, this.leapZ);
                }
            }
        }
        super.aiStep();
    }

    boolean canLeap() {
        return !this.leaping && this.hopCooldown <= 0 && this.onGround() && !this.isInWater() && !this.isInLava();
    }

    /** One bound towards (dx, dz), dist blocks away: never past the spot (or into its target's lap). */
    void leap(double dx, double dz, double dist, boolean chasing) {
        double up = chasing ? CHASE_LEAP_UP : WANDER_LEAP_UP;
        double forward = chasing ? CHASE_LEAP_FORWARD : WANDER_LEAP_FORWARD;
        double air = chasing ? CHASE_AIR_TICKS : WANDER_AIR_TICKS;
        double reach = chasing && this.getTarget() != null ? Math.max(0.5, dist - 1.2) : dist;
        double speed = Math.min(forward, reach / air);
        if (speed < 0.03) return;
        this.leapX = dx / dist * speed;
        this.leapZ = dz / dist * speed;
        // a short hop needs less height
        double lift = speed < forward * 0.5 ? up * 0.8 : up;
        this.setDeltaMovement(this.leapX, lift, this.leapZ);
        this.hasImpulse = true;
        this.leaping = true;
        this.leftGround = false;
        this.leapAge = 0;
        this.nextPause = chasing ? CHASE_PAUSE : WANDER_PAUSE_MIN + this.random.nextInt(WANDER_PAUSE_RANDOM);
    }

    /**
     * Moves only by leaping: it turns to face where it is going, and when it stands ready on the ground it bounds
     * towards a point a few steps along its path (straight at its quarry when chasing). It never walks; in water it
     * swims as usual.
     */
    static class HopMoveControl extends MoveControl {
        private final KangarooEntity kangaroo;

        HopMoveControl(KangarooEntity kangaroo) {
            super(kangaroo);
            this.kangaroo = kangaroo;
        }

        @Override
        public void tick() {
            if (this.kangaroo.isInWater() || this.kangaroo.isInLava() || this.operation != MoveControl.Operation.MOVE_TO) {
                super.tick();
                return;
            }
            this.operation = MoveControl.Operation.WAIT;
            this.mob.setSpeed(0.0F);
            this.mob.setZza(0.0F);
            this.mob.setXxa(0.0F);
            boolean chasing = this.speedModifier >= CHASE_SPEED - 0.01;
            // aim: the quarry itself, or a few nodes along the path
            double ax = this.wantedX, az = this.wantedZ;
            LivingEntity target = this.kangaroo.getTarget();
            if (chasing && target != null) {
                ax = target.getX();
                az = target.getZ();
            } else {
                Path path = this.kangaroo.getNavigation().getPath();
                if (path != null && !path.isDone()) {
                    // a bound carries it past several nodes at once: count every node it has landed on or beyond as done
                    int next = path.getNextNodeIndex();
                    for (int j = Math.min(next + 5, path.getNodeCount() - 1); j >= next; j--) {
                        BlockPos n = path.getNodePos(j);
                        double nx = n.getX() + 0.5 - this.mob.getX(), nz = n.getZ() + 0.5 - this.mob.getZ();
                        if (nx * nx + nz * nz < 1.5 && Math.abs(n.getY() - this.mob.getY()) < 1.5) {
                            path.setNextNodeIndex(j + 1);
                            break;
                        }
                    }
                    if (path.isDone()) return;
                    int i = Math.min(path.getNextNodeIndex() + 3, path.getNodeCount() - 1);
                    BlockPos node = path.getNodePos(i);
                    ax = node.getX() + 0.5;
                    az = node.getZ() + 0.5;
                }
            }
            double dx = ax - this.mob.getX(), dz = az - this.mob.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.3) return;
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 40.0F));
            this.mob.yBodyRot = this.mob.getYRot();
            // only leap once it faces about the right way
            if (this.kangaroo.canLeap() && Math.abs(Mth.wrapDegrees(yaw - this.mob.getYRot())) < 30.0F) {
                this.kangaroo.leap(dx, dz, dist, chasing);
            }
        }
    }

    // the kick: half your life, and off you go
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity victim)) return super.doHurtTarget(target);
        DamageSource kick = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KICK), this);
        boolean hit = victim.hurt(kick, victim.getMaxHealth() * 0.5F);
        if (hit) {
            Vec3 away = new Vec3(victim.getX() - this.getX(), 0.0, victim.getZ() - this.getZ());
            if (away.lengthSqr() < 1.0E-4) away = Vec3.directionFromRotation(0.0F, this.getYRot());
            away = away.normalize();
            victim.setDeltaMovement(away.x * KICK_THROW, KICK_LIFT, away.z * KICK_THROW);
            victim.hurtMarked = true;
            victim.hasImpulse = true;
            this.setLastHurtMob(victim);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.2F, 0.7F);
            this.kickTicks = 10;
            this.level().broadcastEntityEvent(this, KICK_EVENT);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == KICK_EVENT) {
            this.kickTicks = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SHORT_GRASS) || stack.is(Items.WHEAT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.KANGAROO.get().create(level);
    }

    // mostly quiet; a goat-like bleat when hurt
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + this.random.nextFloat() * 0.1F;
    }
}
