package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;

import java.util.EnumSet;

// An ambush predator of rivers and swamps. It never drowns, and only moves fast when it hunts or fights;
// otherwise it drifts slowly. It goes for players who come close and hunts animals at the water's edge.
// Most of the day it lies on the bank with its jaws wide open: by day it leaves the water for the shore and
// basks; a hit, prey or a player close by ends that. It wanders and swims around at night.
// It's silent except when it gets hurt.
public class CrocodileEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(CrocodileEntity.class, EntityDataSerializers.BOOLEAN);
    // players closer than this are attacked; further away it leaves them be
    private static final double AMBUSH_RANGE = 8.0;
    private static final int HUNT_INTERVAL = 40;
    // the model is about 3.5 blocks long, longer than the hitbox
    private static final double MODEL_REACH = 1.5;
    // swim speed and land speed, as multiples of MOVEMENT_SPEED
    private static final float SWIM_SPEED = 1.6F;
    private static final float WALK_SPEED = 0.9F;
    // swimming with nothing to chase: a slow drift
    private static final float IDLE_SWIM_FACTOR = 0.35F;
    private static final float BASK_BLEND_SPEED = 0.1F;

    // client side: 0 = jaws shut, 1 = basking with jaws open, eased toward the synced state
    private float baskAmount;
    private float prevBaskAmount;

    public CrocodileEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, SWIM_SPEED, WALK_SPEED, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new BaskGoal(this));
        this.goalSelector.addGoal(3, new GoToBankGoal(this));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 40) {
            @Override
            public boolean canUse() {
                return !CrocodileEntity.this.level().isDay() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !CrocodileEntity.this.level().isDay() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new TryFindWaterGoal(this) {
            @Override
            public boolean canUse() {
                return !CrocodileEntity.this.level().isDay() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p.distanceTo(this) < AMBUSH_RANGE));
        for (Class<? extends Mob> prey : java.util.List.of(Pig.class, Sheep.class, Chicken.class, Rabbit.class, Frog.class)) {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, prey, HUNT_INTERVAL, true, false, null));
        }
    }

    // Underwater it swims freely in 3D, like the axolotl; on land it walks normally
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            float speed = this.getTarget() != null ? this.getSpeed() : this.getSpeed() * IDLE_SWIM_FACTOR;
            this.moveRelative(speed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BASKING, false);
    }

    public boolean isBasking() {
        return this.entityData.get(BASKING);
    }

    public void setBasking(boolean basking) {
        this.entityData.set(BASKING, basking);
    }

    public float getBaskAmount(float partialTick) {
        return Mth.lerp(partialTick, this.prevBaskAmount, this.baskAmount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Basking", this.isBasking());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setBasking(tag.getBoolean("Basking"));
    }

    @Override
    public void tick() {
        super.tick();
        this.prevBaskAmount = this.baskAmount;
        this.baskAmount += ((this.isBasking() ? 1.0F : 0.0F) - this.baskAmount) * BASK_BLEND_SPEED;
        if (!this.level().isClientSide && this.isBasking() && (this.getTarget() != null || this.isInWater())) {
            this.setBasking(false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.setBasking(false);
        }
        return super.hurt(source, amount);
    }

    // stays put like animals do, so a river keeps its crocodiles
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(MODEL_REACH, 0.5, MODEL_REACH);
    }

    // silent, except when it gets hurt
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // Lies still on dry land by day with its jaws open. Holds movement and looking while it lasts.
    static class BaskGoal extends Goal {
        // chance per tick (1 in N) to start basking when idle on land by day
        private static final int SETTLE_CHANCE = 20;
        // chance per tick (1 in N) to get up on its own: about five minutes on average
        private static final int STIR_CHANCE = 6000;
        // a player this close who isn't sneaking rouses it (and then gets attacked, being inside the ambush range)
        private static final double DISTURB_RANGE = 5.0;

        private final CrocodileEntity croc;

        BaskGoal(CrocodileEntity croc) {
            this.croc = croc;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!this.canBask()) return false;
            // a crocodile that was basking when the world was saved goes straight back to it
            if (this.croc.isBasking()) return true;
            return this.croc.getNavigation().isDone() && this.croc.getRandom().nextInt(SETTLE_CHANCE) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canBask() && this.croc.getRandom().nextInt(STIR_CHANCE) != 0;
        }

        @Override
        public void start() {
            this.croc.getNavigation().stop();
            this.croc.setBasking(true);
        }

        @Override
        public void stop() {
            this.croc.setBasking(false);
        }

        private boolean canBask() {
            return this.croc.level().isDay() && this.croc.getTarget() == null && this.croc.onGround()
                    && !this.croc.isInWater() && !this.playerClose();
        }

        private boolean playerClose() {
            return this.croc.level().getNearestPlayer(this.croc.getX(), this.croc.getY(), this.croc.getZ(), DISTURB_RANGE,
                    e -> !e.isSpectator() && !e.isDiscrete()) != null;
        }
    }

    // By day, leave the water for a spot on the bank right beside it, to bask there.
    static class GoToBankGoal extends MoveToBlockGoal {
        private static final int SEARCH_RANGE = 16;
        private static final int VERTICAL_RANGE = 4;
        private final CrocodileEntity croc;

        GoToBankGoal(CrocodileEntity croc) {
            super(croc, 1.0, SEARCH_RANGE, VERTICAL_RANGE);
            this.croc = croc;
        }

        @Override
        public boolean canUse() {
            return this.croc.level().isDay() && this.croc.isInWater() && this.croc.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.croc.level().isDay() && this.croc.getTarget() == null && super.canContinueToUse();
        }

        // dry ground with room above it and water right next to it
        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) return false;
            if (!level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) return false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER) || level.getFluidState(pos.above().relative(dir)).is(FluidTags.WATER)) {
                    return true;
                }
            }
            return false;
        }
    }
}
