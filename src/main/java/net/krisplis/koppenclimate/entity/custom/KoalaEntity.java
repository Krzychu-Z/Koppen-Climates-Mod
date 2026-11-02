package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The koala of the Australian desert's eucalyptus trees. Lazy beyond words: it spends nearly all its life asleep,
 * clinging to a eucalyptus trunk (EucalyptusTreeFeature puts them there), and when it does come down it hardly
 * moves, and soon climbs back up the nearest tree. It never makes a sound.
 *
 * <p>Pet one (use it with an empty hand) and it climbs onto your arm and rides on your shoulder, as a parrot does
 * (it hops off, as parrots do, if you fall, swim, fly, sleep or get hurt, and you can set it down by crouching:
 * KoalaShoulders). While you carry one, kangaroos leave you in peace and vipers keep their distance. But a koala
 * eats only eucalyptus leaves, and on your shoulder it can't: after half a day there it starves to death. It gets
 * its strength back on a tree, or at once if you feed it eucalyptus leaves.
 */
public class KoalaEntity extends ShoulderRidingEntity {
    public static final String ID = KoppenClimateMod.MOD_ID + ":koala";
    // how long it can ride on a shoulder before it starves (half a day), and when it starts to show it
    public static final int STARVE_TICKS = 12000, HUNGRY_TICKS = 9000;
    public static final String HUNGER_TAG = "ShoulderHunger";
    // the side of it its tree is on (a Direction's 3D index), or -1 when it isn't clinging to one
    private static final EntityDataAccessor<Byte> CLING = SynchedEntityData.defineId(KoalaEntity.class, EntityDataSerializers.BYTE);
    // off its shoulder, how fast it recovers (ticks of hunger shed per tick)
    private static final int RECOVERY = 3;

    private int hunger;

    public KoalaEntity(EntityType<? extends ShoulderRidingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.FOLLOW_RANGE, 10.0);
    }

    /** Whether this saved entity (a shoulder's) is a koala. */
    public static boolean isKoala(@Nullable CompoundTag tag) {
        return tag != null && ID.equals(tag.getString("id"));
    }

    /** Whether this is a player with a koala on either shoulder. */
    public static boolean isCarriedBy(@Nullable LivingEntity entity) {
        return entity instanceof Player player && (isKoala(player.getShoulderEntityLeft()) || isKoala(player.getShoulderEntityRight()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLING, (byte) -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.8, Ingredient.of(ModBlocks.EUCALYPTUS_LEAVES.get()), false));
        this.goalSelector.addGoal(4, new ClimbTreeGoal(this));
        // it hardly ever wanders off on its own
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.0005F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------------------------------------------------------------- clinging to its tree

    @Nullable
    public Direction getClingSide() {
        byte b = this.entityData.get(CLING);
        return b < 0 ? null : Direction.from3DDataValue(b);
    }

    public boolean isClinging() {
        return this.getClingSide() != null;
    }

    /** Hangs on to the side of this trunk block (which lies to its {@code side}). */
    public void clingTo(BlockPos trunk, Direction side) {
        this.entityData.set(CLING, (byte) side.get3DDataValue());
        this.setYRot(side.toYRot());
        this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    public void letGo() {
        this.entityData.set(CLING, (byte) -1);
        this.setNoGravity(false);
    }

    // clinging, it doesn't move at all: it's asleep
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isClinging();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        Direction side = this.getClingSide();
        if (side != null) {
            BlockState trunk = this.level().getBlockState(this.blockPosition().relative(side));
            if (!trunk.is(BlockTags.LOGS)) {
                this.letGo();
            } else {
                this.setNoGravity(true);
                this.setDeltaMovement(Vec3.ZERO);
                this.setYRot(side.toYRot());
                this.yBodyRot = this.yHeadRot = this.getYRot();
            }
        }
        // away from a shoulder it gets its strength back, slowly
        if (this.hunger > 0) this.hunger = Math.max(0, this.hunger - RECOVERY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) this.letGo();
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---------------------------------------------------------------- onto your shoulder

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isFood(stack)) {
            // fed, it's not hungry any more (and it may breed, as any animal fed its food)
            if (!this.level().isClientSide) this.hunger = 0;
            return super.mobInteract(player, hand);
        }
        if (stack.isEmpty() && !this.isBaby()) {
            if (player instanceof ServerPlayer serverPlayer) {
                this.letGo();
                this.setEntityOnShoulder(serverPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(HUNGER_TAG, this.hunger);
        tag.putByte("Cling", this.entityData.get(CLING));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hunger = tag.getInt(HUNGER_TAG);
        // just off a shoulder (KoalaShoulders keeps count there), it isn't clinging to anything
        this.entityData.set(CLING, tag.contains("Cling") ? tag.getByte("Cling") : (byte) -1);
    }

    // ---------------------------------------------------------------- the rest of its life

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.EUCALYPTUS_LEAVES.get().asItem());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.KOALA.get().create(level);
    }

    // not a sound, ever
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    /** Down on the ground, it soon shuffles back to the nearest tree trunk and climbs up it to sleep. */
    static class ClimbTreeGoal extends Goal {
        private final KoalaEntity koala;
        @Nullable
        private BlockPos stand;
        @Nullable
        private Direction toTrunk;
        private int ticks;

        ClimbTreeGoal(KoalaEntity koala) {
            this.koala = koala;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.koala.isClinging() || !this.koala.onGround() || this.koala.getRandom().nextInt(60) != 0) return false;
            return this.findTree();
        }

        @Override
        public boolean canContinueToUse() {
            return this.stand != null && !this.koala.isClinging() && this.ticks < 400;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.koala.getNavigation().moveTo(this.stand.getX() + 0.5, this.stand.getY(), this.stand.getZ() + 0.5, 0.7);
        }

        @Override
        public void stop() {
            this.stand = null;
            this.koala.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.ticks++;
            if (this.stand == null || this.toTrunk == null) return;
            if (this.koala.distanceToSqr(this.stand.getX() + 0.5, this.stand.getY(), this.stand.getZ() + 0.5) > 1.0) {
                if (this.koala.getNavigation().isDone()) this.koala.getNavigation().moveTo(this.stand.getX() + 0.5, this.stand.getY(), this.stand.getZ() + 0.5, 0.7);
                return;
            }
            // at the foot of the trunk: up it goes, a block or two, and hangs on
            Level level = this.koala.level();
            for (int up = 2; up >= 0; up--) {
                BlockPos spot = this.stand.above(up);
                if (level.getBlockState(spot.relative(this.toTrunk)).is(BlockTags.LOGS) && level.getBlockState(spot).isAir()
                        && level.getBlockState(spot.above()).isAir()) {
                    this.koala.moveTo(spot.getX() + 0.5 + this.toTrunk.getStepX() * 0.2, spot.getY(), spot.getZ() + 0.5 + this.toTrunk.getStepZ() * 0.2,
                            this.toTrunk.toYRot(), 0.0F);
                    this.koala.clingTo(spot.relative(this.toTrunk), this.toTrunk);
                    break;
                }
            }
            this.stand = null;
        }

        /** The nearest spot on the ground right beside a tree trunk, within a few blocks. */
        private boolean findTree() {
            Level level = this.koala.level();
            BlockPos here = this.koala.blockPosition();
            double best = Double.MAX_VALUE;
            this.stand = null;
            for (BlockPos p : BlockPos.betweenClosed(here.offset(-8, -2, -8), here.offset(8, 2, 8))) {
                if (!level.getBlockState(p).is(BlockTags.LOGS)) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos s = p.relative(d);
                    if (!level.getBlockState(s).isAir() || level.getBlockState(s.below()).isAir()) continue;
                    double dist = s.distSqr(here);
                    if (dist < best) {
                        best = dist;
                        this.stand = s.immutable();
                        this.toTrunk = d.getOpposite();
                    }
                }
            }
            return this.stand != null;
        }
    }
}
